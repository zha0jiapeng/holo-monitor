-- 同步 show_name 字段的 SQL 脚本
-- 当 show_name 为空时，自动取 name 的值

-- 1. 同步 Hierarchy 表的 show_name
UPDATE hm_hierarchy 
SET show_name = name 
WHERE show_name IS NULL OR show_name = '';

-- 2. 同步 Equipment 表的 show_name  
UPDATE hm_equipment 
SET show_name = name 
WHERE show_name IS NULL OR show_name = '';

-- 3. 同步 Testpoint 表的 show_name
UPDATE hm_testpoint 
SET show_name = name 
WHERE show_name IS NULL OR show_name = '';

-- 可选：添加触发器自动同步（MySQL示例）
-- 当插入新记录时，如果 show_name 为空，自动设置为 name

-- Hierarchy 表触发器
DELIMITER $$
CREATE TRIGGER tr_hierarchy_insert_show_name 
BEFORE INSERT ON hm_hierarchy
FOR EACH ROW
BEGIN
    IF NEW.show_name IS NULL OR NEW.show_name = '' THEN
        SET NEW.show_name = NEW.name;
    END IF;
END$$

CREATE TRIGGER tr_hierarchy_update_show_name 
BEFORE UPDATE ON hm_hierarchy
FOR EACH ROW
BEGIN
    IF NEW.show_name IS NULL OR NEW.show_name = '' THEN
        SET NEW.show_name = NEW.name;
    END IF;
END$$

-- Equipment 表触发器
CREATE TRIGGER tr_equipment_insert_show_name 
BEFORE INSERT ON hm_equipment
FOR EACH ROW
BEGIN
    IF NEW.show_name IS NULL OR NEW.show_name = '' THEN
        SET NEW.show_name = NEW.name;
    END IF;
END$$

CREATE TRIGGER tr_equipment_update_show_name 
BEFORE UPDATE ON hm_equipment
FOR EACH ROW
BEGIN
    IF NEW.show_name IS NULL OR NEW.show_name = '' THEN
        SET NEW.show_name = NEW.name;
    END IF;
END$$

-- Testpoint 表触发器
CREATE TRIGGER tr_testpoint_insert_show_name 
BEFORE INSERT ON hm_testpoint
FOR EACH ROW
BEGIN
    IF NEW.show_name IS NULL OR NEW.show_name = '' THEN
        SET NEW.show_name = NEW.name;
    END IF;
END$$

CREATE TRIGGER tr_testpoint_update_show_name 
BEFORE UPDATE ON hm_testpoint
FOR EACH ROW
BEGIN
    IF NEW.show_name IS NULL OR NEW.show_name = '' THEN
        SET NEW.show_name = NEW.name;
    END IF;
END$$

DELIMITER ;
