package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.bo.HierarchyTypePropertyBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;

import java.util.Collection;
import java.util.List;

/**
 * 层级类型属性Service接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface IHierarchyTypePropertyService {

    /**
     * 查询单个
     *
     * @param id 主键ID
     * @return 层级类型属性视图对象
     */
    HierarchyTypePropertyVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<HierarchyTypePropertyVo> queryPageList(HierarchyTypePropertyBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<HierarchyTypePropertyVo> customPageList(HierarchyTypePropertyBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<HierarchyTypePropertyVo> queryList(HierarchyTypePropertyBo bo);

    /**
     * 根据新增业务对象插入层级类型属性
     *
     * @param bo 层级类型属性新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(HierarchyTypePropertyBo bo);

    /**
     * 根据编辑业务对象修改层级类型属性
     *
     * @param bo 层级类型属性编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(HierarchyTypePropertyBo bo);

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
    Boolean saveBatch(List<HierarchyTypeProperty> list);

    /**
     * 根据类型ID查询属性列表
     *
     * @param typeId 类型ID
     * @return 属性列表
     */
    List<HierarchyTypePropertyVo> getPropertiesByTypeId(Long typeId);

}
