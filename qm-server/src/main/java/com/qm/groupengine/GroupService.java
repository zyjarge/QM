package com.qm.groupengine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.common.exception.BizException;
import com.qm.groupengine.entity.RequirementGroup;
import com.qm.groupengine.mapper.RequirementGroupMapper;
import com.qm.integration.im.feishu.FeishuGroupService;
import com.qm.requirement.RequirementService;
import com.qm.requirement.entity.Requirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final RequirementGroupMapper groupMapper;
    private final FeishuGroupService feishuGroupService;
    private final RequirementService requirementService;

    public RequirementGroup getByReqId(String reqId) {
        return groupMapper.selectOne(
            new LambdaQueryWrapper<RequirementGroup>()
                .eq(RequirementGroup::getRequirementId, reqId)
                .last("LIMIT 1"));
    }

    @Transactional
    public RequirementGroup createGroup(String reqId, String operatorId) {
        Requirement req = requirementService.getById(reqId);
        
        // 保密需求不建群
        if (Boolean.TRUE.equals(req.getIsConfidential())) {
            throw BizException.illegalState("保密需求不建群");
        }

        // 检查是否已有群
        RequirementGroup existing = getByReqId(reqId);
        if (existing != null && "active".equals(existing.getStatus())) {
            return existing; // 幂等
        }

        // 生成群名: [REQ-2026-0001] 需求标题
        String groupName = String.format("[%s] %s", req.getReqNo(), req.getTitle());
        if (groupName.length() > 60) {
            groupName = groupName.substring(0, 57) + "...";
        }

        // 调用飞书建群
        String chatId = feishuGroupService.createGroup(groupName, "需求讨论群", operatorId);

        // 记录到 DB
        RequirementGroup group = new RequirementGroup();
        group.setRequirementId(reqId);
        group.setImProvider("feishu");
        group.setChatId(chatId);
        group.setGroupName(groupName);
        group.setStatus("active");
        groupMapper.insert(group);

        log.info("Group created: req={} chatId={} name={}", reqId, chatId, groupName);
        return group;
    }

    @Transactional
    public void dissolveGroup(String reqId, String operatorId) {
        RequirementGroup group = getByReqId(reqId);
        if (group == null || !"active".equals(group.getStatus())) {
            return; // 幂等
        }

        // 调用飞书解散群
        feishuGroupService.dissolveGroup(group.getChatId());

        // 更新状态
        group.setStatus("dissolved");
        group.setDissolvedAt(LocalDateTime.now());
        groupMapper.updateById(group);

        log.info("Group dissolved: req={} chatId={}", reqId, group.getChatId());
    }

    public List<RequirementGroup> listActiveGroups() {
        return groupMapper.selectList(
            new LambdaQueryWrapper<RequirementGroup>()
                .eq(RequirementGroup::getStatus, "active"));
    }
}
