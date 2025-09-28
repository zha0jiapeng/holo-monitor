package org.dromara.hm.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.bo.HierarchyBo;
import org.dromara.hm.domain.template.HierarchyExcelTemplate;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.domain.vo.HierarchyTreeVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 层级Service接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface IHierarchyService extends IService<Hierarchy> {

    /**
     * 查询单个
     *
     * @param id 主键ID
     * @return 层级视图对象
     */
    HierarchyVo queryById(Long id, boolean needProperty);

    HierarchyVo queryById(Long id, boolean needProperty, boolean needHiddenProperty);

    List<HierarchyVo> queryByIds(List<Long> ids,boolean needProperty);

    List<HierarchyVo> queryByIds(List<Long> ids, boolean needProperty, boolean needHiddenProperty);

    /**
     * 查询列表
     */
    TableDataInfo<HierarchyVo> queryPageList(HierarchyBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<HierarchyVo> customPageList(HierarchyBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<HierarchyVo> queryList(HierarchyBo bo);

    /**
     * 根据新增业务对象插入层级
     *
     * @param bo 层级新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(HierarchyBo bo);

    /**
     * 根据编辑业务对象修改层级
     *
     * @param bo 层级编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(HierarchyBo bo);

    /**
     * 校验并删除数据
     *
     * @param ids     主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<Hierarchy> list);


    /**
     * 根据父级ID查询子层级列表
     *
     * @param parentId 父级ID
     * @return 子层级列表
     */
    List<HierarchyVo> getChildrenByParentId(Long parentId);

    /**
     * 查询指定层级下所有子孙层级中指定类型的层级列表
     *
     * @param hierarchyId 层级ID
     * @param targetTypeId 目标层级类型ID
     * @return 指定类型的层级列表
     */
    List<HierarchyVo> getDescendantsByType(Long hierarchyId, Long targetTypeId);

    /**
     * 获取最底层且具有采集配置(1005)属性的层级列表
     *
     * @return 最底层且有采集配置属性的层级列表
     */
    List<HierarchyVo> getBottomLevelWithConfiguration();

    List<Long> selectChildHierarchyIds(Long hierarchyId);

    List<Map<String,String>> selectTargetTypeHierarchyList(List<Long> longs, Long targetTypeId);

    List<HierarchyVo> selectByIds(List<Long> matchedIds,boolean needProperty);

    List<HierarchyVo> selectByIds(List<Long> matchedIds, boolean needProperty, boolean needHiddenProperty);

    List<HierarchyVo> selectByIds(List<Long> matchedIds,List<String> diceNames);

    List<HierarchyVo> getSensorListByDeviceId(Long hierarchyId,boolean showAllFlag);

    /**
     * 获取所有具有采集配置(CONFIGURATION, 1005)属性的传感器列表
     *
     * @return 具有采集配置属性的传感器列表
     */
    List<HierarchyVo> getAllSensorsWithConfiguration();

    Map<String, List<HierarchyVo>> sensorList(Long parentId, Long hierarchyId);

    JSONObject getLocationByHierarchyId(Long hierarchyId);

    JSONObject downloadTemplate(Long typeId);

    Long getIdByNameAndType(String name, Long typeId);

    void upload(List<HierarchyExcelTemplate> hierarchyExcelTemplates, String properties, Long typeId);

    /**
     * 根据parentId递归获取unit类型的层级树结构，包含传感器绑定信息
     *
     * @param parentId 父级ID
     * @param hierarchyId 设备ID（用于查询传感器绑定情况）
     * @return unit类型层级的树结构（包含传感器分组）
     */
    List<HierarchyTreeVo> getUnitHierarchyTree(Long parentId, Long hierarchyId);

    /**
     * 根据传感器层级code查找其所属的变电站（typeId=7）详情和属性列表
     *
     * @param sensorCode 传感器层级编码
     * @return 变电站详情和属性列表，如果未找到返回null
     */
    HierarchyVo getSubstationBySensorCode(String sensorCode);
}
