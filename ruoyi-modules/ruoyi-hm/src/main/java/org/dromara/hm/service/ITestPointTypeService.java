package org.dromara.hm.service;

import org.dromara.hm.domain.vo.TestPointTypeVo;

import java.util.List;
import java.util.Map;

/**
 * 测点类型服务接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface ITestPointTypeService {

    /**
     * 获取所有测点类型列表
     *
     * @return 测点类型VO列表
     */
    List<TestPointTypeVo> getAllTestPointTypes();

    /**
     * 获取测点类型统计信息
     *
     * @return Map<类型名称, 测点数量>
     */
    Map<String, Long> getTestPointTypeStatistics();

    /**
     * 根据设备ID获取测点类型分布
     *
     * @param equipmentId 设备ID
     * @return Map<类型名称, 测点数量>
     */
    Map<String, Long> getTestPointTypeDistributionByEquipment(Long equipmentId);

    /**
     * 获取指定类型的测点数量
     *
     * @param typeCode 类型编码
     * @return 测点数量
     */
    Long getTestPointCountByType(Integer typeCode);

    /**
     * 批量更新测点类型（基于mt值）
     *
     * @return 更新的测点数量
     */
    Long batchUpdateTestPointTypeByMt();

    /**
     * 验证并修复测点类型数据
     *
     * @return 修复的测点数量
     */
    Long validateAndFixTestPointTypes();
}
