package org.dromara.hm.controller;


import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.hm.domain.bo.TestpointOfflineBo;
import org.dromara.hm.domain.vo.TestpointOfflineVo;
import org.dromara.hm.service.ITestpointOfflineService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测点离线记录
 *
 * @author Mashir0
 * @date 2025-01-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/testpointOffline")
public class TestpointOfflineController extends BaseController {

    private final ITestpointOfflineService testpointOfflineService;

    /**
     * 查询测点离线记录列表
     */
    @GetMapping("/list")
    public TableDataInfo<TestpointOfflineVo> list(TestpointOfflineBo bo, PageQuery pageQuery) {
        return testpointOfflineService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询测点离线记录列表
     */
    @GetMapping("/customList")
    public TableDataInfo<TestpointOfflineVo> customList(TestpointOfflineBo bo, PageQuery pageQuery) {
        return testpointOfflineService.customPageList(bo, pageQuery);
    }

    /**
     * 导出测点离线记录列表
     */
    @Log(title = "测点离线记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(TestpointOfflineBo bo, HttpServletResponse response) {
        List<TestpointOfflineVo> list = testpointOfflineService.queryList(bo);
        ExcelUtil.exportExcel(list, "测点离线记录", TestpointOfflineVo.class, response);
    }

    /**
     * 获取测点离线记录详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<TestpointOfflineVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(testpointOfflineService.queryById(id));
    }

    /**
     * 新增测点离线记录
     */
    @Log(title = "测点离线记录", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody TestpointOfflineBo bo) {
        return toAjax(testpointOfflineService.insertByBo(bo));
    }

    /**
     * 修改测点离线记录
     */
    @Log(title = "测点离线记录", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody TestpointOfflineBo bo) {
        return toAjax(testpointOfflineService.updateByBo(bo));
    }

    /**
     * 删除测点离线记录
     *
     * @param ids 主键串
     */
    @Log(title = "测点离线记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(testpointOfflineService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 根据测点编码查询当前离线记录
     *
     * @param kksCode 测点编码
     */
    @GetMapping("/current/{kksCode}")
    public R<TestpointOfflineVo> getCurrentOffline(@PathVariable String kksCode) {
        return R.ok(testpointOfflineService.queryCurrentOfflineByKksCode(kksCode));
    }

    /**
     * 根据测点编码查询历史离线记录
     *
     * @param kksCode 测点编码
     */
    @GetMapping("/history/{kksCode}")
    public R<List<TestpointOfflineVo>> getHistoryOffline(@PathVariable String kksCode) {
        return R.ok(testpointOfflineService.queryHistoryByKksCode(kksCode));
    }

    /**
     * 获取离线状态统计
     */
    @GetMapping("/statistics")
    public R<List<TestpointOfflineVo>> getOfflineStatistics(@RequestParam(required = false) LocalDateTime startTime,
                                                           @RequestParam(required = false) LocalDateTime endTime) {
        return R.ok(testpointOfflineService.getOfflineStatistics(startTime, endTime));
    }

    /**
     * 手动记录测点恢复（用于测试或特殊情况）
     *
     * @param kksCode 测点编码
     */
    @Log(title = "测点离线记录", businessType = BusinessType.UPDATE)
    @PostMapping("/recordRecovery")
    public R<Void> recordRecovery(@RequestParam String kksCode) {
        return toAjax(testpointOfflineService.recordRecovery(kksCode));
    }
}
