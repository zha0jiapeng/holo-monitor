package org.dromara.hm.service;

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
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IHierarchyService {

    /**
     * 查询单个
     *
     * @return
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
     * @return
     */
    Boolean insertByBo(HierarchyBo bo);

    /**
     * 根据编辑业务对象修改层级
     *
     * @param bo 层级编辑业务对象
     * @return
     */
    Boolean updateByBo(HierarchyBo bo);

    /**
     * 校验并删除数据
     *
     * @param ids     主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<Hierarchy> list);

    /**
     * 根据层级类型查询层级列表
     *
     * @param type 层级类型
     * @return 层级列表
     */
    List<Hierarchy> getHierarchiesByType(Integer type);

    /**
     * 根据父级ID查询子层级列表
     *
     * @param idParent 父级ID
     * @return 子层级列表
     */
    List<HierarchyVo> getChildrenByParentId(Long idParent);

    /**
     * 从JSON数据导入层级
     *
     * @param jsonData JSON数据
     * @return 导入结果
     */
    Boolean importFromJson(String jsonData);

}
