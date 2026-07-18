package com.qm.integration.im.feishu;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import com.alibaba.fastjson2.JSONObject;
import com.qm.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 飞书回调安全：验签 + 解密 + 时间戳防重放（设计红线 03-im-integration §安全）
 * - 配置了 encrypt-key 后：强制验签（X-Lark-Signature）+ 支持 {"encrypt": ...} 加密体解密
 * - 未配置（本地开发态）：放行并告警日志
 */
@Slf4j
@Service
public class FeishuSecurityService {

    /** 时间戳容差 ±5 分钟（防重放） */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    @Value("${qm.feishu.encrypt-key:}")
    private String encryptKey;

    @Value("${qm.feishu.verification-token:}")
    private String verificationToken;

    public boolean isSecurityConfigured() {
        return encryptKey != null && !encryptKey.isEmpty();
    }

    /**
     * 验签：signature = sha256_hex(timestamp + "\n" + nonce + "\n" + encryptKey + "\n" + rawBody)
     * encrypt-key 未配置时放行（仅开发态），配置后缺头/过期/不符一律拒绝
     */
    public void verifySignature(String timestamp, String nonce, String signature, String rawBody) {
        if (!isSecurityConfigured()) {
            log.debug("Feishu encrypt-key not configured, skip signature verification (dev mode)");
            return;
        }
        if (timestamp == null || nonce == null || signature == null) {
            throw BizException.forbidden("缺少飞书签名头");
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw BizException.forbidden("非法的回调时间戳");
        }
        if (Math.abs(System.currentTimeMillis() / 1000 - ts) > TIMESTAMP_TOLERANCE_SECONDS) {
            throw BizException.forbidden("回调时间戳超出容差(±5min)，疑似重放");
        }
        String content = timestamp + "\n" + nonce + "\n" + encryptKey + "\n" + rawBody;
        String expected = DigestUtil.sha256Hex(content);
        if (!expected.equalsIgnoreCase(signature)) {
            throw BizException.forbidden("飞书回调验签失败");
        }
    }

    /**
     * 若 body 为加密格式 {"encrypt": "..."} 则解密后返回明文 JSON，否则原样返回。
     * 解密方案（飞书官方）：key = sha256(encryptKey)，data = base64decode(encrypt)，
     * iv = data[0:16]，明文 = AES-256-CBC-PKCS5 解密 data[16:]
     */
    public String decryptIfNeeded(String rawBody) {
        JSONObject body;
        try {
            body = JSONObject.parseObject(rawBody);
        } catch (Exception e) {
            throw BizException.param("回调体不是合法 JSON");
        }
        String encrypt = body == null ? null : body.getString("encrypt");
        if (encrypt == null) {
            return rawBody;
        }
        if (!isSecurityConfigured()) {
            throw BizException.illegalState("收到加密回调但未配置 encrypt-key");
        }
        try {
            byte[] key = DigestUtil.sha256(encryptKey);
            byte[] data = Base64.decode(encrypt);
            byte[] iv = Arrays.copyOfRange(data, 0, 16);
            byte[] cipher = Arrays.copyOfRange(data, 16, data.length);
            AES aes = new AES("CBC", "PKCS5Padding", key, iv);
            return aes.decryptStr(cipher, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Feishu callback decrypt failed", e);
            throw BizException.forbidden("回调解密失败");
        }
    }

    /**
     * url_verification 的 token 校验：配置了 verification-token 时要求 body.token 一致（缺失也拒绝）
     */
    public void checkVerificationToken(JSONObject body) {
        if (verificationToken == null || verificationToken.isEmpty()) {
            return;
        }
        String token = body == null ? null : body.getString("token");
        if (!verificationToken.equals(token)) {
            throw BizException.forbidden("verification token 不匹配");
        }
    }
}
