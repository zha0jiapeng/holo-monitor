package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.HierarchyType;
import org.dromara.hm.domain.bo.HierarchyTypeBo;
import org.dromara.hm.domain.vo.HierarchyTypeVo;

import java.util.Collection;
import java.util.List;

/**
 * 层级类型Service接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface IHierarchyTypeService {

    /**
     * 查询单个
     *
     * @param id 主键ID
     * @return 层级类型视图对象
     */
    HierarchyTypeVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<HierarchyTypeVo> queryPageList(HierarchyTypeBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<HierarchyTypeVo> customPageList(HierarchyTypeBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<HierarchyTypeVo> queryList(HierarchyTypeBo bo);

    /**
     * 根据新增业务对象插入层级类型
     *
     * @param bo 层级类型新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(HierarchyTypeBo bo);

    /**
     * 根据编辑业务对象修改层级类型
     *
     * @param bo 层级类型编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(HierarchyTypeBo bo);

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
    Boolean saveBatch(List<HierarchyType> list);



}
