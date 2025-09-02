package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.domain.bo.TestpointBo;
import org.dromara.hm.domain.vo.TestpointVo;
import lombok.RequiredArgsConstructor;
import org.dromara.hm.enums.TestpointTypeEnum;
import org.dromara.hm.service.ITestpointService;
import org.dromara.hm.validate.BindGroup;
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
 * 测点Controller
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/testpoint")
public class TestpointController extends BaseController {

    private final ITestpointService testpointService;

    /**
     * 查询测点列表
     */
    @SaCheckPermission("hm:testpoint:list")
    @GetMapping("/list")
    public TableDataInfo<TestpointVo> list(@Validated(QueryGroup.class) TestpointBo bo, PageQuery pageQuery) {
        return testpointService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("hm:testpoint:list")
    @GetMapping("/positionList")
    public R<List<Testpoint>> positionList(Long equipmentId) {
        LambdaQueryWrapper<Testpoint> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
            Testpoint::getId,
            Testpoint::getHierarchyId,
            Testpoint::getKksCode,
            Testpoint::getKksName,
            Testpoint::getMt,
            //Testpoint::getType,
            Testpoint::getKksName,
            Testpoint::getPositionX,
            Testpoint::getPositionY,
            Testpoint::getPositionZ
        );
        queryWrapper.eq(Testpoint::getHierarchyId, equipmentId);
        queryWrapper.isNotNull(Testpoint::getPositionX);
        return R.ok(testpointService.list(queryWrapper));
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("hm:testpoint:list")
    @GetMapping("/page")
    public TableDataInfo<TestpointVo> page(@Validated(QueryGroup.class) TestpointBo bo, PageQuery pageQuery) {
        return testpointService.customPageList(bo, pageQuery);
    }

    /**
     * 根据设备ID查询测点列表
     */
    @SaCheckPermission("hm:testpoint:list")
    @GetMapping("/equipment/{equipmentId}")
    public R<List<TestpointVo>> listByEquipment(@NotNull(message = "设备ID不能为空")
                                              @PathVariable("equipmentId") Long equipmentId) {
        return R.ok(testpointService.queryByEquipmentId(equipmentId));
    }

    /**
     * 根据KKS编码查询测点
     */
    @SaCheckPermission("hm:testpoint:query")
    @GetMapping("/kks/{kksCode}")
    public R<TestpointVo> getByKksCode(@NotNull(message = "KKS编码不能为空")
                                     @PathVariable("kksCode") String kksCode) {
        return R.ok(testpointService.queryByKksCode(kksCode));
    }

    /**
     * 获取测点类型映射列表
     */
    @SaCheckPermission("hm:testpoint:list")
    @GetMapping("/types")
    public R<Map<Integer, String>> getTestPointTypes() {
        Map<Integer, String> typeMap = TestpointTypeEnum.getTypeMap();
        return R.ok(typeMap);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "测点", businessType = BusinessType.IMPORT)
    @SaCheckPermission("hm:testpoint:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<TestpointBo> excelResult = ExcelUtil.importExcel(file.getInputStream(), TestpointBo.class, true);
        List<Testpoint> list = MapstructUtils.convert(excelResult.getList(), Testpoint.class);
        testpointService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出测点列表
     */
    @SaCheckPermission("hm:testpoint:export")
    @Log(title = "测点", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated TestpointBo bo, HttpServletResponse response) {
        List<TestpointVo> list = testpointService.queryList(bo);
        ExcelUtil.exportExcel(list, "测点", TestpointVo.class, response);
    }

    /**
     * 获取测点详细信息
     *
     * @param id 测点ID
     */
    @SaCheckPermission("hm:testpoint:query")
    @GetMapping("/{id}")
    public R<TestpointVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(testpointService.queryById(id));
    }

    /**
     * 新增测点
     */
    @SaCheckPermission("hm:testpoint:add")
    @Log(title = "测点", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody TestpointBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(testpointService.insertByBo(bo));
    }

    /**
     * 修改测点
     */
    @SaCheckPermission("hm:testpoint:edit")
    @Log(title = "测点", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody TestpointBo bo) {
        return toAjax(testpointService.updateByBo(bo));
    }

    /**
     * 绑定测点坐标
     */
    @SaCheckPermission("hm:testpoint:edit")
    @Log(title = "绑定测点坐标", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/bind")
    public R<Void> bind(@Validated(BindGroup.class) @RequestBody List<TestpointBo> bo) {
        return toAjax(testpointService.updateBatchByBo(bo));
    }

    /**
     * 解绑测点坐标
     */
    @SaCheckPermission("hm:testpoint:edit")
    @Log(title = "解绑测点坐标", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/unbind")
    public R<Void> unbind(@RequestBody List<Long> ids) {
        return toAjax(testpointService.unbind(ids));
    }

    /**
     * 删除测点
     *
     * @param ids 测点ID串
     */
    @SaCheckPermission("hm:testpoint:remove")
    @Log(title = "测点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(testpointService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
