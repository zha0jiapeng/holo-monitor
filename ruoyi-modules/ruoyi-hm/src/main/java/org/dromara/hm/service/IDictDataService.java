package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.DictData;
import org.dromara.hm.domain.bo.DictDataBo;
import org.dromara.hm.domain.vo.DictDataVo;

import java.util.Collection;
import java.util.List;

/**
 * 字典数据Service接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface IDictDataService {

    /**
     * 查询单个
     *
     * @param dictCode 字典编码
     * @return 字典数据视图对象
     */
    DictDataVo queryById(Long dictCode);

    /**
     * 查询列表
     */
    TableDataInfo<DictDataVo> queryPageList(DictDataBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<DictDataVo> customPageList(DictDataBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<DictDataVo> queryList(DictDataBo bo);

    /**
     * 根据新增业务对象插入字典数据
     *
     * @param bo 字典数据新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(DictDataBo bo);

    /**
     * 根据编辑业务对象修改字典数据
     *
     * @param bo 字典数据编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(DictDataBo bo);

    /**
     * 校验并删除数据
     *
     * @param dictCodes 主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> dictCodes, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<DictData> list);

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<DictDataVo> queryListByDictType(String dictType);

}
