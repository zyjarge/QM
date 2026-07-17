package com.qm.integration.im.feishu;

import com.alibaba.fastjson2.JSONObject;
import com.qm.common.exception.BizException;
import com.qm.org.UserService;
import com.qm.org.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 飞书免登 SSO
 * 流程：前端 H5 取 code → 后端 code 换 user_access_token → 获取用户信息 → 创建/查找用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuSsoService {

    private final FeishuClient feishuClient;
    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${feishu.app-id:}")
    private String appId;

    @Value("${feishu.app-secret:}")
    private String appSecret;

    /**
     * 用 OAuth code 换用户信息并登录
     */
    public User loginByCode(String code) {
        // 1. code 换 user_access_token
        String tokenUrl = "https://open.feishu.cn/open-apis/authen/v1/oidc/access_token";
        JSONObject tokenReq = new JSONObject();
        tokenReq.put("grant_type", "authorization_code");
        tokenReq.put("code", code);
        tokenReq.put("app_id", appId);
        tokenReq.put("app_secret", appSecret);

        JSONObject tokenResp = restTemplate.postForObject(tokenUrl, tokenReq, JSONObject.class);
        if (tokenResp == null || tokenResp.getIntValue("code") != 0) {
            log.error("Feishu OAuth token failed: {}", tokenResp);
            throw BizException.illegalState("飞书登录失败: " + (tokenResp != null ? tokenResp.getString("msg") : "未知错误"));
        }

        JSONObject data = tokenResp.getJSONObject("data");
        String userAccessToken = data.getString("access_token");
        String openId = data.getString("open_id");
        String name = data.getString("name");
        String email = data.getString("email");

        log.info("Feishu OAuth success: openId={} name={}", openId, name);

        // 2. 查找或创建用户
        User user = userService.getByFeishuOpenId(openId);
        if (user == null) {
            user = userService.createFromFeishu(openId, name, email);
        }

        return user;
    }

    /**
     * 获取飞书登录 URL（前端跳转用）
     */
    public String getLoginUrl(String redirectUri) {
        return String.format(
            "https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=%s&redirect_uri=%s&response_type=code&state=qm",
            appId, redirectUri);
    }
}
