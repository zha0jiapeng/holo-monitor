package org.dromara.common.web.annotation;

import java.lang.annotation.*;

/**
 * Brotli压缩注解
 * 用于标记需要进行Brotli压缩的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrotliCompress {

    /**
     * 是否启用压缩
     */
    boolean enabled() default true;

    /**
     * 压缩等级，取值范围 0-11
     * 0: 最快但压缩率最低
     * 11: 最慢但压缩率最高
     * 默认值6提供较好的压缩率和速度平衡
     */
    int quality() default 6;

    /**
     * 压缩阈值（字节）
     * 只有当响应体大小超过此阈值时才进行压缩
     * 默认1024字节（1KB）
     */
    int threshold() default 1024;

    /**
     * 窗口大小位数，取值范围 10-24
     * 值越大，压缩率越高，但内存使用也越大
     * 默认值22
     */
    int window() default 22;
}
