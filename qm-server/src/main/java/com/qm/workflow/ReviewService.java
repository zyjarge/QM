package com.qm.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.common.RabbitMQConfig;
import com.qm.common.exception.BizException;
import com.qm.integration.im.feishu.FeishuGroupService;
import com.qm.requirement.RequirementState;
import com.qm.requirement.RequirementService;
import com.qm.requirement.entity.Requirement;
import com.qm.workflow.entity.ReviewFlow;
import com.qm.workflow.entity.ReviewVote;
import com.qm.workflow.mapper.ReviewFlowMapper;
import com.qm.workflow.mapper.ReviewVoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewFlowMapper flowMapper;
    private final ReviewVoteMapper voteMapper;
    private final RequirementService requirementService;
    private final com.qm.groupengine.GroupService groupService;
    private final FeishuGroupService feishuGroupService;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    public List<ReviewFlow> listFlows(String reqId) {
        return flowMapper.selectList(
            new LambdaQueryWrapper<ReviewFlow>()
                .eq(ReviewFlow::getRequirementId, reqId)
                .orderByDesc(ReviewFlow::getRoundNo));
    }

    public List<ReviewVote> listVotes(String flowId) {
        return voteMapper.selectList(
            new LambdaQueryWrapper<ReviewVote>()
                .eq(ReviewVote::getFlowId, flowId));
    }

    @Transactional
    public ReviewFlow startReview(String reqId, String reviewType, String mode,
                                   List<String> voterIds, String operatorId) {
        Requirement req = requirementService.getById(reqId);
        // 状态校验
        if (!RequirementState.pending_review.name().equals(req.getStatus())) {
            throw BizException.illegalState("当前状态不允许发起评审");
        }

        // 创建评审流
        ReviewFlow flow = new ReviewFlow();
        flow.setRequirementId(reqId);
        flow.setReviewType(reviewType == null ? "final" : reviewType);
        flow.setMode(mode == null ? "all" : mode);
        flow.setStatus("in_progress");
        flow.setStartedAt(LocalDateTime.now());
        flowMapper.insert(flow);

        // 创建投票记录
        for (String voterId : voterIds) {
            ReviewVote vote = new ReviewVote();
            vote.setFlowId(flow.getId());
            vote.setVoterId(voterId);
            vote.setDecision("pending");
            voteMapper.insert(vote);
        }

        // 需求状态 → reviewing
        requirementService.transition(reqId, "reviewing", operatorId, "发起评审");

        // 投两档超时检查（48h 提醒 / 72h 升级，TTL+DLX 延迟队列，到期未完成才触发）
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_DELAY_REVIEW_REMIND,
            java.util.Map.of("flowId", flow.getId(), "reqId", reqId, "tier", "remind"));
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_DELAY_REVIEW_ESCALATE,
            java.util.Map.of("flowId", flow.getId(), "reqId", reqId, "tier", "escalate"));

        // 自动发评审卡片到需求群（A2）
        try {
            com.qm.groupengine.entity.RequirementGroup group = groupService.getByReqId(reqId);
            if (group != null && "active".equals(group.getStatus())) {
                String cardJson = buildReviewCard(req, flow, voterIds);
                feishuGroupService.sendCard(group.getChatId(), cardJson);
                log.info("Review card sent: flow={} chatId={}", flow.getId(), group.getChatId());
            }
        } catch (Exception e) {
            log.warn("Failed to send review card: {}", e.getMessage());
        }

        log.info("Review started: req={} flow={} voters={}", reqId, flow.getId(), voterIds);
        return flow;
    }

    private String buildReviewCard(Requirement req, ReviewFlow flow, List<String> voterIds) {
        String voters = String.join(", ", voterIds);
        String actions = "";
        for (String v : voterIds) {
            actions += String.format(
                "{\"tag\":\"button\",\"text\":{\"tag\":\"plain_text\",\"content\":\"✅ %s 同意\"},\"type\":\"primary\",\"value\":{\"type\":\"review\",\"id\":\"%s\",\"action\":\"approve\",\"voter\":\"%s\"}},",
                v, flow.getId(), v);
        }
        actions += String.format(
            "{\"tag\":\"button\",\"text\":{\"tag\":\"plain_text\",\"content\":\"❌ 驳回\"},\"type\":\"danger\",\"value\":{\"type\":\"review\",\"id\":\"%s\",\"action\":\"reject\"}}",
            flow.getId());

        return String.format(
            "{\"config\":{\"wide_screen_mode\":true},\"header\":{\"template\":\"blue\",\"title\":{\"tag\":\"plain_text\",\"content\":\"📋 评审请求 %s\"}},\"elements\":[" +
            "{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"**%s**\\n\\n%s\\n\\n**投票人**: %s\\n**模式**: %s\\n**轮次**: %d\"}}," +
            "{\"tag\":\"action\",\"actions\":[%s]}]}",
            req.getReqNo(), req.getTitle(),
            req.getReqType() != null ? "类型: " + req.getReqType() : "",
            voters, flow.getMode(), flow.getRoundNo() == null ? 1 : flow.getRoundNo(),
            actions);
    }

    @Transactional
    public ReviewVote castVote(String flowId, String voterId, String decision,
                                String comment, String operatorId) {
        // 幂等校验
        ReviewVote existing = voteMapper.selectOne(
            new LambdaQueryWrapper<ReviewVote>()
                .eq(ReviewVote::getFlowId, flowId)
                .eq(ReviewVote::getVoterId, voterId)
                .eq(ReviewVote::getDecision, "pending"));
        if (existing == null) {
            throw BizException.illegalState("您已投过票或不在评审名单中");
        }

        existing.setDecision(decision);
        existing.setComment(comment);
        existing.setVotedAt(LocalDateTime.now());
        voteMapper.updateById(existing);

        // 检查流程是否完成
        checkFlowCompletion(flowId);

        log.info("Vote cast: flow={} voter={} decision={}", flowId, voterId, decision);
        return existing;
    }

    private void checkFlowCompletion(String flowId) {
        ReviewFlow flow = flowMapper.selectById(flowId);
        if (flow == null || !"in_progress".equals(flow.getStatus())) return;

        List<ReviewVote> votes = voteMapper.selectList(
            new LambdaQueryWrapper<ReviewVote>().eq(ReviewVote::getFlowId, flowId));

        long total = votes.size();
        long approved = votes.stream().filter(v -> "approve".equals(v.getDecision())).count();
        long rejected = votes.stream().filter(v -> "reject".equals(v.getDecision())).count();

        boolean allVoted = votes.stream().noneMatch(v -> "pending".equals(v.getDecision()));
        boolean passed = "all".equals(flow.getMode()) ? approved == total : approved > 0;
        boolean hasReject = rejected > 0;

        if (allVoted || (hasReject && "any".equals(flow.getMode()))) {
            flow.setStatus(passed ? "passed" : "rejected");
            flow.setFinishedAt(LocalDateTime.now());
            flowMapper.updateById(flow);

            // 驱动需求状态机
            if (passed) {
                requirementService.transition(flow.getRequirementId(), "pending_sign",
                    "system", "评审通过(" + approved + "/" + total + ")");

                // 自动发签认卡片到群（A3）
                try {
                    com.qm.groupengine.entity.RequirementGroup group = groupService.getByReqId(flow.getRequirementId());
                    Requirement req = requirementService.getById(flow.getRequirementId());
                    if (group != null && "active".equals(group.getStatus()) && req != null) {
                        String signCard = String.format(
                            "{\"config\":{\"wide_screen_mode\":true},\"header\":{\"template\":\"green\",\"title\":{\"tag\":\"plain_text\",\"content\":\"✅ 评审通过 %s\"}},\"elements\":[" +
                            "{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"**%s**\\n\\n评审已通过 (%d/%d)，请业务负责人确认基线。\"}}," +
                            "{\"tag\":\"action\",\"actions\":[" +
                            "{\"tag\":\"button\",\"text\":{\"tag\":\"plain_text\",\"content\":\"✅ 确认基线\"},\"type\":\"primary\",\"value\":{\"type\":\"baseline\",\"id\":\"%s\",\"action\":\"sign\"}}," +
                            "{\"tag\":\"button\",\"text\":{\"tag\":\"plain_text\",\"content\":\"❌ 驳回\"},\"type\":\"danger\",\"value\":{\"type\":\"baseline\",\"id\":\"%s\",\"action\":\"reject\"}}" +
                            "]}]}",
                            req.getReqNo(), req.getTitle(), approved, total,
                            flow.getRequirementId(), flow.getRequirementId());
                        feishuGroupService.sendCard(group.getChatId(), signCard);
                        log.info("Sign card sent: req={} chatId={}", req.getId(), group.getChatId());
                    }
                } catch (Exception e) {
                    log.warn("Failed to send sign card: {}", e.getMessage());
                }
            } else {
                requirementService.transition(flow.getRequirementId(), "clarifying",
                    "system", "评审拒绝(" + rejected + "/" + total + ")");
            }
        }
    }

    @Transactional
    public ReviewFlow getCurrentFlow(String reqId) {
        return flowMapper.selectOne(
            new LambdaQueryWrapper<ReviewFlow>()
                .eq(ReviewFlow::getRequirementId, reqId)
                .eq(ReviewFlow::getStatus, "in_progress")
                .last("LIMIT 1"));
    }
}
