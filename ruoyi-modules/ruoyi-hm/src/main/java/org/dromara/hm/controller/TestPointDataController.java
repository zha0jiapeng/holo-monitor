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
import org.dromara.hm.domain.TestPointData;
import org.dromara.hm.domain.bo.TestPointDataBo;
import org.dromara.hm.domain.vo.TestPointDataVo;
import org.dromara.hm.service.ITestPointDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 测点数据Controller
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/testpointdata")
public class TestPointDataController extends BaseController {

    private final ITestPointDataService testPointDataService;

    /**
     * 查询测点数据列表
     */
    @SaCheckPermission("hm:testpointdata:list")
    @GetMapping("/list")
    public TableDataInfo<TestPointDataVo> list(@Validated(QueryGroup.class) TestPointDataBo bo, PageQuery pageQuery) {
        return testPointDataService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:testpointdata:list")
    @GetMapping("/page")
    public TableDataInfo<TestPointDataVo> page(@Validated(QueryGroup.class) TestPointDataBo bo, PageQuery pageQuery) {
        return testPointDataService.customPageList(bo, pageQuery);
    }

    /**
     * 根据KKS编码查询测点数据列表
     */
    @SaCheckPermission("hm:testpointdata:list")
    @GetMapping("/kks/{kksCode}")
    public R<List<TestPointDataVo>> listByKksCode(@NotBlank(message = "KKS编码不能为空")
                                                @PathVariable("kksCode") String kksCode) {
        return R.ok(testPointDataService.queryByKksCode(kksCode));
    }

    /**
     * 根据KKS编码和时间范围查询测点数据
     */
    @SaCheckPermission("hm:testpointdata:query")
    @GetMapping("/kks/{kksCode}/timerange")
    public R<List<TestPointDataVo>> listByKksCodeAndTimeRange(
            @NotBlank(message = "KKS编码不能为空") @PathVariable("kksCode") String kksCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(testPointDataService.queryByKksCodeAndTimeRange(kksCode, startTime, endTime));
    }

    /**
     * 根据报警状态查询测点数据
     */
    @SaCheckPermission("hm:testpointdata:query")
    @GetMapping("/alarm")
    public R<List<TestPointDataVo>> listByAlarmStatus(
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) Integer st) {
        return R.ok(testPointDataService.queryByAlarmStatus(alarmType, st));
    }

    /**
     * 获取最新的测点数据
     */
    @SaCheckPermission("hm:testpointdata:query")
    @GetMapping("/latest/{kksCode}")
    public R<TestPointDataVo> getLatest(@NotBlank(message = "KKS编码不能为空")
                                      @PathVariable("kksCode") String kksCode) {
        return R.ok(testPointDataService.getLatestByKksCode(kksCode));
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "测点数据", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:testpointdata:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<TestPointDataBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), TestPointDataBo.class, true);
        List<TestPointData> list = MapstructUtils.convert(excelResult.getList(), TestPointData.class);
        testPointDataService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出测点数据列表
     */
    @SaCheckPermission("hm:testpointdata:export")
    @Log(title = "测点数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated TestPointDataBo bo, HttpServletResponse response) {
        List<TestPointDataVo> list = testPointDataService.queryList(bo);
        ExcelUtil.exportExcel(list, "测点数据", TestPointDataVo.class, response);
    }

    /**
     * 获取测点数据详细信息
     *
     * @param id 测点数据ID
     */
    @SaCheckPermission("hm:testpointdata:query")
    @GetMapping("/{id}")
    public R<TestPointDataVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable("id") Long id) {
        return R.ok(testPointDataService.queryById(id));
    }

    /**
     * 新增测点数据
     */
    @SaCheckPermission("hm:testpointdata:add")
    @Log(title = "测点数据", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody TestPointDataBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(testPointDataService.insertByBo(bo));
    }

    /**
     * 修改测点数据
     */
    @SaCheckPermission("hm:testpointdata:edit")
    @Log(title = "测点数据", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody TestPointDataBo bo) {
        return toAjax(testPointDataService.updateByBo(bo));
    }

    /**
     * 批量新增测点数据
     */
    @SaCheckPermission("hm:testpointdata:add")
    @Log(title = "测点数据", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/batch")
    public R<Void> addBatch(@RequestBody List<TestPointDataBo> boList) {
        List<TestPointData> list = MapstructUtils.convert(boList, TestPointData.class);
        return toAjax(testPointDataService.saveBatch(list));
    }

    /**
     * 删除测点数据
     *
     * @param ids 测点数据ID串
     */
    @SaCheckPermission("hm:testpointdata:remove")
    @Log(title = "测点数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(testPointDataService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
