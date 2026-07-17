package com.qm.org;

import com.qm.common.Result;
import com.qm.integration.im.feishu.FeishuSsoService;
import com.qm.org.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final FeishuSsoService feishuSsoService;

    @Operation(summary = "飞书免登 - 获取登录 URL")
    @GetMapping("/feishu/login-url")
    public Result<Map<String, String>> getLoginUrl(
            @RequestParam String redirectUri) {
        return Result.ok(Map.of("url", feishuSsoService.getLoginUrl(redirectUri)));
    }

    @Operation(summary = "飞书免登 - code 换用户")
    @PostMapping("/feishu/login")
    public Result<User> feishuLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String redirectUri = body.get("redirectUri");
        if (code == null || code.isEmpty()) {
            return Result.error("QM-4001", "code 不能为空");
        }
        if (redirectUri == null || redirectUri.isEmpty()) {
            return Result.error("QM-4002", "redirectUri 不能为空");
        }
        try {
            User user = feishuSsoService.loginByCode(code, redirectUri);
            return Result.ok(user);
        } catch (Exception e) {
            log.error("Feishu login failed", e);
            return Result.error("QM-4000", e.getMessage());
        }
    }
}
