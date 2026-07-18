package com.qm.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.common.RabbitMQConfig;
import com.qm.notify.NotificationService;
import com.qm.org.UserService;
import com.qm.org.entity.User;
import com.qm.requirement.RequirementService;
import com.qm.requirement.entity.Requirement;
import com.qm.workflow.entity.ReviewFlow;
import com.qm.workflow.entity.ReviewVote;
import com.qm.workflow.mapper.ReviewFlowMapper;
import com.qm.workflow.mapper.ReviewVoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 评审超时升级（设计 02-state-machine：48h 提醒未投票人，72h 升级）
 * 延迟由 TTL+DLX 队列实现（见 RabbitMQConfig）；评审发起时按两档各投一条，
 * 到期触发时若流程已结束则直接忽略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewTimeoutService {

    private final ReviewFlowMapper flowMapper;
    private final ReviewVoteMapper voteMapper;
    private final RequirementService requirementService;
    private final UserService userService;
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TIMEOUT_REVIEW)
    public void onReviewTimeout(Map<String, Object> msg) {
        String flowId = (String) msg.get("flowId");
        String tier = (String) msg.get("tier");
        ReviewFlow flow = flowMapper.selectById(flowId);
        if (flow == null || !"in_progress".equals(flow.getStatus())) {
            log.debug("Review timeout ignored (flow finished): flow={} tier={}", flowId, tier);
            return;
        }

        Requirement req = requirementService.getById(flow.getRequirementId());
        String reqNo = req != null ? req.getReqNo() : flow.getRequirementId();

        if ("escalate".equals(tier)) {
            // 72h 升级：通知需求创建人跟进（P0 简化；设计上应通知评审人的上级）
            if (req != null && req.getCreatedBy() != null) {
                notificationService.send(req.getCreatedBy(), "review_timeout_escalate",
                    "评审超时升级",
                    String.format("需求 %s 的评审已超过 72 小时未完成，请尽快跟进", reqNo),
                    "feishu", null);
            }
            log.warn("Review escalated: flow={} req={}", flowId, reqNo);
            return;
        }

        // 48h 提醒：通知所有未投票的评审人
        List<ReviewVote> pending = voteMapper.selectList(
            new LambdaQueryWrapper<ReviewVote>()
                .eq(ReviewVote::getFlowId, flowId)
                .eq(ReviewVote::getDecision, "pending"));
        int reminded = 0;
        for (ReviewVote vote : pending) {
            User voter = resolveUser(vote.getVoterId());
            if (voter == null) {
                continue;
            }
            notificationService.send(voter.getId(), "review_timeout_remind",
                "评审超时提醒",
                String.format("需求 %s 的评审等待您的投票已超过 48 小时，请及时处理", reqNo),
                "feishu", null);
            reminded++;
        }
        log.info("Review timeout reminded: flow={} req={} reminded={}/{}", flowId, reqNo, reminded, pending.size());
    }

    /** voterId 可能是飞书 open_id（卡片回调路径）或平台用户 id，两种都试 */
    private User resolveUser(String voterId) {
        User user = userService.getByFeishuOpenId(voterId);
        if (user == null) {
            user = userService.getById(voterId);
        }
        return user;
    }
}
