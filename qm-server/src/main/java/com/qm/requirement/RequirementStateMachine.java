package com.qm.requirement;

import com.qm.common.exception.BizException;
import com.qm.requirement.entity.Requirement;
import com.qm.requirement.mapper.RequirementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementStateMachine {

    private final RequirementMapper requirementMapper;

    @Transactional
    public void transition(String reqId, RequirementState target, String operatorId, String comment) {
        Requirement req = requirementMapper.selectById(reqId);
        if (req == null) {
            throw BizException.notFound("需求");
        }

        RequirementState current = RequirementState.valueOf(req.getStatus());
        if (!current.canTransitionTo(target)) {
            throw BizException.illegalState(
                String.format("状态[%s]不允许流转到[%s]", current, target));
        }

        req.setStatus(target.name());
        requirementMapper.updateById(req);

        log.info("Requirement {}: {} -> {} (by {}, comment: {})",
            reqId, current, target, operatorId, comment);
    }

    public void assertCan(Requirement req, RequirementState action) {
        RequirementState current = RequirementState.valueOf(req.getStatus());
        if (!current.canTransitionTo(action)) {
            throw BizException.illegalState(
                String.format("当前状态[%s]不允许执行[%s]", current, action));
        }
    }
}
