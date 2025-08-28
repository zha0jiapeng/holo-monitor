package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.HierarchyTypeShow;
import org.dromara.hm.domain.bo.HierarchyTypeShowBo;
import org.dromara.hm.domain.vo.HierarchyTypeShowVo;

import java.util.Collection;
import java.util.List;

/**
 * 层级类型展示Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IHierarchyTypeShowService {

    /**
     * 查询单个
     *
     * @param id 主键ID
     * @return 层级类型展示视图对象
     */
    HierarchyTypeShowVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<HierarchyTypeShowVo> queryPageList(HierarchyTypeShowBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<HierarchyTypeShowVo> customPageList(HierarchyTypeShowBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<HierarchyTypeShowVo> queryList(HierarchyTypeShowBo bo);

    /**
     * 根据新增业务对象插入层级类型展示
     *
     * @param bo 层级类型展示新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(HierarchyTypeShowBo bo);

    /**
     * 根据编辑业务对象修改层级类型展示
     *
     * @param bo 层级类型展示编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(HierarchyTypeShowBo bo);

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
    Boolean saveBatch(List<HierarchyTypeShow> list);

}
