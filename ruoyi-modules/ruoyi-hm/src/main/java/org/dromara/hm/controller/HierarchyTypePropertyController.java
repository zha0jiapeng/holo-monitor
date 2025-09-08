package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.web.annotation.BrotliCompress;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.bo.HierarchyTypePropertyBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.service.IHierarchyTypePropertyService;
import lombok.RequiredArgsConstructor;
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
 * 层级类型属性Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/hierarchy/type/property")
@BrotliCompress
public class HierarchyTypePropertyController extends BaseController {

    private final IHierarchyTypePropertyService hierarchyTypePropertyService;

    /**
     * 查询层级类型属性列表
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:list")
    @GetMapping("/list")
    public TableDataInfo<HierarchyTypePropertyVo> list(@Validated(QueryGroup.class) HierarchyTypePropertyBo bo, PageQuery pageQuery) {
        return hierarchyTypePropertyService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:list")
    @GetMapping("/page")
    public TableDataInfo<HierarchyTypePropertyVo> page(@Validated(QueryGroup.class) HierarchyTypePropertyBo bo, PageQuery pageQuery) {
        return hierarchyTypePropertyService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "层级类型属性", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:hierarchyTypeProperty:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<HierarchyTypePropertyBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), HierarchyTypePropertyBo.class, true);
        List<HierarchyTypeProperty> list = MapstructUtils.convert(excelResult.getList(), HierarchyTypeProperty.class);
        hierarchyTypePropertyService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出层级类型属性列表
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:export")
    @Log(title = "层级类型属性", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated HierarchyTypePropertyBo bo, HttpServletResponse response) {
        List<HierarchyTypePropertyVo> list = hierarchyTypePropertyService.queryList(bo);
        ExcelUtil.exportExcel(list, "层级类型属性", HierarchyTypePropertyVo.class, response);
    }

    /**
     * 获取层级类型属性详细信息
     *
     * @param id 层级类型属性ID
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:query")
    @GetMapping("/{id}")
    public R<HierarchyTypePropertyVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(hierarchyTypePropertyService.queryById(id));
    }

    /**
     * 新增层级类型属性
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:add")
    @Log(title = "层级类型属性", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody HierarchyTypePropertyBo bo) {
        return toAjax(hierarchyTypePropertyService.insertByBo(bo));
    }

    /**
     * 新增层级类型属性 批量
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:add")
    @Log(title = "层级类型属性", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping("/batch")
    public R<Void> addBatch(@Validated(AddGroup.class) @RequestBody List<HierarchyTypeProperty> bo) {
        return toAjax(hierarchyTypePropertyService.saveBatch(bo));
    }

    /**
     * 修改层级类型属性
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:edit")
    @Log(title = "层级类型属性", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody HierarchyTypePropertyBo bo) {
        return toAjax(hierarchyTypePropertyService.updateByBo(bo));
    }

    /**
     * 删除层级类型属性
     *
     * @param ids 层级类型属性ID串
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:remove")
    @Log(title = "层级类型属性", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(hierarchyTypePropertyService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 根据类型ID获取属性列表
     */
    @SaCheckPermission("hm:hierarchyTypeProperty:list")
    @GetMapping("/type/{typeId}")
    public R<List<HierarchyTypePropertyVo>> getPropertiesByTypeId(@NotNull(message = "类型ID不能为空")
                                                                 @PathVariable("typeId") Long typeId) {
        return R.ok(hierarchyTypePropertyService.getPropertiesByTypeId(typeId));
    }

}
