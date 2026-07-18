package com.qm.audit;

import com.alibaba.fastjson2.JSONObject;
import com.qm.audit.entity.AuditLog;
import com.qm.audit.mapper.AuditLogMapper;
import com.qm.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 写操作审计切面（设计红线 05/06：所有写 API 自动落 audit_logs，只增不改）
 * 覆盖 /api/v1 下 POST/PUT/DELETE/PATCH；飞书回调（高频入口，业务表自带留痕）与认证接口不拦截。
 * 审计写入失败只记日志，不阻断业务。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)"
        + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
        + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)"
        + " || @annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public void writeOperation() {
    }

    @AfterReturning(pointcut = "restController() && writeOperation()", returning = "result")
    public void auditSuccess(JoinPoint jp, Object result) {
        // Result 包装的业务失败（如 QM-4xxx 状态机拒绝）也如实记录
        String outcome = "success";
        String error = null;
        if (result instanceof Result<?> r && !"QM-0000".equals(r.getCode())) {
            outcome = "biz_fail";
            error = r.getCode() + " " + r.getMessage();
        }
        record(jp, outcome, error);
    }

    @AfterThrowing(pointcut = "restController() && writeOperation()", throwing = "e")
    public void auditFail(JoinPoint jp, Throwable e) {
        record(jp, "exception", e.getMessage());
    }

    private void record(JoinPoint jp, String outcome, String error) {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest req = attrs.getRequest();
            String uri = req.getRequestURI();
            if (!uri.startsWith("/api/v1/")
                || uri.startsWith("/api/v1/feishu/")
                || uri.startsWith("/api/v1/auth/")) {
                return;
            }

            String[] seg = uri.split("/");
            JSONObject detail = new JSONObject();
            detail.put("outcome", outcome);
            if (error != null) {
                detail.put("error", abbreviate(error, 300));
            }
            detail.put("args", abbreviate(Arrays.toString(jp.getArgs()), 500));

            AuditLog entry = new AuditLog();
            entry.setActorId(req.getHeader("X-User-Id"));
            entry.setAction(req.getMethod() + " " + uri);
            entry.setTargetType(seg.length > 3 ? seg[3] : null);
            entry.setTargetId(seg.length > 4 ? seg[4] : null);
            entry.setDetail(detail.toJSONString());
            entry.setIp(req.getRemoteAddr());
            entry.setUserAgent(abbreviate(req.getHeader("User-Agent"), 200));
            auditLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("Audit record failed: {}", e.getMessage());
        }
    }

    private String abbreviate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
