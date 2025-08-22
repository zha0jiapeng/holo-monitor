package org.dromara.hm.validate;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举值验证注解
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EnumValidator.class)
public @interface EnumValid {

    /**
     * 错误消息
     */
    String message() default "枚举值不正确";

    /**
     * 分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 枚举类
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 枚举值的获取方法名
     */
    String method() default "getCode";

}
