package com.qm.integration.im.feishu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.qm.common.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 飞书消息事件 → MQ 投递单测。RabbitTemplate mock, 不连 MQ。
 * 事件 JSON 结构参照 im.message.receive_v1。
 */
@ExtendWith(MockitoExtension.class)
class FeishuMessageEventServiceTest {

    /** 1700000000000 ms = 2023-11-14T22:13:20Z = Asia/Shanghai 2023-11-15T06:13:20 */
    private static final String CREATE_TIME_MS = "1700000000000";
    private static final String EXPECTED_ISO_TIME = "2023-11-15T06:13:20";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FeishuMessageEventService service;

    /** 构造 im.message.receive_v1 事件体（仅保留被测代码消费的字段） */
    private JSONObject buildEvent(String chatType, String msgType, String content, String createTime) {
        JSONObject senderId = new JSONObject().fluentPut("open_id", "ou_sender_1");
        JSONObject sender = new JSONObject().fluentPut("sender_id", senderId);
        JSONObject message = new JSONObject()
            .fluentPut("message_id", "om_msg_1")
            .fluentPut("chat_id", "oc_chat_1")
            .fluentPut("chat_type", chatType)
            .fluentPut("message_type", msgType)
            .fluentPut("content", content)
            .fluentPut("create_time", createTime);
        return new JSONObject().fluentPut("sender", sender).fluentPut("message", message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureArchiveEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.RK_ARCHIVER_MESSAGE), captor.capture());
        return (Map<String, Object>) captor.getValue();
    }

    @Test
    @DisplayName("text 消息: 归档字段齐全, contentText 提取正文, 毫秒戳转 ISO 本地时间")
    void textMessageArchived() {
        String rawContent = "{\"text\":\"需求变更：增加导出按钮\"}";
        service.onMessageReceive(buildEvent("group", "text", rawContent, CREATE_TIME_MS));

        Map<String, Object> event = captureArchiveEvent();
        assertThat(event.get("msgId")).isEqualTo("om_msg_1");
        assertThat(event.get("chatId")).isEqualTo("oc_chat_1");
        assertThat(event.get("provider")).isEqualTo("feishu");
        assertThat(event.get("senderId")).isEqualTo("ou_sender_1");
        assertThat(event.get("msgType")).isEqualTo("text");
        assertThat(event.get("content")).isEqualTo(rawContent);
        assertThat(event.get("contentText")).isEqualTo("需求变更：增加导出按钮");

        // msgTime 必须可被 LocalDateTime.parse 解析, 且为 Asia/Shanghai 本地时间
        String msgTime = (String) event.get("msgTime");
        assertThatCode(() -> LocalDateTime.parse(msgTime)).doesNotThrowAnyException();
        assertThat(msgTime).isEqualTo(EXPECTED_ISO_TIME);
        assertThat(LocalDateTime.parse(msgTime)).isEqualTo(LocalDateTime.of(2023, 11, 15, 6, 13, 20));
    }

    @Test
    @DisplayName("post 富文本: 标题+段落+链接拼接, 段落间换行")
    void postMessageTextConcatenated() {
        JSONArray para1 = new JSONArray()
            .fluentAdd(new JSONObject().fluentPut("tag", "text").fluentPut("text", "通过评审，详见"))
            .fluentAdd(new JSONObject().fluentPut("tag", "a").fluentPut("text", "文档")
                .fluentPut("href", "https://example.com/doc"));
        JSONArray para2 = new JSONArray()
            .fluentAdd(new JSONObject().fluentPut("tag", "text").fluentPut("text", "下周排期"));
        JSONObject zhCn = new JSONObject()
            .fluentPut("title", "需求评审结论")
            .fluentPut("content", new JSONArray().fluentAdd(para1).fluentAdd(para2));
        String rawContent = new JSONObject().fluentPut("zh_cn", zhCn).toJSONString();

        service.onMessageReceive(buildEvent("group", "post", rawContent, CREATE_TIME_MS));

        Map<String, Object> event = captureArchiveEvent();
        assertThat(event.get("contentText")).isEqualTo(
            "需求评审结论\n通过评审，详见文档(https://example.com/doc)\n下周排期");
    }

    @Test
    @DisplayName("image 消息: contentText 给 [image] 占位符")
    void imageMessagePlaceholder() {
        service.onMessageReceive(buildEvent("group", "image", "{\"image_key\":\"img_v2_abc\"}", CREATE_TIME_MS));
        assertThat(captureArchiveEvent().get("contentText")).isEqualTo("[image]");
    }

    @Test
    @DisplayName("file 等非文本消息: contentText 给 [file] 占位符")
    void fileMessagePlaceholder() {
        service.onMessageReceive(buildEvent("group", "file", "{\"file_key\":\"file_v2_abc\"}", CREATE_TIME_MS));
        assertThat(captureArchiveEvent().get("contentText")).isEqualTo("[file]");
    }

    @Test
    @DisplayName("p2p 私聊消息不投递 MQ")
    void p2pMessageSkipped() {
        service.onMessageReceive(buildEvent("p2p", "text", "{\"text\":\"私聊\"}", CREATE_TIME_MS));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("null 事件 / 缺 message / 缺 sender 均不投递")
    void malformedEventSkipped() {
        service.onMessageReceive(null);
        service.onMessageReceive(new JSONObject().fluentPut("sender", new JSONObject()));
        service.onMessageReceive(new JSONObject().fluentPut("message", new JSONObject()));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("MQ 投递失败只记日志不重抛（飞书回调需快速 ACK）")
    void mqFailureSwallowed() {
        doThrow(new RuntimeException("broker down"))
            .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        JSONObject event = buildEvent("group", "text", "{\"text\":\"hi\"}", CREATE_TIME_MS);
        assertThatCode(() -> service.onMessageReceive(event)).doesNotThrowAnyException();
    }
}
