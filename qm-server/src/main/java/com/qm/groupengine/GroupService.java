package com.qm.groupengine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.archiver.ArchiveExportService;
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
    private final ArchiveExportService archiveExportService;

    public RequirementGroup getByReqId(String reqId) {
        return groupMapper.selectOne(
            new LambdaQueryWrapper<RequirementGroup>()
                .eq(RequirementGroup::getRequirementId, reqId)
                .last("LIMIT 1"));
    }

    public RequirementGroup getByChatId(String chatId) {
        return groupMapper.selectOne(
            new LambdaQueryWrapper<RequirementGroup>()
                .eq(RequirementGroup::getChatId, chatId)
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

        // 拉干系人入群
        List<String> stakeholderIds = requirementService.getStakeholders(reqId).stream()
            .map(s -> s.getUserId())
            .filter(id -> id != null && !id.isEmpty())
            .toList();
        if (!stakeholderIds.isEmpty()) {
            try {
                feishuGroupService.addMembers(chatId, stakeholderIds);
                log.info("Stakeholders added: req={} count={}", reqId, stakeholderIds.size());
            } catch (Exception e) {
                log.warn("Failed to add stakeholders: {}", e.getMessage());
            }
        }

        // 发送需求公告卡片到群
        try {
            String announcement = String.format(
                "{\"config\":{\"wide_screen_mode\":true},\"header\":{\"title\":{\"tag\":\"plain_text\",\"content\":\"📋 %s\"}},\"elements\":[{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"**需求标题**: %s\\n**需求类型**: %s\\n**优先级**: %s\\n**创建人**: %s\"}}]}",
                req.getReqNo(), req.getTitle(), req.getReqType(), req.getPriority(), req.getCreatedBy());
            feishuGroupService.sendCard(chatId, announcement);
        } catch (Exception e) {
            log.warn("Failed to send announcement card: {}", e.getMessage());
        }

        log.info("Group created: req={} chatId={} name={}", reqId, chatId, groupName);
        return group;
    }

    @Transactional
    public void dissolveGroup(String reqId, String operatorId) {
        RequirementGroup group = getByReqId(reqId);
        if (group == null || !"active".equals(group.getStatus())) {
            return; // 幂等
        }

        // 先导出群档案到 MinIO：导出失败则中止解散，保证"归档永不丢失"
        String archivePath = archiveExportService.exportRequirementArchive(reqId);
        group.setArchivePath(archivePath);

        // 调用飞书解散群
        feishuGroupService.dissolveGroup(group.getChatId());

        // 更新状态
        group.setStatus("dissolved");
        group.setDissolvedAt(LocalDateTime.now());
        groupMapper.updateById(group);

        log.info("Group dissolved: req={} chatId={} archive={}", reqId, group.getChatId(), archivePath);
    }

    public List<RequirementGroup> listActiveGroups() {
        return groupMapper.selectList(
            new LambdaQueryWrapper<RequirementGroup>()
                .eq(RequirementGroup::getStatus, "active"));
    }
}
