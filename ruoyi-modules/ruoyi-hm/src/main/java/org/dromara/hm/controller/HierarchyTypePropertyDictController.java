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
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.bo.HierarchyTypePropertyDictBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.service.IHierarchyTypePropertyDictService;
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
 * 层级类型属性字典Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/hierarchy/type/property/dict")
@BrotliCompress
public class HierarchyTypePropertyDictController extends BaseController {

    private final IHierarchyTypePropertyDictService hierarchyTypePropertyDictService;

    /**
     * 查询层级类型属性字典列表
     */
    @SaCheckPermission("hm:hierarchyTypePropertyDict:list")
    @GetMapping("/list")
    public TableDataInfo<HierarchyTypePropertyDictVo> list(@Validated(QueryGroup.class) HierarchyTypePropertyDictBo bo, PageQuery pageQuery) {
        return hierarchyTypePropertyDictService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:hierarchyTypePropertyDict:list")
    @GetMapping("/page")
    public TableDataInfo<HierarchyTypePropertyDictVo> page(@Validated(QueryGroup.class) HierarchyTypePropertyDictBo bo, PageQuery pageQuery) {
        return hierarchyTypePropertyDictService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "层级类型属性字典", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:hierarchyTypePropertyDict:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<HierarchyTypePropertyDictBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), HierarchyTypePropertyDictBo.class, true);
        List<HierarchyTypePropertyDict> list = MapstructUtils.convert(excelResult.getList(), HierarchyTypePropertyDict.class);
        hierarchyTypePropertyDictService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出层级类型属性字典列表
     */
    @SaCheckPermission("hm:hierarchyTypePropertyDict:export")
    @Log(title = "层级类型属性字典", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated HierarchyTypePropertyDictBo bo, HttpServletResponse response) {
        List<HierarchyTypePropertyDictVo> list = hierarchyTypePropertyDictService.queryList(bo);
        ExcelUtil.exportExcel(list, "层级类型属性字典", HierarchyTypePropertyDictVo.class, response);
    }

    /**
     * 获取层级类型属性字典详细信息
     *
     * @param id 层级类型属性字典ID
     */
    @SaCheckPermission("hm:hierarchyTypePropertyDict:query")
    @GetMapping("/{id}")
    public R<HierarchyTypePropertyDictVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(hierarchyTypePropertyDictService.queryById(id));
    }

    /**
     * 新增层级类型属性字典
     */
    @SaCheckPermission("hm:hierarchyTypePropertyDict:add")
    @Log(title = "层级类型属性字典", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody HierarchyTypePropertyDictBo bo) {
        return toAjax(hierarchyTypePropertyDictService.insertByBo(bo));
    }

    /**
     * 修改层级类型属性字典
     */
    @SaCheckPermission("hm:hierarchyTypePropertyDict:edit")
    @Log(title = "层级类型属性字典", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody HierarchyTypePropertyDictBo bo) {
        return toAjax(hierarchyTypePropertyDictService.updateByBo(bo));
    }

    /**
     * 删除层级类型属性字典
     *
     * @param ids 层级类型属性字典ID串
     */
    @SaCheckPermission("hm:hierarchyTypePropertyDict:remove")
    @Log(title = "层级类型属性字典", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(hierarchyTypePropertyDictService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    @GetMapping("/getDataTypeEnum")
    public R<Map<Integer, String>> getDataTypeEnum() {
                                                // 根据编码获取枚举
        Map<Integer, String> typeMap = DataTypeEnum.getTypeMap();
        return R.ok(typeMap);
    }

}
