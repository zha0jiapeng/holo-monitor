package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.dromara.common.core.domain.R;
import org.dromara.hm.enums.TemplateEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板枚举Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/templateEnum")
public class TemplateEnumController {

    /**
     * 获取所有模板枚举值
     */
    @SaCheckPermission("hm:templateEnum:list")
    @GetMapping("/list")
    public R<List<Map<String, String>>> getAllEnums() {
        List<Map<String, String>> result = Arrays.stream(TemplateEnum.values())
            .map(enumValue -> {
                Map<String, String> map = new HashMap<>();
                map.put("name", enumValue.getName());
                map.put("template", enumValue.getTemplate());
                return map;
            })
            .collect(Collectors.toList());
        return R.ok(result);
    }

    /**
     * 根据名称获取模板枚举
     *
     * @param name 模板名称
     */
    @SaCheckPermission("hm:templateEnum:query")
    @GetMapping("/byName/{name}")
    public R<Map<String, String>> getByName(@NotBlank(message = "模板名称不能为空") @PathVariable String name) {
        TemplateEnum enumValue = TemplateEnum.getByName(name);
        if (enumValue == null) {
            return R.fail("未找到对应的模板枚举");
        }

        Map<String, String> result = new HashMap<>();
        result.put("name", enumValue.getName());
        result.put("template", enumValue.getTemplate());
        return R.ok(result);
    }

    /**
     * 获取模板类型映射
     */
    @SaCheckPermission("hm:templateEnum:list")
    @GetMapping("/typeMap")
    public R<Map<String, String>> getTypeMap() {
        return R.ok(TemplateEnum.getTypeMap());
    }

    /**
     * 获取模板名称列表
     */
    @SaCheckPermission("hm:templateEnum:list")
    @GetMapping("/names")
    public R<List<String>> getNames() {
        List<String> names = Arrays.stream(TemplateEnum.values())
            .map(TemplateEnum::getName)
            .collect(Collectors.toList());
        return R.ok(names);
    }

    /**
     * 获取模板类型列表
     */
    @SaCheckPermission("hm:templateEnum:list")
    @GetMapping("/templates")
    public R<List<String>> getTemplates() {
        List<String> templates = Arrays.stream(TemplateEnum.values())
            .map(TemplateEnum::getTemplate)
            .distinct()
            .collect(Collectors.toList());
        return R.ok(templates);
    }

    /**
     * 验证模板名称是否存在
     *
     * @param name 模板名称
     */
    @SaCheckPermission("hm:templateEnum:query")
    @GetMapping("/exists/{name}")
    public R<Boolean> existsByName(@NotBlank(message = "模板名称不能为空") @PathVariable String name) {
        TemplateEnum enumValue = TemplateEnum.getByName(name);
        return R.ok(enumValue != null);
    }

}
