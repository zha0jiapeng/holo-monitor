package org.dromara.hm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.bo.HierarchyPropertyBo;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;

import java.util.Collection;
import java.util.List;

/**
 * 层级属性Service接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface IHierarchyPropertyService extends IService<HierarchyProperty> {

    /**
     * 查询单个
     *
     * @param id 主键ID
     * @return 层级属性视图对象
     */
    HierarchyPropertyVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<HierarchyPropertyVo> queryPageList(HierarchyPropertyBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<HierarchyPropertyVo> customPageList(HierarchyPropertyBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<HierarchyPropertyVo> queryList(HierarchyPropertyBo bo);

    /**
     * 根据新增业务对象插入层级属性
     *
     * @param bo 层级属性新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(HierarchyPropertyBo bo);

    /**
     * 根据编辑业务对象修改层级属性
     *
     * @param bo 层级属性编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(HierarchyPropertyBo bo);

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
    Boolean saveBatch(List<HierarchyProperty> list);

    /**
     * 根据层级ID查询属性列表
     *
     * @param typeId 层级ID
     * @return 属性列表
     */
    List<HierarchyPropertyVo> getPropertiesByTypeId(Long typeId);

    /**
     * 根据层级ID和属性key查询属性
     *
     * @param typeId 层级ID
     * @param propertyDictId 属性key
     * @return 属性对象
     */
    HierarchyPropertyVo getPropertyByTypeIdAndName(Long typeId, String propertyDictId);

    Boolean updateByDictKey(HierarchyPropertyBo bo);
}
