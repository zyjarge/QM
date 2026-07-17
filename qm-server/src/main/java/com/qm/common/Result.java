package com.qm.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = "QM-0000";
        r.message = "success";
        r.data = data;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> Result<T> error(String code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public boolean isSuccess() {
        return "QM-0000".equals(code);
    }
}
