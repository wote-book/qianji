package com.example.finance.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BizException extends RuntimeException {

    private final HttpStatus status;
    private final int code;

    public BizException(String message) {
        this(HttpStatus.BAD_REQUEST, 400, message);
    }

    public BizException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BizException notFound() {
        return new BizException(HttpStatus.NOT_FOUND, 404, "资源不存在");
    }

    public static BizException unauthorized(String message) {
        return new BizException(HttpStatus.UNAUTHORIZED, 401, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(HttpStatus.FORBIDDEN, 403, message);
    }

    public static BizException conflict(String message) {
        return new BizException(HttpStatus.CONFLICT, 409, message);
    }
}
