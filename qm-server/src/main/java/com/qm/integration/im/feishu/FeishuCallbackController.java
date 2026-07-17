package com.qm.integration.im.feishu;

import com.alibaba.fastjson2.JSONObject;
import com.qm.common.Result;
import com.qm.common.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书事件订阅入口
 * - URL 验证
 * - 卡片回调
 * - 消息事件
 */
@Slf4j
@Tag(name = "飞书回调")
@RestController
@RequestMapping("/api/v1/feishu")
@RequiredArgsConstructor
public class FeishuCallbackController {

    private final FeishuCardCallbackService cardCallbackService;

    /**
     * 飞书事件回调统一入口
     * 飞书会以 POST 发送各种事件过来：
     * - url_verification: URL 验证
     * - event: 各类事件（卡片回调/消息事件等）
     */
    @Operation(summary = "飞书事件回调")
    @PostMapping("/callback")
    public Object callback(@RequestBody JSONObject body,
                           @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
                           @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
                           @RequestHeader(value = "X-Lark-Signature", required = false) String signature) {
        log.info("Feishu callback: body={}", body.toJSONString());

        // URL 验证（飞书配置回调 URL 时会发一次）
        String type = body.getString("type");
        if ("url_verification".equals(type) ||
            (body.containsKey("challenge") && body.getJSONObject("header") == null)) {
            return Map.of("challenge", body.getString("challenge"));
        }

        // 新格式: {schema, header: {event_type}, event: {...}}
        JSONObject header = body.getJSONObject("header");
        if (header != null) {
            String eventType = header.getString("event_type");
            log.info("Event type: {}", eventType);

            // 卡片动作触发（用户在群里点按钮）
            if ("card.action.trigger".equals(eventType) ||
                "interactive_callback".equals(eventType)) {
                return handleCardAction(body);
            }

            // URL 验证（新格式）
            if ("url_verification".equals(eventType) ||
                (body.containsKey("type") && "url_verification".equals(body.getString("type")))) {
                return Map.of("challenge", body.getString("challenge"));
            }

            // 消息事件（群消息归档 - 已由书记员处理）
            if ("im.message.receive_v1".equals(eventType)) {
                return Map.of("code", 0, "msg", "ok");
            }

            return Map.of("code", 0, "msg", "ok");
        }

        return Map.of("code", 0, "msg", "ok");
    }

    private Map<String, Object> handleCardAction(JSONObject body) {
        try {
            JSONObject action = body.getJSONObject("action");
            JSONObject value = null;
            String actionTag = "";

            // 新版卡片 action 格式
            if (action != null) {
                value = action.getJSONObject("value");
                actionTag = action.getString("tag");
            } else if (body.containsKey("challenge")) {
                return Map.of("challenge", body.getString("challenge"));
            }

            if (value == null) {
                // 旧版格式
                JSONObject event = body.getJSONObject("event");
                if (event != null) {
                    action = event.getJSONObject("action");
                    if (action != null) {
                        value = action.getJSONObject("value");
                        actionTag = action.getString("tag");
                    }
                }
            }

            if (value == null) {
                log.warn("No action value in callback: {}", body.toJSONString());
                return Map.of("code", 0, "msg", "ok");
            }

            String callbackType = value.getString("type");
            String callbackId = value.getString("id");
            String callbackAction = value.getString("action");

            // 操作人
            JSONObject operator = body.getJSONObject("operator");
            String openId = null;
            if (operator != null) {
                openId = operator.getString("open_id");
            }
            if (openId == null && body.getJSONObject("event") != null) {
                JSONObject eventOp = body.getJSONObject("event").getJSONObject("operator");
                if (eventOp != null) {
                    openId = eventOp.getString("open_id");
                }
            }

            String eventId = body.getJSONObject("header") != null
                ? body.getJSONObject("header").getString("event_id")
                : body.getString("event_id");
            if (eventId == null) {
                eventId = callbackType + ":" + callbackId + ":" + openId + ":" + System.currentTimeMillis();
            }

            String result = cardCallbackService.handle(
                eventId, callbackAction, callbackType, callbackId, openId);

            // 返回新的卡片（替换原卡片）
            return Map.of(
                "code", 0,
                "msg", "success",
                "card", Map.of(
                    "config", Map.of("wide_screen_mode", true),
                    "header", Map.of(
                        "template", "green",
                        "title", Map.of("tag", "plain_text", "content", "✅ 操作完成")),
                    "elements", java.util.List.of(
                        Map.of("tag", "div",
                            "text", Map.of("tag", "lark_md", "content", result))
                    )
                )
            );
        } catch (BizException e) {
            log.warn("Card callback business error: {}", e.getMessage());
            // 返回新的卡片（红色提示）
            return Map.of(
                "code", 0,
                "msg", "success",
                "card", Map.of(
                    "config", Map.of("wide_screen_mode", true),
                    "header", Map.of(
                        "template", "red",
                        "title", Map.of("tag", "plain_text", "content", "❌ 操作失败")),
                    "elements", java.util.List.of(
                        Map.of("tag", "div",
                            "text", Map.of("tag", "lark_md", "content", e.getMessage()))
                    )
                )
            );
        } catch (Exception e) {
            log.error("Card callback failed", e);
            return Map.of("code", -1, "msg", "系统异常: " + e.getMessage());
        }
    }
}
