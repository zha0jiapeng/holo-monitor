package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.bo.EquipmentBo;
import org.dromara.hm.domain.vo.EquipmentVo;

import java.util.Collection;
import java.util.List;

/**
 * 设备Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IEquipmentService {

    /**
     * 查询单个
     *
     * @return
     */
    EquipmentVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<EquipmentVo> queryPageList(EquipmentBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<EquipmentVo> customPageList(EquipmentBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<EquipmentVo> queryList(EquipmentBo bo);

    /**
     * 根据新增业务对象插入设备
     *
     * @param bo 设备新增业务对象
     * @return
     */
    Boolean insertByBo(EquipmentBo bo);

    /**
     * 根据编辑业务对象修改设备
     *
     * @param bo 设备编辑业务对象
     * @return
     */
    Boolean updateByBo(EquipmentBo bo);

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
    Boolean saveBatch(List<Equipment> list);

    /**
     * 从JSON数据导入设备
     */
    Boolean importFromJson(String jsonData);

    /**
     * 从JSON数据导入设备（新版本，只处理设备数据）
     */
    Boolean importEquipmentsFromJson(String jsonData);

    /**
     * 根据设备类型查询设备列表
     *
     * @param type 设备类型
     * @return 设备列表
     */
    List<Equipment> getEquipmentsByType(Integer type);

}
