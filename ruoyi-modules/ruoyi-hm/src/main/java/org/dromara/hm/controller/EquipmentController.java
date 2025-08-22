package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.bo.EquipmentBo;
import org.dromara.hm.domain.bo.TestpointBo;
import org.dromara.hm.domain.vo.EquipmentVo;
import org.dromara.hm.service.IEquipmentService;
import lombok.RequiredArgsConstructor;
import org.dromara.hm.validate.BindGroup;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 设备Controller
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/equipment")
public class EquipmentController extends BaseController {

    private final IEquipmentService equipmentService;

    /**
     * 查询设备列表
     */
    @SaCheckPermission("hm:equipment:list")
    @GetMapping("/list")
    public TableDataInfo<EquipmentVo> list(@Validated(QueryGroup.class) EquipmentBo bo, PageQuery pageQuery) {
        return equipmentService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:equipment:list")
    @GetMapping("/page")
    public TableDataInfo<EquipmentVo> page(@Validated(QueryGroup.class) EquipmentBo bo, PageQuery pageQuery) {
        return equipmentService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "设备", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:equipment:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<EquipmentBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), EquipmentBo.class, true);
        List<Equipment> list = MapstructUtils.convert(excelResult.getList(), Equipment.class);
        equipmentService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出设备列表
     */
    @SaCheckPermission("hm:equipment:export")
    @Log(title = "设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated EquipmentBo bo, HttpServletResponse response) {
        List<EquipmentVo> list = equipmentService.queryList(bo);
        ExcelUtil.exportExcel(list, "设备", EquipmentVo.class, response);
    }

    /**
     * 获取设备详细信息
     *
     * @param id 设备ID
     */
    @SaCheckPermission("hm:equipment:query")
    @GetMapping("/{id}")
    public R<EquipmentVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(equipmentService.queryById(id));
    }

    /**
     * 新增设备
     */
    @SaCheckPermission("hm:equipment:add")
    @Log(title = "设备", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody EquipmentBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(equipmentService.insertByBo(bo));
    }

    /**
     * 修改设备
     */
    @SaCheckPermission("hm:equipment:edit")
    @Log(title = "设备", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody EquipmentBo bo) {
        return toAjax(equipmentService.updateByBo(bo));
    }

    /**
     * 删除设备
     *
     * @param ids 设备ID串
     */
    @SaCheckPermission("hm:equipment:remove")
    @Log(title = "设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(equipmentService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 批量导入设备数据（从JSON）
     */
    @SaCheckPermission("hm:equipment:import")
    @Log(title = "设备", businessType = BusinessType.IMPORT)
    @PostMapping("/importFromJson")
    public R<Void> importFromJson(@RequestBody String jsonData) {
        return toAjax(equipmentService.importFromJson(jsonData));
    }

    /**
     * 绑定测点坐标
     */
    @SaCheckPermission("hm:equipment:edit")
    @Log(title = "绑定测点坐标", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/bind")
    public R<Void> bind(@Validated(BindGroup.class) @RequestBody List<EquipmentBo> bo) {
        return toAjax(equipmentService.updateBatchByBo(bo));
    }

    /**
     * 解绑测点坐标
     */
    @SaCheckPermission("hm:equipment:edit")
    @Log(title = "解绑测点坐标", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/unbind")
    public R<Void> unbind(@RequestBody List<Long> ids) {
        return toAjax(equipmentService.unbind(ids));
    }
}
