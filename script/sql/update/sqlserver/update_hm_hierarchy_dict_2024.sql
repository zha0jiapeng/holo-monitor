-- 更新 hm_hierarchy_type_property_dict 表，添加 system_flag 和 dict_key 字段
-- 执行日期: 2024年
-- 描述: 为层级类型属性字典表添加系统属性标识和字典key字段

ALTER TABLE [hm_hierarchy_type_property_dict]
    ADD [system_flag] TINYINT DEFAULT 0,
        [dict_key] NVARCHAR(255);

-- 添加注释 (SQL Server 使用扩展属性)
EXEC sp_addextendedproperty
    @name = N'MS_Description',
    @value = N'系统属性',
    @level0type = N'Schema', @level0name = 'dbo',
    @level1type = N'Table', @level1name = 'hm_hierarchy_type_property_dict',
    @level2type = N'Column', @level2name = 'system_flag';

EXEC sp_addextendedproperty
    @name = N'MS_Description',
    @value = N'字典key',
    @level0type = N'Schema', @level0name = 'dbo',
    @level1type = N'Table', @level1name = 'hm_hierarchy_type_property_dict',
    @level2type = N'Column', @level2name = 'dict_key';
