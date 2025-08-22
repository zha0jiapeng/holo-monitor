package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.EquipmentFile;
import org.dromara.hm.domain.vo.EquipmentFileVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 设备文件Mapper接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface EquipmentFileMapper extends BaseMapperPlus<EquipmentFile, EquipmentFileVo> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    Page<EquipmentFileVo> customPageList(@Param("page") Page<EquipmentFile> page, @Param("ew") Wrapper<EquipmentFile> wrapper);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default <P extends IPage<EquipmentFileVo>> P selectVoPage(IPage<EquipmentFile> page, Wrapper<EquipmentFile> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default List<EquipmentFileVo> selectVoList(Wrapper<EquipmentFile> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission(value = {
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    }, joinStr = "AND")
    List<EquipmentFile> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    int updateById(@Param(Constants.ENTITY) EquipmentFile entity);

    /**
     * 根据设备ID查询文件列表
     */
    List<EquipmentFileVo> selectByEquipmentId(@Param("equipmentId") Long equipmentId);

    /**
     * 根据文件ID查询设备列表
     */
    List<EquipmentFileVo> selectByFileId(@Param("fileId") Long fileId);

    /**
     * 根据设备ID和文件类型查询
     */
    List<EquipmentFileVo> selectByEquipmentIdAndFileType(@Param("equipmentId") Long equipmentId, @Param("fileType") Integer fileType);

    /**
     * 根据设备ID删除所有关联记录
     */
    int deleteByEquipmentId(@Param("equipmentId") Long equipmentId);

    /**
     * 根据文件ID删除所有关联记录
     */
    int deleteByFileId(@Param("fileId") Long fileId);

}
