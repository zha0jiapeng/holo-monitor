package org.dromara.hm.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dromara.hm.enums.AlgorithmTypeEnum;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 枚举值验证器
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public class EnumValidator implements ConstraintValidator<EnumValid, Object> {

    private Class<? extends Enum<?>> enumClass;
    private String methodName;
    private List<Object> validValues;

    @Override
    public void initialize(EnumValid annotation) {
        this.enumClass = annotation.enumClass();
        this.methodName = annotation.method();

        try {
            // 获取枚举的所有实例
            Enum<?>[] enumConstants = enumClass.getEnumConstants();
            if (enumConstants == null || enumConstants.length == 0) {
                throw new IllegalArgumentException("枚举类" + enumClass.getName() + "没有常量");
            }

            // 获取指定方法的返回值
            Method method = enumClass.getMethod(methodName);
            this.validValues = Arrays.stream(enumConstants)
                    .map(enumConstant -> {
                        try {
                            return method.invoke(enumConstant);
                        } catch (Exception e) {
                            throw new RuntimeException("无法调用枚举方法: " + methodName, e);
                        }
                    })
                    .collect(Collectors.toList());

        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("枚举类" + enumClass.getName() + "没有方法: " + methodName, e);
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // 让@NotNull处理null值
        }

        // 检查值是否在有效值列表中
        return validValues.contains(value);
    }
}
