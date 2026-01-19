package com.vibe.common.core.domain;

/**
 * 状态码常量
 */
public class StatusCode {

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 失败
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 无权限
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 请求过多
     */
    public static final int TOO_MANY_REQUESTS = 429;

    /**
     * 服务器错误
     */
    public static final int ERROR = 500;

    /**
     * 服务不可用
     */
    public static final int SERVICE_UNAVAILABLE = 503;

    private StatusCode() {
    }
}
