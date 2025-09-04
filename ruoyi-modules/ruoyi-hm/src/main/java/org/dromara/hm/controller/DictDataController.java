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
import org.dromara.hm.domain.DictData;
import org.dromara.hm.domain.bo.DictDataBo;
import org.dromara.hm.domain.vo.DictDataVo;
import org.dromara.hm.service.IDictDataService;
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
 * 字典数据Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/dictData")
public class DictDataController extends BaseController {

    private final IDictDataService dictDataService;

    /**
     * 查询字典数据列表
     */
    @SaCheckPermission("hm:dictData:list")
    @GetMapping("/list")
    public TableDataInfo<DictDataVo> list(@Validated(QueryGroup.class) DictDataBo bo, PageQuery pageQuery) {
        return dictDataService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:dictData:list")
    @GetMapping("/page")
    public TableDataInfo<DictDataVo> page(@Validated(QueryGroup.class) DictDataBo bo, PageQuery pageQuery) {
        return dictDataService.customPageList(bo, pageQuery);
    }

    /**
     * 根据字典类型查询字典数据
     */
    @SaCheckPermission("hm:dictData:list")
    @GetMapping("/type/{dictType}")
    public R<List<DictDataVo>> getDictDataByType(@PathVariable String dictType) {
        return R.ok(dictDataService.queryListByDictType(dictType));
    }

    /**
     * 导出字典数据 Excel
     */
    @SaCheckPermission("hm:dictData:export")
    @Log(title = "字典数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated DictDataBo bo, HttpServletResponse response) {
        List<DictDataVo> list = dictDataService.queryList(bo);
        ExcelUtil.exportExcel(list, "字典数据", DictDataVo.class, response);
    }

    /**
     * 获取字典数据详细信息
     *
     * @param dictCode 字典编码
     */
    @SaCheckPermission("hm:dictData:query")
    @GetMapping("/{dictCode}")
    public R<DictDataVo> getInfo(@NotNull(message = "字典编码不能为空") @PathVariable Long dictCode) {
        return R.ok(dictDataService.queryById(dictCode));
    }

    /**
     * 新增字典数据
     */
    @SaCheckPermission("hm:dictData:add")
    @Log(title = "字典数据", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DictDataBo bo) {
        return toAjax(dictDataService.insertByBo(bo));
    }

    /**
     * 修改字典数据
     */
    @SaCheckPermission("hm:dictData:edit")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DictDataBo bo) {
        return toAjax(dictDataService.updateByBo(bo));
    }

    /**
     * 删除字典数据
     *
     * @param dictCodes 字典编码串
     */
    @SaCheckPermission("hm:dictData:remove")
    @Log(title = "字典数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictCodes}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] dictCodes) {
        return toAjax(dictDataService.deleteWithValidByIds(Arrays.asList(dictCodes), true));
    }

    /**
     * 导入字典数据 Excel
     */
    @Log(title = "字典数据", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:dictData:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<DictDataBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), DictDataBo.class, true);
        List<DictData> list = MapstructUtils.convert(excelResult.getList(), DictData.class);
        dictDataService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 获取字典数据下载模板
     */
    @SaCheckPermission("hm:dictData:import")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "字典数据模板", DictDataVo.class, response);
    }

}
