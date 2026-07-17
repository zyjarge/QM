package com.qm.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.common.exception.BizException;
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

        log.info("Review started: req={} flow={} voters={}", reqId, flow.getId(), voterIds);
        return flow;
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
