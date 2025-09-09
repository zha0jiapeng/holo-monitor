-- 更新 hm_hierarchy_type_property_dict 表，添加 system_flag 和 dict_key 字段
-- 执行日期: 2024年
-- 描述: 为层级类型属性字典表添加系统属性标识和字典key字段

ALTER TABLE `hm_hierarchy_type_property_dict`
    ADD COLUMN `system_flag` tinyint DEFAULT '0' COMMENT '系统属性' AFTER `dict_values`,
    ADD COLUMN `dict_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字典key' AFTER `system_flag`;
