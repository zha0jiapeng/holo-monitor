-- 更新 hm_hierarchy_type_property_dict 表，添加 system_flag 和 dict_key 字段
-- 执行日期: 2024年
-- 描述: 为层级类型属性字典表添加系统属性标识和字典key字段

ALTER TABLE hm_hierarchy_type_property_dict
    ADD COLUMN system_flag SMALLINT DEFAULT 0,
    ADD COLUMN dict_key VARCHAR(255);

-- 添加注释
COMMENT ON COLUMN hm_hierarchy_type_property_dict.system_flag IS '系统属性';
COMMENT ON COLUMN hm_hierarchy_type_property_dict.dict_key IS '字典key';
