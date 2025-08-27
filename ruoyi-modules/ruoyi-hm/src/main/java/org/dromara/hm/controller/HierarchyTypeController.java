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
import org.dromara.hm.domain.HierarchyType;
import org.dromara.hm.domain.bo.HierarchyTypeBo;
import org.dromara.hm.domain.vo.HierarchyTypeVo;
import org.dromara.hm.service.IHierarchyTypeService;
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
 * 层级类型Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/hierarchy/type")
public class HierarchyTypeController extends BaseController {

    private final IHierarchyTypeService hierarchyTypeService;

    /**
     * 查询层级类型列表
     */
    @SaCheckPermission("hm:hierarchyType:list")
    @GetMapping("/list")
    public TableDataInfo<HierarchyTypeVo> list(@Validated(QueryGroup.class) HierarchyTypeBo bo, PageQuery pageQuery) {
        return hierarchyTypeService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:hierarchyType:list")
    @GetMapping("/page")
    public TableDataInfo<HierarchyTypeVo> page(@Validated(QueryGroup.class) HierarchyTypeBo bo, PageQuery pageQuery) {
        return hierarchyTypeService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "层级类型", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:hierarchyType:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<HierarchyTypeBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), HierarchyTypeBo.class, true);
        List<HierarchyType> list = MapstructUtils.convert(excelResult.getList(), HierarchyType.class);
        hierarchyTypeService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出层级类型列表
     */
    @SaCheckPermission("hm:hierarchyType:export")
    @Log(title = "层级类型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated HierarchyTypeBo bo, HttpServletResponse response) {
        List<HierarchyTypeVo> list = hierarchyTypeService.queryList(bo);
        ExcelUtil.exportExcel(list, "层级类型", HierarchyTypeVo.class, response);
    }

    /**
     * 获取层级类型详细信息
     *
     * @param id 层级类型ID
     */
    @SaCheckPermission("hm:hierarchyType:query")
    @GetMapping("/{id}")
    public R<HierarchyTypeVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(hierarchyTypeService.queryById(id));
    }

    /**
     * 新增层级类型
     */
    @SaCheckPermission("hm:hierarchyType:add")
    @Log(title = "层级类型", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody HierarchyTypeBo bo) {
        return toAjax(hierarchyTypeService.insertByBo(bo));
    }

    /**
     * 修改层级类型
     */
    @SaCheckPermission("hm:hierarchyType:edit")
    @Log(title = "层级类型", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody HierarchyTypeBo bo) {
        return toAjax(hierarchyTypeService.updateByBo(bo));
    }

    /**
     * 删除层级类型
     *
     * @param ids 层级类型ID串
     */
    @SaCheckPermission("hm:hierarchyType:remove")
    @Log(title = "层级类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(hierarchyTypeService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
