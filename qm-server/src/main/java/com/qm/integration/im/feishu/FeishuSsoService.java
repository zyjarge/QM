package com.qm.integration.im.feishu;

import com.alibaba.fastjson2.JSONObject;
import com.qm.common.exception.BizException;
import com.qm.org.UserService;
import com.qm.org.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    @Value("${qm.feishu.app-id:}")
    private String appId;

    @Value("${qm.feishu.app-secret:}")
    private String appSecret;

    /**
     * 用 OAuth code 换用户信息并登录
     */
    public User loginByCode(String code, String redirectUri) {
        // 1. code 换 user_access_token
        //    v2 接口：标准 OAuth2 响应，access_token 在顶层（无 code/data 包装）；
        //    请求必须是 JSON；失败时返回 HTTP 4xx，body 为 {"error":..., "error_description":...}
        String tokenUrl = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
        JSONObject tokenReq = new JSONObject();
        tokenReq.put("grant_type", "authorization_code");
        tokenReq.put("code", code);
        tokenReq.put("client_id", appId);
        tokenReq.put("client_secret", appSecret);
        tokenReq.put("redirect_uri", redirectUri);

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject tokenResp;
        try {
            tokenResp = restTemplate.postForObject(tokenUrl,
                    new HttpEntity<>(tokenReq, tokenHeaders), JSONObject.class);
        } catch (HttpStatusCodeException e) {
            JSONObject errBody = tryParse(e.getResponseBodyAsString());
            String desc = errBody != null && errBody.getString("error_description") != null
                    ? errBody.getString("error_description")
                    : e.getResponseBodyAsString();
            log.error("Feishu OAuth token failed: {} {}", e.getStatusCode(), desc);
            throw BizException.illegalState("飞书登录失败: " + desc);
        }
        if (tokenResp == null || tokenResp.getString("access_token") == null) {
            log.error("Feishu OAuth token failed: {}", tokenResp);
            throw BizException.illegalState("飞书登录失败: 未获取到 access_token");
        }
        String userAccessToken = tokenResp.getString("access_token");

        // 2. 用 user_access_token 获取用户信息（GET，响应有 code/data 包装）
        String userInfoUrl = "https://open.feishu.cn/open-apis/authen/v1/user_info";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userAccessToken);
        JSONObject userInfoResp = restTemplate.exchange(userInfoUrl, HttpMethod.GET,
                new HttpEntity<>(headers), JSONObject.class).getBody();

        if (userInfoResp == null || userInfoResp.getIntValue("code") != 0) {
            log.error("Feishu user_info failed: {}", userInfoResp);
            throw BizException.illegalState("飞书登录失败: 获取用户信息失败");
        }
        JSONObject userData = userInfoResp.getJSONObject("data");
        String openId = userData.getString("open_id");
        String name = userData.getString("name");
        String email = userData.getString("email");

        log.info("Feishu OAuth success: openId={} name={}", openId, name);

        // 3. 查找或创建用户
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
            appId, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
    }

    private JSONObject tryParse(String body) {
        try {
            return JSONObject.parseObject(body);
        } catch (Exception e) {
            return null;
        }
    }
}
