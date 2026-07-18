package com.qm.integration.im.feishu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.qm.common.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 书记员生产端：飞书消息事件 → MQ（qm.archiver.message）
 * 消费端见 com.qm.archiver.MessageArchiveService（幂等靠 (im_provider, im_msg_id) 唯一索引）。
 * 注意：飞书事件回调必须快速 ACK（<3s），本地 MQ 投递是毫秒级，失败只记日志不重抛，
 * 避免 Feishu 重试风暴；归档断链时由错误日志兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuMessageEventService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final RabbitTemplate rabbitTemplate;

    /**
     * 处理 im.message.receive_v1 事件：提取归档字段并投递 MQ
     */
    public void onMessageReceive(JSONObject event) {
        if (event == null) {
            return;
        }
        JSONObject message = event.getJSONObject("message");
        JSONObject sender = event.getJSONObject("sender");
        if (message == null || sender == null) {
            log.warn("Malformed message event: {}", event.toJSONString());
            return;
        }

        // 只归档群聊；私聊机器人（P1 AI 澄清通道）暂不入档
        if (!"group".equals(message.getString("chat_type"))) {
            log.debug("Skip non-group message: chat_type={}", message.getString("chat_type"));
            return;
        }

        String msgType = message.getString("message_type");
        String rawContent = message.getString("content");

        JSONObject senderId = sender.getJSONObject("sender_id");

        Map<String, Object> archiveEvent = new HashMap<>();
        archiveEvent.put("msgId", message.getString("message_id"));
        archiveEvent.put("chatId", message.getString("chat_id"));
        archiveEvent.put("provider", "feishu");
        archiveEvent.put("senderId", senderId != null ? senderId.getString("open_id") : null);
        archiveEvent.put("msgType", msgType);
        archiveEvent.put("content", rawContent);
        archiveEvent.put("contentText", extractText(msgType, rawContent));
        archiveEvent.put("msgTime", toIsoLocal(message.getString("create_time")));

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_ARCHIVER_MESSAGE, archiveEvent);
        } catch (Exception e) {
            log.error("Publish message event failed: msgId={}", archiveEvent.get("msgId"), e);
        }
    }

    /** 提取纯文本（供全文检索/人工阅读）；非文本类型给占位符 */
    private String extractText(String msgType, String rawContent) {
        if (rawContent == null) {
            return null;
        }
        try {
            JSONObject content = JSONObject.parseObject(rawContent);
            return switch (msgType) {
                case "text" -> content.getString("text");
                case "post" -> extractPostText(content);
                default -> "[" + msgType + "]";
            };
        } catch (Exception e) {
            log.warn("Parse message content failed: type={} content={}", msgType, rawContent);
            return rawContent;
        }
    }

    /** post 富文本：拼接所有文本片段（含链接地址），保留段落换行 */
    private String extractPostText(JSONObject content) {
        JSONObject body = content.getJSONObject("zh_cn") != null
            ? content.getJSONObject("zh_cn") : content.getJSONObject("en_us");
        if (body == null) {
            return content.toJSONString();
        }
        StringBuilder sb = new StringBuilder();
        String title = body.getString("title");
        if (title != null && !title.isEmpty()) {
            sb.append(title).append('\n');
        }
        JSONArray paragraphs = body.getJSONArray("content");
        if (paragraphs != null) {
            for (int i = 0; i < paragraphs.size(); i++) {
                JSONArray segments = paragraphs.getJSONArray(i);
                if (segments == null) {
                    continue;
                }
                for (int j = 0; j < segments.size(); j++) {
                    JSONObject seg = segments.getJSONObject(j);
                    String text = seg.getString("text");
                    if (text != null) {
                        sb.append(text);
                    }
                    if ("a".equals(seg.getString("tag"))) {
                        sb.append('(').append(seg.getString("href")).append(')');
                    }
                }
                sb.append('\n');
            }
        }
        return sb.toString().trim();
    }

    /** 飞书 create_time 是毫秒时间戳字符串，转 ISO 本地时间（消费端 LocalDateTime.parse） */
    private String toIsoLocal(String epochMillis) {
        long ms = Long.parseLong(epochMillis);
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZONE)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
