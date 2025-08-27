package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.bo.HierarchyTypePropertyDictBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;

import java.util.Collection;
import java.util.List;

/**
 * 层级类型属性字典Service接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface IHierarchyTypePropertyDictService {

    /**
     * 查询单个
     *
     * @param id 主键ID
     * @return 层级类型属性字典视图对象
     */
    HierarchyTypePropertyDictVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<HierarchyTypePropertyDictVo> queryPageList(HierarchyTypePropertyDictBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<HierarchyTypePropertyDictVo> customPageList(HierarchyTypePropertyDictBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<HierarchyTypePropertyDictVo> queryList(HierarchyTypePropertyDictBo bo);

    /**
     * 根据新增业务对象插入层级类型属性字典
     *
     * @param bo 层级类型属性字典新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(HierarchyTypePropertyDictBo bo);

    /**
     * 根据编辑业务对象修改层级类型属性字典
     *
     * @param bo 层级类型属性字典编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(HierarchyTypePropertyDictBo bo);

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
    Boolean saveBatch(List<HierarchyTypePropertyDict> list);

}
