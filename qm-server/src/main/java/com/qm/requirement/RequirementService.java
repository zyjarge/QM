package com.qm.requirement;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qm.common.exception.BizException;
import com.qm.requirement.entity.Requirement;
import com.qm.requirement.entity.RequirementVersion;
import com.qm.requirement.entity.RequirementStakeholder;
import com.qm.requirement.mapper.RequirementMapper;
import com.qm.requirement.mapper.RequirementVersionMapper;
import com.qm.requirement.mapper.RequirementStakeholderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementMapper requirementMapper;
    private final RequirementVersionMapper versionMapper;
    private final RequirementStakeholderMapper stakeholderMapper;
    private final RequirementStateMachine stateMachine;

    private static final String REQ_NO_PREFIX = "REQ-";

    public Page<Requirement> page(int page, int size, String status, String reqType,
                                   String productLine, String keyword) {
        LambdaQueryWrapper<Requirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Requirement::getStatus, status)
               .eq(reqType != null, Requirement::getReqType, reqType)
               .eq(productLine != null, Requirement::getProductLine, productLine)
               .like(keyword != null, Requirement::getTitle, keyword)
               .orderByDesc(Requirement::getCreatedAt);
        return requirementMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public java.util.List<Requirement> listAll() {
        return requirementMapper.selectList(null);
    }

    public Requirement getById(String id) {
        Requirement req = requirementMapper.selectById(id);
        if (req == null) throw BizException.notFound("需求");
        return req;
    }

    @Transactional
    public Requirement create(Requirement req, String operatorId) {
        // 生成编号
        req.setReqNo(generateReqNo());
        req.setStatus(RequirementState.draft.name());
        req.setCreatedBy(operatorId);
        req.setSourceChannel(req.getSourceChannel() == null ? "web" : req.getSourceChannel());
        req.setIsConfidential(req.getIsConfidential() != null && req.getIsConfidential());

        requirementMapper.insert(req);

        // 创建初始版本（content 传 null, 仅存 contentText 用于后续编辑）
        RequirementVersion v = new RequirementVersion();
        v.setRequirementId(req.getId());
        v.setVersionNo(1);
        v.setContentText(req.getTitle());
        v.setContentHash(SecureUtil.sha256(req.getTitle()));
        v.setEditedBy(operatorId);
        versionMapper.insertVersion(v);

        req.setCurrentVersionId(v.getId());
        requirementMapper.updateById(req);

        // 创建干系人记录(创建者=requester)
        RequirementStakeholder sh = new RequirementStakeholder();
        sh.setRequirementId(req.getId());
        sh.setUserId(operatorId);
        sh.setStakeholderRole("requester");
        sh.setAddedBy(operatorId);
        stakeholderMapper.insert(sh);

        log.info("Requirement created: {} {}", req.getReqNo(), req.getTitle());
        return req;
    }

    @Transactional
    public RequirementVersion saveVersion(String reqId, String content, String fieldsData,
                                          String changeSummary, String operatorId) {
        Requirement req = getById(reqId);
        if (RequirementState.baselined.name().equals(req.getStatus())) {
            throw BizException.illegalState("需求已基线，需走变更流程");
        }

        RequirementVersion v = new RequirementVersion();
        v.setRequirementId(reqId);
        int nextVer = getNextVersionNo(reqId);
        v.setVersionNo(nextVer);
        v.setContent(content);
        v.setContentText(content); // TODO: strip markdown/html for search
        v.setContentHash(SecureUtil.sha256(content));
        v.setFieldsData(fieldsData);
        v.setEditedBy(operatorId);
        v.setChangeSummary(changeSummary);
        versionMapper.insertVersion(v);

        req.setCurrentVersionId(v.getId());
        requirementMapper.updateById(req);

        return v;
    }

    public RequirementVersion getCurrentVersion(String reqId) {
        Requirement req = getById(reqId);
        if (req.getCurrentVersionId() == null) return null;
        return versionMapper.selectById(req.getCurrentVersionId());
    }

    public List<RequirementVersion> getVersions(String reqId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<RequirementVersion>()
                .eq(RequirementVersion::getRequirementId, reqId)
                .orderByDesc(RequirementVersion::getVersionNo));
    }

    public List<RequirementStakeholder> getStakeholders(String reqId) {
        return stakeholderMapper.selectList(
            new LambdaQueryWrapper<RequirementStakeholder>()
                .eq(RequirementStakeholder::getRequirementId, reqId));
    }

    @Transactional
    public void addStakeholder(String reqId, String userId, String role, String operatorId) {
        RequirementStakeholder sh = new RequirementStakeholder();
        sh.setRequirementId(reqId);
        sh.setUserId(userId);
        sh.setStakeholderRole(role);
        sh.setAddedBy(operatorId);
        sh.setAddedAt(LocalDateTime.now());
        stakeholderMapper.insert(sh);
    }

    @Transactional
    public void transition(String reqId, String targetState, String operatorId, String comment) {
        stateMachine.transition(reqId, RequirementState.valueOf(targetState), operatorId, comment);
    }

    private String generateReqNo() {
        // 用 DB sequence 避免 LIKE 查询
        Long seq = requirementMapper.selectReqNoSeq();
        String year = String.valueOf(Year.now().getValue());
        return String.format(REQ_NO_PREFIX + "%s-%04d", year, seq);
    }

    private int getNextVersionNo(String reqId) {
        LambdaQueryWrapper<RequirementVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementVersion::getRequirementId, reqId)
               .orderByDesc(RequirementVersion::getVersionNo)
               .last("LIMIT 1");
        RequirementVersion last = versionMapper.selectOne(wrapper);
        return last == null ? 1 : last.getVersionNo() + 1;
    }
}
