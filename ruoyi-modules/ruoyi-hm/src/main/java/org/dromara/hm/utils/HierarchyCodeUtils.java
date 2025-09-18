package org.dromara.hm.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.dromara.hm.domain.*;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.mapper.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 层级编码生成工具类
 * 
 * @author Mashir0
 * @date 2024-01-01
 */
public class HierarchyCodeUtils {

    /**
     * 生成层级完整编码
     * 根据层级属性中data_type=1001的隐藏属性，按层级类型的code_sort排序组合编码前缀
     * 然后根据前缀去数据库查找已有编码（code LIKE '前缀%'），按id倒序取最大值并递增生成新编码
     * 如果没找到已有编码，则用指定长度补零（如长度3就是001）
     *
     * @param hierarchyId 层级ID
     * @param hierarchyMapper 层级Mapper
     * @param hierarchyPropertyMapper 层级属性Mapper
     * @param hierarchyTypeMapper 层级类型Mapper
     * @param hierarchyTypePropertyMapper 层级类型属性Mapper
     * @param hierarchyTypePropertyDictMapper 层级类型属性字典Mapper
     * @return 生成的编码字符串
     */
    public static Map<String, Object> generateHierarchyCode(Long hierarchyId,
                                                           HierarchyMapper hierarchyMapper,
                                                           HierarchyPropertyMapper hierarchyPropertyMapper,
                                                           HierarchyTypeMapper hierarchyTypeMapper,
                                                           HierarchyTypePropertyMapper hierarchyTypePropertyMapper,
                                                           HierarchyTypePropertyDictMapper hierarchyTypePropertyDictMapper) {
        List<Map<String, Object>> hierarchyCodeInfoList = new ArrayList<>();
        List<Map<String, Object>> configurationList = new ArrayList<>();

        // 查询该层级的所有属性
        List<HierarchyProperty> properties = hierarchyPropertyMapper.selectList(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .eq(HierarchyProperty::getHierarchyId, hierarchyId)
        );

        // 获取当前层级信息（用于后续获取编码长度，但不加入前缀）
        Hierarchy current = hierarchyMapper.selectById(hierarchyId);

        // 遍历属性，找出data_type=1001的隐藏属性
        for (HierarchyProperty property : properties) {
            HierarchyTypeProperty typeProperty = hierarchyTypePropertyMapper.selectById(property.getTypePropertyId());
            if (typeProperty != null) {
                HierarchyTypePropertyDictVo dictVo = hierarchyTypePropertyDictMapper.selectVoById(typeProperty.getPropertyDictId());
                if (dictVo != null && dictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                    // 这是一个层级类型的隐藏属性，property_value代表层级id
                    Long relatedHierarchyId = Long.valueOf(property.getPropertyValue());
                    Hierarchy relatedHierarchy = hierarchyMapper.selectById(relatedHierarchyId);

                    if (relatedHierarchy != null && relatedHierarchy.getCode() != null && !relatedHierarchy.getCode().isEmpty()) {
                        // 获取相关层级的类型信息
                        HierarchyType relatedType = hierarchyTypeMapper.selectById(relatedHierarchy.getTypeId());
                        if (relatedType != null) {
                            Map<String, Object> hierarchyInfo = new HashMap<>();
                            hierarchyInfo.put("code", relatedHierarchy.getCode());
                            hierarchyInfo.put("codeSort", relatedType.getCodeSort() != null ? relatedType.getCodeSort() : 0);
                            hierarchyCodeInfoList.add(hierarchyInfo);
                        }

                        // 获取相关层级的采集配置属性（data_type=1005）
                        List<HierarchyProperty> relatedProperties = hierarchyPropertyMapper.selectList(
                            Wrappers.<HierarchyProperty>lambdaQuery()
                                .eq(HierarchyProperty::getHierarchyId, relatedHierarchyId)
                        );

                        for (HierarchyProperty relatedProperty : relatedProperties) {
                            HierarchyTypeProperty relatedTypeProperty = hierarchyTypePropertyMapper.selectById(relatedProperty.getTypePropertyId());
                            if (relatedTypeProperty != null) {
                                HierarchyTypePropertyDictVo relatedDictVo = hierarchyTypePropertyDictMapper.selectVoById(relatedTypeProperty.getPropertyDictId());
                                if (relatedDictVo != null && relatedDictVo.getDataType().equals(DataTypeEnum.CONFIGURATION.getCode())) {
                                    // 这是一个采集配置属性
                                    Map<String, Object> configInfo = new HashMap<>();
                                    configInfo.put("propertyValue", relatedProperty.getPropertyValue());
                                    configInfo.put("typePropertyId", relatedTypeProperty.getId());
                                    configInfo.put("codeSort", relatedType != null ? (relatedType.getCodeSort() != null ? relatedType.getCodeSort() : 0) : 0);
                                    configurationList.add(configInfo);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (hierarchyCodeInfoList.isEmpty()) {
            return null;
        }

        // 按code_sort升序排序
        hierarchyCodeInfoList.sort(Comparator.comparingInt(info -> (Integer) info.get("codeSort")));

        // 提取排序后的编码
        List<String> codeParts = hierarchyCodeInfoList.stream()
            .map(info -> (String) info.get("code"))
            .collect(Collectors.toList());

        // 选择code_sort最大的采集配置
        String selectedConfiguration = null;
        Long typePropertyId = null;
        if (!configurationList.isEmpty()) {
            configurationList.sort(Comparator.comparingInt((Map<String, Object> info) -> (Integer) info.get("codeSort")).reversed());
            selectedConfiguration = (String) configurationList.get(0).get("propertyValue");
            typePropertyId = (Long) configurationList.get(0).get("typePropertyId");
        }

        // 拼装属性编码前缀
        String codePrefix = String.join("", codeParts);

        // 获取当前层级类型信息用于确定编码长度
        Integer codeLength = 3; // 默认长度3
        if (current != null) {
            HierarchyType currentType = hierarchyTypeMapper.selectById(current.getTypeId());
            codeLength = (currentType != null && currentType.getCodeLength() != null) ? currentType.getCodeLength() : 3;
        }

        // 根据前缀生成完整编码
        String completeCode = generateCompleteCodeWithPrefix(codePrefix, codeLength, hierarchyMapper);

        Map<String, Object> result = new HashMap<>();
        result.put("code", completeCode);
        result.put("configuration", selectedConfiguration);
        result.put("typePropertyId", typePropertyId);
        return result;
    }

    /**
     * 根据前缀生成完整编码
     * 根据属性拼装的前缀去数据库中查找已有编码，按id倒序取最大值然后递增
     * 如果没找到则用指定长度补零
     *
     * @param codePrefix 编码前缀
     * @param codeLength 编码长度（后缀部分）
     * @param hierarchyMapper 层级Mapper
     * @return 生成的完整编码
     */
    public static String generateCompleteCodeWithPrefix(String codePrefix, Integer codeLength, HierarchyMapper hierarchyMapper) {
        if (codePrefix == null || codePrefix.isEmpty()) {
            return null;
        }

        if (codeLength == null || codeLength <= 0) {
            codeLength = 3; // 默认长度3
        }

        // 查找数据库中以该前缀开头的编码，按id倒序取最大的一个
        LambdaQueryWrapper<Hierarchy> queryWrapper = Wrappers.<Hierarchy>lambdaQuery()
            .likeRight(Hierarchy::getCode, codePrefix) // code LIKE '前缀%'
            .isNotNull(Hierarchy::getCode)
            .orderByDesc(Hierarchy::getId)
            .last("LIMIT 1");

        Hierarchy latestHierarchy = hierarchyMapper.selectOne(queryWrapper);

        if (latestHierarchy == null || latestHierarchy.getCode() == null) {
            // 没有找到已有编码，生成第一个编码：前缀 + 001（根据长度补零）
            return codePrefix + String.format("%0" + codeLength + "d", 1);
        }

        String existingCode = latestHierarchy.getCode();

        // 找到了已有编码，提取后缀部分并递增
        if (existingCode.length() <= codePrefix.length() || !existingCode.startsWith(codePrefix)) {
            // 如果现有编码长度不足或不是以前缀开头，重新开始：前缀 + 001
            return codePrefix + String.format("%0" + codeLength + "d", 1);
        }

        // 提取后缀部分
        String suffix = existingCode.substring(codePrefix.length());

        // 根据后缀长度确定实际的编码长度（不依赖传入的codeLength参数）
        int actualCodeLength = suffix.length();

        // 尝试将后缀解析为数字并递增
        try {
            int suffixNumber = Integer.parseInt(suffix);
            int nextNumber = suffixNumber + 1;

            // 使用实际的后缀长度来格式化数字，保持长度一致
            return codePrefix + String.format("%0" + actualCodeLength + "d", nextNumber);

        } catch (NumberFormatException e) {
            // 后缀不是纯数字，使用现有的字母递增逻辑
            String nextAlphaSuffix = generateNextCodeFromExisting(suffix, actualCodeLength);
            if (nextAlphaSuffix == null) {
                // 无法生成更多编码
                return null;
            }
            return codePrefix + nextAlphaSuffix;
        }
    }

    /**
     * 生成下一个编码
     * 根据typeId和parentId查询最近的编码，生成下一个编码
     *
     * @param typeId 层级类型ID
     * @param parentId 父级ID
     * @param codeLength 编码长度
     * @param hierarchyMapper 层级Mapper
     * @return 生成的编码，如果无法生成则返回null
     */
    public static String generateNextCode(Long typeId, Long parentId, Integer codeLength, HierarchyMapper hierarchyMapper) {
        if (codeLength == null || codeLength <= 0) {
            return null;
        }

        // 查询同typeId和parentId的最近编码（按id倒序）
        LambdaQueryWrapper<Hierarchy> queryWrapper = Wrappers.<Hierarchy>lambdaQuery()
            .eq(Hierarchy::getTypeId, typeId)
            .eq(parentId != null, Hierarchy::getParentId, parentId)
            .isNull(parentId == null, Hierarchy::getParentId)
            .isNotNull(Hierarchy::getCode)
            .orderByDesc(Hierarchy::getId)
            .last("LIMIT 1");

        Hierarchy latestHierarchy = hierarchyMapper.selectOne(queryWrapper);

        String nextCode;
        if (latestHierarchy == null || latestHierarchy.getCode() == null) {
            // 没有找到已有编码，生成第一个编码
            nextCode = generateFirstCode(codeLength);
        } else {
            // 基于最新编码生成下一个编码
            nextCode = generateNextCodeFromExisting(latestHierarchy.getCode(), codeLength);
        }

        return nextCode;
    }

    /**
     * 生成第一个编码
     * 根据长度生成初始编码，如：01, 001, 0001等
     */
    public static String generateFirstCode(Integer codeLength) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength - 1; i++) {
            code.append("0");
        }
        code.append("1");
        return code.toString();
    }

    /**
     * 基于现有编码生成下一个编码
     * 编码规则：先用完所有数字组合，再用字母
     * 例如：01->02->...->99->0A->0B->...->0Z->1A->1B->...->9Z->AA->AB->...->ZZ
     */
    public static String generateNextCodeFromExisting(String currentCode, Integer codeLength) {
        if (currentCode == null || currentCode.length() != codeLength) {
            return generateFirstCode(codeLength);
        }

        // 检查当前编码是否全为数字
        if (isAllDigits(currentCode)) {
            // 如果是全数字，尝试数字递增
            String nextNumericCode = incrementNumericCode(currentCode);
            if (nextNumericCode != null) {
                return nextNumericCode;
            }
            // 如果数字已达上限，转换为第一个字母编码
            return getFirstAlphaCode(codeLength);
        } else {
            // 如果已包含字母，按字母规则递增
            return incrementAlphaCode(currentCode, codeLength);
        }
    }

    /**
     * 检查字符串是否全为数字
     */
    public static boolean isAllDigits(String code) {
        for (char c : code.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * 递增纯数字编码
     */
    public static String incrementNumericCode(String currentCode) {
        char[] codeArray = currentCode.toCharArray();
        int codeLength = codeArray.length;

        // 从最后一位开始递增
        for (int i = codeLength - 1; i >= 0; i--) {
            if (codeArray[i] < '9') {
                codeArray[i]++;
                return new String(codeArray);
            } else {
                codeArray[i] = '0';
                // 如果是第一位也需要进位，说明数字已达上限
                if (i == 0) {
                    return null; // 返回null表示数字编码已达上限
                }
            }
        }
        return null;
    }

    /**
     * 获取第一个字母编码
     * 例如：长度2时返回"0A"，长度3时返回"00A"
     */
    public static String getFirstAlphaCode(Integer codeLength) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength - 1; i++) {
            code.append("0");
        }
        code.append("A");
        return code.toString();
    }

    /**
     * 递增字母编码（已包含字母的编码）
     */
    public static String incrementAlphaCode(String currentCode, Integer codeLength) {
        char[] codeArray = currentCode.toCharArray();

        // 从最后一位开始进位
        for (int i = codeLength - 1; i >= 0; i--) {
            char currentChar = codeArray[i];
            char nextChar = getNextAlphaChar(currentChar);

            if (nextChar != '0') {
                // 当前位可以进位，不需要向前进位
                codeArray[i] = nextChar;
                return new String(codeArray);
            } else {
                // 当前位已到最大值，需要向前进位
                if (i == 0) {
                    // 已经到达最大编码，无法再生成
                    return null;
                }
                // 重置当前位为A（字母编码的起始字符）
                codeArray[i] = 'A';
                // 继续向前进位
            }
        }

        return new String(codeArray);
    }

    /**
     * 获取字母编码中字符的下一个字符
     * 0-9 -> 1-9,A
     * A-Z -> B-Z,0(进位)
     */
    public static char getNextAlphaChar(char currentChar) {
        if (currentChar >= '0' && currentChar <= '8') {
            // 数字0-8，直接+1
            return (char) (currentChar + 1);
        } else if (currentChar == '9') {
            // 数字9，下一个是A
            return 'A';
        } else if (currentChar >= 'A' && currentChar <= 'Y') {
            // 字母A-Y，直接+1
            return (char) (currentChar + 1);
        } else if (currentChar == 'Z') {
            // 字母Z，需要进位，返回0表示需要进位
            return '0';
        }
        return '0'; // 默认返回0表示需要进位
    }
}
