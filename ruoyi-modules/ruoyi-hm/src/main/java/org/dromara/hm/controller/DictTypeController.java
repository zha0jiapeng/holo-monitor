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
import org.dromara.hm.domain.DictType;
import org.dromara.hm.domain.bo.DictTypeBo;
import org.dromara.hm.domain.vo.DictTypeVo;
import org.dromara.hm.service.IDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.*;

/**
 * 字典类型Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/dictType")
public class DictTypeController extends BaseController {

    private final IDictTypeService dictTypeService;

    /**
     * 查询字典类型列表
     */
    @SaCheckPermission("hm:dictType:list")
    @GetMapping("/list")
    public TableDataInfo<DictTypeVo> list(@Validated(QueryGroup.class) DictTypeBo bo, PageQuery pageQuery) {
        return dictTypeService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:dictType:list")
    @GetMapping("/page")
    public TableDataInfo<DictTypeVo> page(@Validated(QueryGroup.class) DictTypeBo bo, PageQuery pageQuery) {
        return dictTypeService.customPageList(bo, pageQuery);
    }

    /**
     * 导出字典类型 Excel
     */
    @SaCheckPermission("hm:dictType:export")
    @Log(title = "字典类型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated DictTypeBo bo, HttpServletResponse response) {
        List<DictTypeVo> list = dictTypeService.queryList(bo);
        ExcelUtil.exportExcel(list, "字典类型", DictTypeVo.class, response);
    }

    /**
     * 获取字典类型详细信息
     *
     * @param dictId 字典主键
     */
    @SaCheckPermission("hm:dictType:query")
    @GetMapping("/{dictId}")
    public R<DictTypeVo> getInfo(@NotNull(message = "字典主键不能为空") @PathVariable Long dictId) {
        return R.ok(dictTypeService.queryById(dictId));
    }

    /**
     * 新增字典类型
     */
    @SaCheckPermission("hm:dictType:add")
    @Log(title = "字典类型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DictTypeBo bo) {
        return toAjax(dictTypeService.insertByBo(bo));
    }

    /**
     * 修改字典类型
     */
    @SaCheckPermission("hm:dictType:edit")
    @Log(title = "字典类型", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DictTypeBo bo) {
        return toAjax(dictTypeService.updateByBo(bo));
    }

    /**
     * 删除字典类型
     *
     * @param dictIds 字典主键串
     */
    @SaCheckPermission("hm:dictType:remove")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] dictIds) {
        return toAjax(dictTypeService.deleteWithValidByIds(Arrays.asList(dictIds), true));
    }

    /**
     * 导入字典类型 Excel
     */
    @Log(title = "字典类型", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:dictType:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<DictTypeBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), DictTypeBo.class, true);
        List<DictType> list = MapstructUtils.convert(excelResult.getList(), DictType.class);
        dictTypeService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 获取字典类型下载模板
     */
    @SaCheckPermission("hm:dictType:import")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "字典类型模板", DictTypeVo.class, response);
    }

}
