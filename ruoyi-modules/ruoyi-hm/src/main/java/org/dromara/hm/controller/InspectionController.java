package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 巡检数据Controller
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/pms-app-tf-overhauldevmgt-svr")
public class InspectionController {

    /**
     * 接收巡检局放异常数据
     */
    @PostMapping(value = "/inspectionPartialDischargeAbnormal/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaIgnore
    public R<String> addInspectionPartialDischargeAbnormal(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String partialDischargeAverage,
            @RequestParam(required = false) String partialDischargeAverageType,
            @RequestParam(required = false) String partialDischargeAverageUnit,
            @RequestParam(required = false) String partialDischargePulseCount,
            @RequestParam(required = false) String partialDischargeCountPulseType,
            @RequestParam(required = false) String partialDischargeCountPulseUnit,
            @RequestParam(required = false) String partialDischargeResult,
            @RequestParam(required = false) String endDeviceId,
            @RequestParam(required = false) String endDeviceType,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String generationfindTime,
            @RequestParam(required = false) String inspectionChartType,
            @RequestParam Map<String, Object> allParams) {

        log.info("=== 接收到巡检局放异常数据 ===");
        log.info("sourceType: {}", sourceType);
        log.info("sourceId: {}", sourceId);
        log.info("partialDischargeAverage: {}", partialDischargeAverage);
        log.info("partialDischargeAverageType: {}", partialDischargeAverageType);
        log.info("partialDischargeAverageUnit: {}", partialDischargeAverageUnit);
        log.info("partialDischargePulseCount: {}", partialDischargePulseCount);
        log.info("partialDischargeCountPulseType: {}", partialDischargeCountPulseType);
        log.info("partialDischargeCountPulseUnit: {}", partialDischargeCountPulseUnit);
        log.info("partialDischargeResult: {}", partialDischargeResult);
        log.info("endDeviceId: {}", endDeviceId);
        log.info("endDeviceType: {}", endDeviceType);
        log.info("generationfindTime: {}", generationfindTime);
        log.info("inspectionChartType: {}", inspectionChartType);

        if (file != null) {
            log.info("file name: {}", file.getOriginalFilename());
            log.info("file size: {} bytes", file.getSize());
            log.info("file content type: {}", file.getContentType());
        } else {
            log.info("file: null");
        }
        log.info("=== 参数打印完毕 ===");

        return R.ok("数据接收成功");
    }
}
