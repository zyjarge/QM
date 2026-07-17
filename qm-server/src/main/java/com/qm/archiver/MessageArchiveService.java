package com.qm.archiver;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.archiver.entity.MessageArchive;
import com.qm.archiver.mapper.MessageArchiveMapper;
import com.qm.groupengine.GroupService;
import com.qm.groupengine.entity.RequirementGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageArchiveService {

    private final MessageArchiveMapper archiveMapper;
    private final GroupService groupService;

    /**
     * 从 MQ 消费群消息事件，归档到 DB
     * 幂等：im_msg_id 唯一索引，重复消费直接跳过
     */
    @RabbitListener(queues = "qm.archiver.message")
    @Transactional
    public void archiveMessage(Map<String, Object> event) {
        String msgId = (String) event.get("msgId");
        String chatId = (String) event.get("chatId");
        String provider = (String) event.getOrDefault("provider", "feishu");
        
        // 幂等检查
        if (archiveMapper.selectCount(
            new LambdaQueryWrapper<MessageArchive>()
                .eq(MessageArchive::getImMsgId, msgId)
                .eq(MessageArchive::getImProvider, provider)) > 0) {
            log.debug("Message already archived: {}", msgId);
            return;
        }
        
        // 通过 chatId 找到关联需求
        RequirementGroup group = groupService.getByChatId(chatId);
        if (group == null) {
            log.warn("No requirement found for chat: {}", chatId);
            return;
        }
        
        // 归档消息
        MessageArchive archive = new MessageArchive();
        archive.setRequirementId(group.getRequirementId());
        archive.setImMsgId(msgId);
        archive.setImProvider(provider);
        archive.setSenderId((String) event.get("senderId"));
        archive.setMsgType((String) event.getOrDefault("msgType", "text"));
        archive.setContent((String) event.get("content"));
        archive.setContentText((String) event.get("contentText"));
        archive.setMsgTime(LocalDateTime.parse((String) event.get("msgTime")));
        archive.setIsKeyInfo(false); // AI 打标后续做
        archiveMapper.insert(archive);
        
        log.info("Message archived: req={} msgId={} type={}", 
            group.getRequirementId(), msgId, archive.getMsgType());
    }

    public List<MessageArchive> listByReqId(String reqId, Integer limit) {
        return archiveMapper.selectList(
            new LambdaQueryWrapper<MessageArchive>()
                .eq(MessageArchive::getRequirementId, reqId)
                .orderByAsc(MessageArchive::getMsgTime)
                .last("LIMIT " + (limit == null ? 100 : limit)));
    }

    public Long countByReqId(String reqId) {
        return archiveMapper.selectCount(
            new LambdaQueryWrapper<MessageArchive>()
                .eq(MessageArchive::getRequirementId, reqId));
    }
}
