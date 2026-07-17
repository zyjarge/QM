package com.qm.notify;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qm.common.Result;
import com.qm.notify.entity.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "通知")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "我的未读通知")
    @GetMapping("/unread")
    public Result<List<Notification>> unread(
            @RequestHeader("X-User-Id") String userId) {
        return Result.ok(notificationService.listUnread(userId));
    }

    @Operation(summary = "我的通知列表")
    @GetMapping
    public Result<Page<Notification>> list(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(notificationService.listByUser(userId, page, size));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread/count")
    public Result<Long> unreadCount(
            @RequestHeader("X-User-Id") String userId) {
        return Result.ok(notificationService.countUnread(userId));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/{id}/read")
    public Result<Void> markRead(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        notificationService.markRead(id, userId);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead(
            @RequestHeader("X-User-Id") String userId) {
        notificationService.markAllRead(userId);
        return Result.ok();
    }

    @Operation(summary = "发送通知（测试用）")
    @PostMapping("/test")
    public Result<Notification> sendTest(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        return Result.ok(notificationService.send(
            body.getOrDefault("userId", userId),
            body.getOrDefault("type", "system"),
            body.getOrDefault("title", "测试通知"),
            body.getOrDefault("content", "这是一条测试通知"),
            body.getOrDefault("channel", "web"),
            body.get("payload")
        ));
    }
}
