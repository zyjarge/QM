package com.qm.integration.im.feishu;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.common.exception.BizException;
import com.qm.notify.NotificationService;
import com.qm.requirement.RequirementService;
import com.qm.requirement.entity.Requirement;
import com.qm.workflow.BaselineService;
import com.qm.workflow.ReviewService;
import com.qm.workflow.entity.ReviewFlow;
import com.qm.workflow.mapper.ReviewFlowMapper;
import com.qm.workflow.mapper.ReviewVoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 飞书卡片回调处理
 * - A2: 评审投票
 * - A3: 基线签认
 * - A4: 越权防护（投票人必须在 voterIds 内）
 * - A6: 幂等（Redis SETNX 30 分钟去重）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuCardCallbackService {

    private final ReviewService reviewService;
    private final BaselineService baselineService;
    private final RequirementService requirementService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redis;
    private final ReviewFlowMapper reviewFlowMapper;
    private final ReviewVoteMapper voteMapper;

    /**
     * 处理卡片动作回调
     * @param eventId 飞书事件 ID（用于幂等）
     * @param action 动作：approve/reject/sign_baseline
     * @param targetType 目标类型：review/baseline
     * @param targetId 目标 ID
     * @param operatorOpenId 操作人 open_id
     * @return 响应消息（会显示在卡片上）
     */
    @Transactional
    public String handle(String eventId, String action, String targetType,
                         String targetId, String operatorOpenId) {
        // A6: 幂等 - Redis SETNX
        String dedupKey = "qm:cb:dedup:" + eventId;
        Boolean firstTime = redis.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofMinutes(30));
        if (Boolean.FALSE.equals(firstTime)) {
            log.info("Duplicate callback ignored: {}", eventId);
            return "操作已完成（重复回调已忽略）";
        }

        log.info("Card callback: action={} type={} target={} operator={}",
            action, targetType, targetId, operatorOpenId);

        if ("review".equals(targetType)) {
            return handleReviewAction(action, targetId, operatorOpenId);
        } else if ("baseline".equals(targetType)) {
            return handleBaselineAction(action, targetId, operatorOpenId);
        } else {
            throw BizException.illegalState("未知回调类型: " + targetType);
        }
    }

    private String handleReviewAction(String action, String flowId, String operatorOpenId) {
        // 直接通过 ReviewFlowMapper 查询
        ReviewFlow flow = reviewFlowMapper.selectById(flowId);
        if (flow == null) {
            throw BizException.notFound("评审流程不存在: " + flowId);
        }

        // A4: 越权防护 - 操作人必须是评审流程中的投票人
        String reqId = flow.getRequirementId();
        java.util.List<String> allowedVoters = voteMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.qm.workflow.entity.ReviewVote>()
                .eq(com.qm.workflow.entity.ReviewVote::getFlowId, flowId))
            .stream()
            .map(com.qm.workflow.entity.ReviewVote::getVoterId)
            .filter(java.util.Objects::nonNull)
            .toList();
        if (!allowedVoters.contains(operatorOpenId)) {
            log.warn("Unauthorized vote attempt: flow={} operator={}", flowId, operatorOpenId);
            throw BizException.forbidden("您不是本次评审的投票人");
        }

        String decision = "approve".equals(action) ? "approve" : "reject";
        reviewService.castVote(flowId, operatorOpenId, decision, null, operatorOpenId);

        // 通知需求创建人
        Requirement req = requirementService.getById(reqId);
        if (req != null && req.getCreatedBy() != null) {
            notificationService.send(req.getCreatedBy(), "review_vote",
                "收到评审投票",
                String.format("%s %s了评审 %s", operatorOpenId, decision, req.getReqNo()),
                "im_card", null);
        }

        return "approve".equals(action) ? "✅ 已投票通过" : "❌ 已投票驳回";
    }

    private String handleBaselineAction(String action, String reqId, String operatorOpenId) {
        Requirement req = requirementService.getById(reqId);
        if (req == null) {
            throw BizException.notFound("需求不存在: " + reqId);
        }

        // A4: 仅需求创建人或 owner 可签认
        if (!operatorOpenId.equals(req.getCreatedBy())
            && !operatorOpenId.equals(req.getOwnerId())) {
            log.warn("Unauthorized sign attempt: req={} operator={}", reqId, operatorOpenId);
            throw BizException.forbidden("您不是该需求的负责人，无权签认");
        }

        if ("sign".equals(action)) {
            baselineService.signBaseline(reqId, operatorOpenId, "card-callback", "feishu-card");
            return "✅ 基线签认成功";
        } else if ("reject".equals(action)) {
            // 驳回：状态回到 pending_review
            requirementService.transition(reqId, "pending_review", operatorOpenId, "基线驳回");
            return "❌ 已驳回，重新进入评审";
        } else {
            throw BizException.illegalState("未知基线动作: " + action);
        }
    }
}
