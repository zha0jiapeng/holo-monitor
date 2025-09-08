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
import org.dromara.hm.domain.HierarchyData;
import org.dromara.hm.domain.bo.HierarchyDataBo;
import org.dromara.hm.domain.vo.HierarchyDataVo;
import org.dromara.hm.service.IHierarchyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * 层级数据Controller
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/hierarchy/data")
@BrotliCompress
public class HierarchyDataController extends BaseController {

    private final IHierarchyDataService hierarchyDataService;

    /**
     * 查询层级数据列表
     */
    @SaCheckPermission("hm:hierarchyData:list")
    @GetMapping("/list")
    public TableDataInfo<HierarchyDataVo> list(@Validated(QueryGroup.class) HierarchyDataBo bo, PageQuery pageQuery) {
        return hierarchyDataService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出层级数据列表
     */
    @SaCheckPermission("hm:hierarchyData:export")
    @Log(title = "层级数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated HierarchyDataBo bo, HttpServletResponse response) {
        List<HierarchyDataVo> list = hierarchyDataService.queryList(bo);
        ExcelUtil.exportExcel(list, "层级数据", HierarchyDataVo.class, response);
    }

    /**
     * 获取层级数据详细信息
     *
     * @param id 层级数据ID
     */
    @SaCheckPermission("hm:hierarchyData:query")
    @GetMapping("/{id}")
    public R<HierarchyDataVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable("id") Long id) {
        return R.ok(hierarchyDataService.queryById(id));
    }

    /**
     * 新增层级数据
     */
    @SaCheckPermission("hm:hierarchyData:add")
    @Log(title = "层级数据", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = java.util.concurrent.TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody HierarchyDataBo bo) {
        return toAjax(hierarchyDataService.insertByBo(bo));
    }

    /**
     * 修改层级数据
     */
    @SaCheckPermission("hm:hierarchyData:edit")
    @Log(title = "层级数据", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody HierarchyDataBo bo) {
        return toAjax(hierarchyDataService.updateByBo(bo));
    }

    /**
     * 删除层级数据
     *
     * @param ids 层级数据ID串
     */
    @SaCheckPermission("hm:hierarchyData:remove")
    @Log(title = "层级数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                         @PathVariable Long[] ids) {
        return toAjax(hierarchyDataService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "层级数据", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:hierarchyData:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<HierarchyDataBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), HierarchyDataBo.class, true);
        List<HierarchyData> list = MapstructUtils.convert(excelResult.getList(), HierarchyData.class);
        hierarchyDataService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

}
