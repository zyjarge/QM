package com.qm.integration.im.feishu;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FeishuClient {

    @Value("${qm.feishu.app-id}")
    private String appId;

    @Value("${qm.feishu.app-secret}")
    private String appSecret;

    private volatile String tenantToken;
    private volatile long tokenExpireAt;

    private static final String BASE = "https://open.feishu.cn/open-apis";
    private static final long TOKEN_TTL = TimeUnit.SECONDS.toMillis(7000);

    public synchronized String getTenantToken() {
        if (tenantToken != null && System.currentTimeMillis() < tokenExpireAt) {
            return tenantToken;
        }
        JSONObject body = new JSONObject();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);

        try (HttpResponse resp = HttpRequest.post(BASE + "/auth/v3/tenant_access_token/internal")
                .body(body.toJSONString())
                .execute()) {
            JSONObject result = JSONObject.parseObject(resp.body());
            if (result.getIntValue("code") != 0) {
                throw new RuntimeException("获取 tenant_token 失败: " + result.getString("msg"));
            }
            tenantToken = result.getString("tenant_access_token");
            tokenExpireAt = System.currentTimeMillis() + TOKEN_TTL;
            return tenantToken;
        }
    }

    public JSONObject post(String path, Object body) {
        String token = getTenantToken();
        try (HttpResponse resp = HttpRequest.post(BASE + path)
                .header("Authorization", "Bearer " + token)
                .body(body instanceof String ? (String) body : JSONObject.toJSONString(body))
                .execute()) {
            return JSONObject.parseObject(resp.body());
        }
    }

    public JSONObject put(String path, Object body) {
        String token = getTenantToken();
        try (HttpResponse resp = HttpRequest.put(BASE + path)
                .header("Authorization", "Bearer " + token)
                .body(body instanceof String ? (String) body : JSONObject.toJSONString(body))
                .execute()) {
            return JSONObject.parseObject(resp.body());
        }
    }

    public JSONObject get(String path, Map<String, Object> params) {
        String token = getTenantToken();
        var req = HttpRequest.get(BASE + path)
                .header("Authorization", "Bearer " + token);
        if (params != null) {
            params.forEach(req::form);
        }
        try (HttpResponse resp = req.execute()) {
            return JSONObject.parseObject(resp.body());
        }
    }

    public JSONObject delete(String path) {
        String token = getTenantToken();
        try (HttpResponse resp = HttpRequest.delete(BASE + path)
                .header("Authorization", "Bearer " + token)
                .execute()) {
            return JSONObject.parseObject(resp.body());
        }
    }
}
