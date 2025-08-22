package org.dromara.hm.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.EquipmentFile;
import org.dromara.hm.domain.bo.EquipmentFileBo;
import org.dromara.hm.domain.vo.EquipmentFileVo;

import java.util.Collection;
import java.util.List;

/**
 * 设备文件Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IEquipmentFileService {

    /**
     * 查询单个
     *
     * @return
     */
    EquipmentFileVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<EquipmentFileVo> queryPageList(EquipmentFileBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<EquipmentFileVo> customPageList(EquipmentFileBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<EquipmentFileVo> queryList(EquipmentFileBo bo);

    /**
     * 根据新增业务对象插入设备文件
     *
     * @param bo 设备文件新增业务对象
     * @return
     */
    Boolean insertByBo(EquipmentFileBo bo);

    /**
     * 根据编辑业务对象修改设备文件
     *
     * @param bo 设备文件编辑业务对象
     * @return
     */
    Boolean updateByBo(EquipmentFileBo bo);

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
    Boolean saveBatch(List<EquipmentFile> list);

    /**
     * 根据设备ID查询文件列表
     */
    List<EquipmentFileVo> selectByEquipmentId(Long equipmentId);

    /**
     * 根据文件ID查询设备列表
     */
    List<EquipmentFileVo> selectByFileId(Long fileId);

    /**
     * 根据设备ID和文件类型查询
     */
    List<EquipmentFileVo> selectByEquipmentIdAndFileType(Long equipmentId, Integer fileType);

    /**
     * 根据设备ID删除所有关联记录
     */
    Boolean deleteByEquipmentId(Long equipmentId);

    /**
     * 根据文件ID删除所有关联记录
     */
    Boolean deleteByFileId(Long fileId);

    /**
     * 批量添加设备文件关联
     */
    Boolean batchInsertByEquipmentId(Long equipmentId, List<Long> fileIds, Integer fileType, Integer algorithmType);

    /**
     * 批量更新设备文件关联
     */
    Boolean batchUpdateByEquipmentId(Long equipmentId, List<Long> fileIds, Integer fileType, Integer algorithmType);

}
