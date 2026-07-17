package com.qm.workflow;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.common.exception.BizException;
import com.qm.requirement.RequirementState;
import com.qm.requirement.RequirementService;
import com.qm.requirement.entity.Requirement;
import com.qm.requirement.entity.RequirementVersion;
import com.qm.workflow.entity.Baseline;
import com.qm.workflow.mapper.BaselineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaselineService {

    private final BaselineMapper baselineMapper;
    private final RequirementService requirementService;

    public Baseline getCurrentBaseline(String reqId) {
        return baselineMapper.selectOne(
            new LambdaQueryWrapper<Baseline>()
                .eq(Baseline::getRequirementId, reqId)
                .orderByDesc(Baseline::getSignedAt)
                .last("LIMIT 1"));
    }

    @Transactional
    public Baseline signBaseline(String reqId, String operatorId, String ip, String userAgent) {
        Requirement req = requirementService.getById(reqId);
        if (!RequirementState.pending_sign.name().equals(req.getStatus())) {
            throw BizException.illegalState("当前状态不允许基线签认");
        }

        RequirementVersion current = requirementService.getCurrentVersion(reqId);
        if (current == null) throw BizException.illegalState("当前无有效版本");

        // 检查是否已有基线
        Baseline existing = getCurrentBaseline(reqId);
        if (existing != null && existing.getContentHash().equals(current.getContentHash())) {
            throw BizException.illegalState("当前版本已签认基线，无需重复签认");
        }

        // 创建基线
        Baseline baseline = new Baseline();
        baseline.setRequirementId(reqId);
        baseline.setVersionId(current.getId());
        baseline.setContentHash(current.getContentHash());
        baseline.setSnapshot(current.getContent()); // 快照内容
        baseline.setSignedBy(operatorId);
        baseline.setSignedAt(LocalDateTime.now());
        baseline.setSignatureMeta(String.format(
            "{\"ip\":\"%s\",\"userAgent\":\"%s\"}", ip, userAgent));
        baselineMapper.insert(baseline);

        // 驱动状态机
        requirementService.transition(reqId, "baselined", operatorId, "基线签认");

        log.info("Baseline signed: req={} hash={} by={}", reqId, current.getContentHash().substring(0, 16), operatorId);
        return baseline;
    }

    public List<Baseline> listBaselines(String reqId) {
        return baselineMapper.selectList(
            new LambdaQueryWrapper<Baseline>()
                .eq(Baseline::getRequirementId, reqId)
                .orderByDesc(Baseline::getSignedAt));
    }
}
