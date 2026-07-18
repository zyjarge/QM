package com.qm.integration.im.feishu;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import com.alibaba.fastjson2.JSONObject;
import com.qm.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 飞书回调安全单测：验签 / 防重放时间戳 / AES-256-CBC 解密往返。
 * 纯单测，用 ReflectionTestUtils 注入配置字段，不起 Spring 上下文。
 */
class FeishuSecurityServiceTest {

    private static final String ENCRYPT_KEY = "test-encrypt-key-0123456789";
    private static final String NONCE = "test-nonce-abc";
    private static final String RAW_BODY = "{\"schema\":\"2.0\",\"header\":{\"event_type\":\"im.message.receive_v1\"}}";

    private FeishuSecurityService service;

    @BeforeEach
    void setUp() {
        service = new FeishuSecurityService();
        ReflectionTestUtils.setField(service, "encryptKey", ENCRYPT_KEY);
        ReflectionTestUtils.setField(service, "verificationToken", "test-verification-token");
    }

    /** 与生产代码相同的签名算法: sha256_hex(timestamp\nnonce\nencryptKey\nrawBody) */
    private String sign(String timestamp, String nonce, String rawBody) {
        return DigestUtil.sha256Hex(timestamp + "\n" + nonce + "\n" + ENCRYPT_KEY + "\n" + rawBody);
    }

    private String nowSeconds() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    @Nested
    @DisplayName("verifySignature 验签")
    class VerifySignature {

        @Test
        @DisplayName("正确签名放行")
        void validSignaturePasses() {
            String ts = nowSeconds();
            assertThatCode(() -> service.verifySignature(ts, NONCE, sign(ts, NONCE, RAW_BODY), RAW_BODY))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("签名不符抛 BizException(QM-2000)")
        void wrongSignatureRejected() {
            String ts = nowSeconds();
            String forged = DigestUtil.sha256Hex("attacker-controlled-content");
            assertThatThrownBy(() -> service.verifySignature(ts, NONCE, forged, RAW_BODY))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-2000");
        }

        @Test
        @DisplayName("body 被篡改后原签名不再匹配")
        void tamperedBodyRejected() {
            String ts = nowSeconds();
            String signature = sign(ts, NONCE, RAW_BODY);
            assertThatThrownBy(() -> service.verifySignature(ts, NONCE, signature, RAW_BODY + " "))
                .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("缺签名头(任一)抛 BizException(QM-2000)")
        void missingHeadersRejected() {
            String ts = nowSeconds();
            String signature = sign(ts, NONCE, RAW_BODY);
            assertThatThrownBy(() -> service.verifySignature(null, NONCE, signature, RAW_BODY))
                .isInstanceOf(BizException.class);
            assertThatThrownBy(() -> service.verifySignature(ts, null, signature, RAW_BODY))
                .isInstanceOf(BizException.class);
            assertThatThrownBy(() -> service.verifySignature(ts, NONCE, null, RAW_BODY))
                .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("时间戳超 ±5min 容差抛异常（防重放）")
        void staleTimestampRejected() {
            long now = System.currentTimeMillis() / 1000;
            for (long ts : new long[]{now - 301, now - 3600, now + 301, now + 3600}) {
                String timestamp = String.valueOf(ts);
                String signature = sign(timestamp, NONCE, RAW_BODY); // 签名本身合法也应被拒
                assertThatThrownBy(() -> service.verifySignature(timestamp, NONCE, signature, RAW_BODY))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getCode())
                    .isEqualTo("QM-2000");
            }
        }

        @Test
        @DisplayName("非数字时间戳抛 BizException(QM-2000)")
        void nonNumericTimestampRejected() {
            assertThatThrownBy(() -> service.verifySignature("not-a-number", NONCE, "whatever", RAW_BODY))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-2000");
        }

        @Test
        @DisplayName("未配置 encrypt-key 时验签放行（本地开发态）")
        void devModeSkipsVerification() {
            ReflectionTestUtils.setField(service, "encryptKey", "");
            assertThatCode(() -> service.verifySignature(null, null, null, RAW_BODY))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("decryptIfNeeded 解密")
    class Decrypt {

        /** 与生产代码相同的加密算法构造密文: key=sha256(encryptKey), data=IV(16B)+AES-256-CBC-PKCS5 密文, base64 */
        private String encrypt(String plain) {
            byte[] key = DigestUtil.sha256(ENCRYPT_KEY);
            byte[] iv = new byte[16]; // 固定 IV, 保证测试可重复
            for (int i = 0; i < iv.length; i++) {
                iv[i] = (byte) i;
            }
            AES aes = new AES("CBC", "PKCS5Padding", key, iv);
            byte[] cipher = aes.encrypt(plain.getBytes(StandardCharsets.UTF_8));
            byte[] data = new byte[iv.length + cipher.length];
            System.arraycopy(iv, 0, data, 0, iv.length);
            System.arraycopy(cipher, 0, data, iv.length, cipher.length);
            return Base64.encode(data);
        }

        @Test
        @DisplayName("加密回调解密往返: 还原出明文 JSON")
        void encryptedBodyRoundTrip() {
            String body = new JSONObject().fluentPut("encrypt", encrypt(RAW_BODY)).toJSONString();
            assertThat(service.decryptIfNeeded(body)).isEqualTo(RAW_BODY);
        }

        @Test
        @DisplayName("非加密 body 原样返回")
        void plainBodyReturnedAsIs() {
            String body = "{\"type\":\"url_verification\",\"token\":\"abc\"}";
            assertThat(service.decryptIfNeeded(body)).isSameAs(body);
        }

        @Test
        @DisplayName("非 JSON body 抛 BizException(QM-1000)")
        void nonJsonBodyRejected() {
            assertThatThrownBy(() -> service.decryptIfNeeded("not-a-json-body"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-1000");
        }

        @Test
        @DisplayName("收到加密回调但未配置 encrypt-key 抛 BizException(QM-4000)")
        void encryptedBodyWithoutKeyRejected() {
            ReflectionTestUtils.setField(service, "encryptKey", "");
            String body = new JSONObject().fluentPut("encrypt", Base64.encode("whatever".getBytes(StandardCharsets.UTF_8)))
                .toJSONString();
            assertThatThrownBy(() -> service.decryptIfNeeded(body))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-4000");
        }

        @Test
        @DisplayName("密文损坏（长度不足 16 字节 IV）抛 BizException(QM-2000)")
        void corruptedCipherRejected() {
            byte[] tooShort = Arrays.copyOf(DigestUtil.sha256("junk"), 8);
            String body = new JSONObject().fluentPut("encrypt", Base64.encode(tooShort)).toJSONString();
            assertThatThrownBy(() -> service.decryptIfNeeded(body))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-2000");
        }
    }
}
