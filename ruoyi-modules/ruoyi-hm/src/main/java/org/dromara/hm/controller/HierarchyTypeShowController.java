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
import org.dromara.hm.domain.HierarchyTypeShow;
import org.dromara.hm.domain.bo.HierarchyTypeShowBo;
import org.dromara.hm.domain.vo.HierarchyTypeShowVo;
import org.dromara.hm.service.IHierarchyTypeShowService;
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
 * 层级类型展示Controller
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/hierarchy/type/show")
public class HierarchyTypeShowController extends BaseController {

    private final IHierarchyTypeShowService hierarchyTypeShowService;

    /**
     * 查询层级类型展示列表
     */
    @SaCheckPermission("hm:hierarchyTypeShow:list")
    @GetMapping("/list")
    public TableDataInfo<HierarchyTypeShowVo> list(@Validated(QueryGroup.class) HierarchyTypeShowBo bo, PageQuery pageQuery) {
        return hierarchyTypeShowService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:hierarchyTypeShow:list")
    @GetMapping("/page")
    public TableDataInfo<HierarchyTypeShowVo> page(@Validated(QueryGroup.class) HierarchyTypeShowBo bo, PageQuery pageQuery) {
        return hierarchyTypeShowService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "层级类型展示", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:hierarchyTypeShow:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<HierarchyTypeShowBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), HierarchyTypeShowBo.class, true);
        List<HierarchyTypeShow> list = MapstructUtils.convert(excelResult.getList(), HierarchyTypeShow.class);
        hierarchyTypeShowService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出层级类型展示列表
     */
    @SaCheckPermission("hm:hierarchyTypeShow:export")
    @Log(title = "层级类型展示", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated HierarchyTypeShowBo bo, HttpServletResponse response) {
        List<HierarchyTypeShowVo> list = hierarchyTypeShowService.queryList(bo);
        ExcelUtil.exportExcel(list, "层级类型展示", HierarchyTypeShowVo.class, response);
    }

    /**
     * 获取层级类型展示详细信息
     *
     * @param id 层级类型展示ID
     */
    @SaCheckPermission("hm:hierarchyTypeShow:query")
    @GetMapping("/{id}")
    public R<HierarchyTypeShowVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(hierarchyTypeShowService.queryById(id));
    }

    /**
     * 新增层级类型展示
     */
    @SaCheckPermission("hm:hierarchyTypeShow:add")
    @Log(title = "层级类型展示", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody HierarchyTypeShowBo bo) {
        return toAjax(hierarchyTypeShowService.insertByBo(bo));
    }

    /**
     * 修改层级类型展示
     */
    @SaCheckPermission("hm:hierarchyTypeShow:edit")
    @Log(title = "层级类型展示", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody HierarchyTypeShowBo bo) {
        return toAjax(hierarchyTypeShowService.updateByBo(bo));
    }

    /**
     * 删除层级类型展示
     *
     * @param ids 层级类型展示ID串
     */
    @SaCheckPermission("hm:hierarchyTypeShow:remove")
    @Log(title = "层级类型展示", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(hierarchyTypeShowService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
