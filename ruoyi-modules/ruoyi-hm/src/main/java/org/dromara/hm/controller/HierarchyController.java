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
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.bo.HierarchyBo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.service.IHierarchyService;
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
 * 层级Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/hierarchy")
@BrotliCompress
public class HierarchyController extends BaseController {

    private final IHierarchyService hierarchyService;

    /**
     * 查询层级列表
     */
    @SaCheckPermission("hm:hierarchy:list")
    @GetMapping("/list")
    public TableDataInfo<HierarchyVo> list(@Validated(QueryGroup.class) HierarchyBo bo, PageQuery pageQuery) {
        return hierarchyService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:hierarchy:list")
    @GetMapping("/page")
    public TableDataInfo<HierarchyVo> page(@Validated(QueryGroup.class) HierarchyBo bo, PageQuery pageQuery) {
        return hierarchyService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "层级", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:hierarchy:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<HierarchyBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), HierarchyBo.class, true);
        List<Hierarchy> list = MapstructUtils.convert(excelResult.getList(), Hierarchy.class);
        hierarchyService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出层级列表
     */
    @SaCheckPermission("hm:hierarchy:export")
    @Log(title = "层级", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated HierarchyBo bo, HttpServletResponse response) {
        List<HierarchyVo> list = hierarchyService.queryList(bo);
        ExcelUtil.exportExcel(list, "层级", HierarchyVo.class, response);
    }

    /**
     * 获取层级详细信息
     *
     * @param id 层级ID
     */
    @SaCheckPermission("hm:hierarchy:query")
    @GetMapping("/{id}")
    public R<HierarchyVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(hierarchyService.queryById(id));
    }

    /**
     * 新增层级
     */
    @SaCheckPermission("hm:hierarchy:add")
    @Log(title = "层级", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody HierarchyBo bo) {
        return toAjax(hierarchyService.insertByBo(bo));
    }

    /**
     * 修改层级
     */
    @SaCheckPermission("hm:hierarchy:edit")
    @Log(title = "层级", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody HierarchyBo bo) {
        return toAjax(hierarchyService.updateByBo(bo));
    }

    /**
     * 删除层级
     *
     * @param ids 层级ID串
     */
    @SaCheckPermission("hm:hierarchy:remove")
    @Log(title = "层级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(hierarchyService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
    /**
     * 根据父级ID查询子层级列表
     *
     * @param parentId 父级ID
     */
    @SaCheckPermission("hm:hierarchy:list")
    @GetMapping("/children/{parentId}")
    public R<List<HierarchyVo>> getChildrenByParentId(@PathVariable("parentId") Long parentId) {
        return R.ok(hierarchyService.getChildrenByParentId(parentId));
    }

    /**
     * 查询指定层级下所有子孙层级中指定类型的层级列表
     *
     * @param hierarchyId 层级ID
     * @param targetTypeId 目标层级类型ID
     */
    @SaCheckPermission("hm:hierarchy:list")
    @GetMapping("/descendants/{hierarchyId}/type/{targetTypeId}")
    public R<List<HierarchyVo>> getDescendantsByType(
            @NotNull(message = "层级ID不能为空") @PathVariable("hierarchyId") Long hierarchyId,
            @NotNull(message = "目标类型ID不能为空") @PathVariable("targetTypeId") Long targetTypeId) {
        return R.ok(hierarchyService.getDescendantsByType(hierarchyId, targetTypeId));
    }
}
