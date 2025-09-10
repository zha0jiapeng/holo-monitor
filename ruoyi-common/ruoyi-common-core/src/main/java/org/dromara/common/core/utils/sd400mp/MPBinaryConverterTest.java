package org.dromara.common.core.utils.sd400mp;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * MPBinaryConverter工具类测试类
 *
 * @author your-name
 */
@Slf4j
public class MPBinaryConverterTest {

    public static void main(String[] args) {
        // 测试空payload
        testEmptyPayload();

        // 测试简单payload（这里需要真实的base64数据进行测试）
        // testSimplePayload();
    }

    /**
     * 测试空payload
     */
    private static void testEmptyPayload() {
        log.info("=== 测试空payload ===");

        // 测试null
        List<DataPointBean> result1 = MPBinaryConverter.dataPointsFromBase64(null);
        log.info("null payload结果: {}", result1.size());

        // 测试空字符串
        List<DataPointBean> result2 = MPBinaryConverter.dataPointsFromBase64("");
        log.info("空字符串payload结果: {}", result2.size());

        // 测试空白字符串
        List<DataPointBean> result3 = MPBinaryConverter.dataPointsFromBase64("   ");
        log.info("空白字符串payload结果: {}", result3.size());

        log.info("空payload测试完成");
    }

    /**
     * 测试简单payload（需要真实的base64数据）
     * 注意：这里需要替换为真实的payload数据进行测试
     */
    private static void testSimplePayload() {
        log.info("=== 测试简单payload ===");

        // 这里应该使用真实的base64编码的payload数据进行测试
        // 例如：String testPayload = "your_actual_base64_payload_here";

        // 以下是示例代码：
        // try {
        //     List<DataPointBean> result = MPBinaryConverter.dataPointsFromBase64(testPayload);
        //     log.info("解析成功，共获得{}个数据点", result.size());
        //     for (DataPointBean point : result) {
        //         log.info("时间: {}, 数值: {}", point.getTime(), point.getValue());
        //     }
        // } catch (Exception e) {
        //     log.error("解析失败: {}", e.getMessage());
        // }

        log.info("简单payload测试完成（需要真实数据）");
    }
}
