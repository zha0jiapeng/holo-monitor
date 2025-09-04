package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.DictType;
import org.dromara.hm.domain.bo.DictTypeBo;
import org.dromara.hm.domain.vo.DictTypeVo;

import java.util.Collection;
import java.util.List;

/**
 * 字典类型Service接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface IDictTypeService {

    /**
     * 查询单个
     *
     * @param dictId 字典主键
     * @return 字典类型视图对象
     */
    DictTypeVo queryById(Long dictId);

    /**
     * 查询列表
     */
    TableDataInfo<DictTypeVo> queryPageList(DictTypeBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<DictTypeVo> customPageList(DictTypeBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<DictTypeVo> queryList(DictTypeBo bo);

    /**
     * 根据新增业务对象插入字典类型
     *
     * @param bo 字典类型新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(DictTypeBo bo);

    /**
     * 根据编辑业务对象修改字典类型
     *
     * @param bo 字典类型编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(DictTypeBo bo);

    /**
     * 校验并删除数据
     *
     * @param dictIds 主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> dictIds, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<DictType> list);

}
