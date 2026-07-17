package com.qm.notify;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qm.common.RabbitMQConfig;
import com.qm.notify.entity.Notification;
import com.qm.notify.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送通知（入 DB + 入 MQ 异步投递）
     */
    @Transactional
    public Notification send(String userId, String type, String title, String content,
                              String channel, String payload) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setChannel(channel == null ? "web" : channel);
        n.setStatus("pending");
        // 将 title/content 合并到 payload
        String mergedPayload = payload == null 
            ? String.format("{\"title\":\"%s\",\"content\":\"%s\"}", title, content)
            : payload;
        n.setPayload(mergedPayload);
        notificationMapper.insert(n);

        // 如果是 IM 渠道，发 MQ 异步投递
        if ("feishu".equals(channel) || "wecom".equals(channel)) {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.RK_NOTIFY,
                Map.of("notificationId", n.getId(), "userId", userId,
                       "channel", channel, "title", title, "content", content)
            );
        }

        log.info("Notification sent: user={} type={} channel={}", userId, type, channel);
        return n;
    }

    /**
     * 消费 MQ 通知，投递到 IM
     */
    @RabbitListener(queues = "qm.notify")
    public void deliverToIm(Map<String, Object> msg) {
        String channel = (String) msg.get("channel");
        String userId = (String) msg.get("userId");
        String title = (String) msg.get("title");
        
        // TODO: 实际调用飞书/企微 API 发送私信
        // P0 先只记录日志，P1 接入真实 IM 投递
        log.info("Delivering to IM: channel={} user={} title={}", channel, userId, title);
    }

    /**
     * 用户的未读通知列表
     */
    public List<Notification> listUnread(String userId) {
        return notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, "pending")
                .orderByDesc(Notification::getCreatedAt));
    }

    /**
     * 用户的通知列表（分页）
     */
    public Page<Notification> listByUser(String userId, int page, int size) {
        return notificationMapper.selectPage(new Page<>(page, size),
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt));
    }

    /**
     * 标记已读
     */
    @Transactional
    public void markRead(String notificationId, String userId) {
        notificationMapper.update(null,
            new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getStatus, "read")
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllRead(String userId) {
        notificationMapper.update(null,
            new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, "pending")
                .set(Notification::getStatus, "read")
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    /**
     * 未读数量
     */
    public Long countUnread(String userId) {
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, "pending"));
    }
}
