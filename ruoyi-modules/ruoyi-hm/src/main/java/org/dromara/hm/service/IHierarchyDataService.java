package org.dromara.hm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.HierarchyData;
import org.dromara.hm.domain.bo.HierarchyDataBo;
import org.dromara.hm.domain.vo.HierarchyDataVo;

import java.util.Collection;
import java.util.List;

/**
 * 层级数据Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IHierarchyDataService  extends IService<HierarchyData> {

    /**
     * 查询单个
     *
     * @param id 主键ID
     * @return 层级数据视图对象
     */
    HierarchyDataVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<HierarchyDataVo> queryPageList(HierarchyDataBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<HierarchyDataVo> queryList(HierarchyDataBo bo);

    /**
     * 根据新增业务对象插入层级数据
     *
     * @param bo 层级数据新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(HierarchyDataBo bo);

    /**
     * 根据编辑业务对象修改层级数据
     *
     * @param bo 层级数据编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(HierarchyDataBo bo);

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
    Boolean saveBatch(List<HierarchyData> list);

}
