package org.dromara.hm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.bo.HierarchyBo;
import org.dromara.hm.domain.vo.HierarchyVo;

import java.util.Collection;
import java.util.List;

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
    HierarchyVo queryById(Long id);

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

    List<Long> selectTargetTypeHierarchyList(List<Long> longs, Long targetTypeId);

    List<HierarchyVo> selectByIds(List<Long> matchedIds);
}
