package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.hm.domain.bo.EquipmentFileBo;
import org.dromara.hm.domain.dto.BatchInsertRequest;
import org.dromara.hm.domain.vo.EquipmentFileVo;
import org.dromara.hm.service.IEquipmentFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * 设备文件Controller
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/equipmentFile")
public class EquipmentFileController extends BaseController {

    private final IEquipmentFileService equipmentFileService;

    /**
     * 查询设备文件列表
     */
    @SaCheckPermission("hm:equipmentFile:list")
    @GetMapping("/list")
    public TableDataInfo<EquipmentFileVo> list(@Validated(QueryGroup.class) EquipmentFileBo bo, PageQuery pageQuery) {
        return equipmentFileService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:equipmentFile:list")
    @GetMapping("/page")
    public TableDataInfo<EquipmentFileVo> page(@Validated(QueryGroup.class) EquipmentFileBo bo, PageQuery pageQuery) {
        return equipmentFileService.customPageList(bo, pageQuery);
    }

    /**
     * 获取设备文件详细信息
     *
     * @param id 设备文件ID
     */
    @SaCheckPermission("hm:equipmentFile:query")
    @GetMapping("/{id}")
    public R<EquipmentFileVo> getInfo(@NotNull(message = "设备文件ID不能为空") @PathVariable Long id) {
        return R.ok(equipmentFileService.queryById(id));
    }

    /**
     * 新增设备文件
     */
    @SaCheckPermission("hm:equipmentFile:add")
    @Log(title = "设备文件", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody EquipmentFileBo bo) {
        return toAjax(equipmentFileService.insertByBo(bo));
    }

    /**
     * 修改设备文件
     */
    @SaCheckPermission("hm:equipmentFile:edit")
    @Log(title = "设备文件", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody EquipmentFileBo bo) {
        return toAjax(equipmentFileService.updateByBo(bo));
    }

    /**
     * 删除设备文件
     *
     * @param ids 设备文件ID数组
     */
    @SaCheckPermission("hm:equipmentFile:remove")
    @Log(title = "设备文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "设备文件ID不能为空") @PathVariable Long[] ids) {
        return toAjax(equipmentFileService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 根据设备ID查询文件列表
     */
    @SaCheckPermission("hm:equipmentFile:list")
    @GetMapping("/equipment/{equipmentId}")
    public R<List<EquipmentFileVo>> getByEquipmentId(@NotNull(message = "设备ID不能为空") @PathVariable Long equipmentId) {
        return R.ok(equipmentFileService.selectByEquipmentId(equipmentId));
    }

    /**
     * 根据文件ID查询设备列表
     */
    @SaCheckPermission("hm:equipmentFile:list")
    @GetMapping("/file/{fileId}")
    public R<List<EquipmentFileVo>> getByFileId(@NotNull(message = "文件ID不能为空") @PathVariable Long fileId) {
        return R.ok(equipmentFileService.selectByFileId(fileId));
    }

    /**
     * 根据设备ID和文件类型查询
     */
    @SaCheckPermission("hm:equipmentFile:list")
    @GetMapping("/equipment/{equipmentId}/type/{fileType}")
    public R<List<EquipmentFileVo>> getByEquipmentIdAndFileType(
            @NotNull(message = "设备ID不能为空") @PathVariable Long equipmentId,
            @NotNull(message = "文件类型不能为空") @PathVariable Integer fileType) {
        return R.ok(equipmentFileService.selectByEquipmentIdAndFileType(equipmentId, fileType));
    }


    /**
     * 根据设备ID删除所有关联记录
     */
    @SaCheckPermission("hm:equipmentFile:remove")
    @Log(title = "设备文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/equipment/{equipmentId}")
    public R<Void> deleteByEquipmentId(@NotNull(message = "设备ID不能为空") @PathVariable Long equipmentId) {
        return toAjax(equipmentFileService.deleteByEquipmentId(equipmentId));
    }

    /**
     * 根据文件ID删除所有关联记录
     */
    @SaCheckPermission("hm:equipmentFile:remove")
    @Log(title = "设备文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/file/{fileId}")
    public R<Void> deleteByFileId(@NotNull(message = "文件ID不能为空") @PathVariable Long fileId) {
        return toAjax(equipmentFileService.deleteByFileId(fileId));
    }

    /**
     * 批量添加设备文件关联
     */
    @SaCheckPermission("hm:equipmentFile:add")
    @Log(title = "设备文件", businessType = BusinessType.INSERT)
    @PostMapping("/batch/{equipmentId}")
    public R<Void> batchInsert(
            @NotNull(message = "设备ID不能为空") @PathVariable Long equipmentId,
            @RequestBody BatchInsertRequest request) {
        return toAjax(equipmentFileService.batchInsertByEquipmentId(
            equipmentId, request.getFileIds(), request.getFileType(), request.getAlgorithmType()));
    }

    /**
     * 批量更新设备文件关联
     */
    @SaCheckPermission("hm:equipmentFile:edit")
    @Log(title = "设备文件", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/{equipmentId}")
    public R<Void> batchUpdate(
            @NotNull(message = "设备ID不能为空") @PathVariable Long equipmentId,
            @RequestBody BatchInsertRequest request) {
        return toAjax(equipmentFileService.batchUpdateByEquipmentId(
            equipmentId, request.getFileIds(), request.getFileType(), request.getAlgorithmType()));
    }
}
