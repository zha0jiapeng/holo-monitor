package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MapstructUtils;
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
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.bo.HierarchyPropertyBo;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.service.IHierarchyPropertyService;
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
 * 层级属性Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/hierarchy/property")
public class HierarchyPropertyController extends BaseController {

    private final IHierarchyPropertyService hierarchyPropertyService;

    /**
     * 查询层级属性列表
     */
    @SaCheckPermission("hm:hierarchyProperty:list")
    @GetMapping("/list")
    public TableDataInfo<HierarchyPropertyVo> list(@Validated(QueryGroup.class) HierarchyPropertyBo bo, PageQuery pageQuery) {
        return hierarchyPropertyService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:hierarchyProperty:list")
    @GetMapping("/page")
    public TableDataInfo<HierarchyPropertyVo> page(@Validated(QueryGroup.class) HierarchyPropertyBo bo, PageQuery pageQuery) {
        return hierarchyPropertyService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "层级属性", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:hierarchyProperty:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<HierarchyPropertyBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), HierarchyPropertyBo.class, true);
        List<HierarchyProperty> list = MapstructUtils.convert(excelResult.getList(), HierarchyProperty.class);
        hierarchyPropertyService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出层级属性列表
     */
    @SaCheckPermission("hm:hierarchyProperty:export")
    @Log(title = "层级属性", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated HierarchyPropertyBo bo, HttpServletResponse response) {
        List<HierarchyPropertyVo> list = hierarchyPropertyService.queryList(bo);
        ExcelUtil.exportExcel(list, "层级属性", HierarchyPropertyVo.class, response);
    }

    /**
     * 获取层级属性详细信息
     *
     * @param id 层级属性ID
     */
    @SaCheckPermission("hm:hierarchyProperty:query")
    @GetMapping("/{id}")
    public R<HierarchyPropertyVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(hierarchyPropertyService.queryById(id));
    }

    /**
     * 新增层级属性
     */
    @SaCheckPermission("hm:hierarchyProperty:add")
    @Log(title = "层级属性", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody HierarchyPropertyBo bo) {
        return toAjax(hierarchyPropertyService.insertByBo(bo));
    }

    /**
     * 修改层级属性
     */
    @SaCheckPermission("hm:hierarchyProperty:edit")
    @Log(title = "层级属性", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody HierarchyPropertyBo bo) {
        return toAjax(hierarchyPropertyService.updateByBo(bo));
    }

    /**
     * 删除层级属性
     *
     * @param ids 层级属性ID串
     */
    @SaCheckPermission("hm:hierarchyProperty:remove")
    @Log(title = "层级属性", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(hierarchyPropertyService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 根据层级ID查询属性列表
     *
     * @param typeId 层级l类型ID
     */
    @SaCheckPermission("hm:hierarchyProperty:list")
    @GetMapping("/byType/{typeId}")
    public R<List<HierarchyPropertyVo>> getPropertiesByHierarchyId(@PathVariable("typeId") Long typeId) {
        return R.ok(hierarchyPropertyService.getPropertiesByTypeId(typeId));
    }

    /**
     * 根据层级ID和属性key查询属性
     *
     * @param typeId 层级ID
     * @param propertyDictId 属性key
     */
    @SaCheckPermission("hm:hierarchyProperty:query")
    @GetMapping("/byType/{typeId}/property/{propertyDictId}")
    public R<HierarchyPropertyVo> getPropertyByHierarchyIdAndName(@PathVariable("typeId") Long typeId,
                                                                  @PathVariable("propertyDictId") String propertyDictId) {
        return R.ok(hierarchyPropertyService.getPropertyByTypeIdAndName(typeId, propertyDictId));
    }

}
