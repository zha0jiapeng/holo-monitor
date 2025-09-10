package org.dromara.common.core.utils.sd400mp;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * MP二进制数据转换器，用于解析base64编码的payload数据
 * 对应JavaScript中的MPBinaryConverter.dataPointsFromBase64方法
 *
 * @author your-name
 */
@Slf4j
public class MPBinaryConverter {

    /**
     * .NET DateTime的纪元偏移量
     */
    private static final long EPOCH_OFFSET = 621355968000000000L;

    /**
     * 每毫秒的ticks数
     */
    private static final long TICKS_PER_MILLISECOND = 10000L;

    /**
     * 解析base64编码的payload数据，返回数据点列表
     * 对应JavaScript中的MPBinaryConverter.dataPointsFromBase64方法
     *
     * @param payload base64编码的payload字符串
     * @return 数据点列表，每个数据点包含时间戳和数值
     */
    public static List<DataPointBean> dataPointsFromBase64(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            log.warn("payload为空，返回空列表");
            return new ArrayList<>();
        }

        try {
            // base64解码
            byte[] bytes = Base64.getDecoder().decode(payload.trim());

            // 创建小端字节序的ByteBuffer
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            List<DataPointBean> result = new ArrayList<>();

            // 每个数据点占用16字节：8字节long（ticks） + 8字节double（value）
            while (buffer.remaining() >= 16) {
                // 读取8字节的long（时间ticks）
                long ticks = buffer.getLong();

                // 读取8字节的double（数值）
                double value = buffer.getDouble();

                // 转换为DataPointBean对象
                DataPointBean dataPoint = new DataPointBean();
                dataPoint.setTime(ticksToDate(ticks));
                dataPoint.setValue(value);

                result.add(dataPoint);
            }

            log.debug("成功解析payload，获得{}个数据点", result.size());
            return result;

        } catch (IllegalArgumentException e) {
            log.error("base64解码失败：{}", e.getMessage());
            throw new RuntimeException("payload格式错误，无法解析base64数据", e);
        } catch (Exception e) {
            log.error("解析payload异常：{}", e.getMessage());
            throw new RuntimeException("解析payload数据失败", e);
        }
    }

    /**
     * 将.NET DateTime的ticks转换为Java Date对象
     *
     * @param ticks .NET DateTime的ticks值
     * @return Java Date对象
     */
    private static Date ticksToDate(long ticks) {
        // 计算毫秒数：(ticks - EPOCH_OFFSET) / TICKS_PER_MILLISECOND
        long milliseconds = (ticks - EPOCH_OFFSET) / TICKS_PER_MILLISECOND;
        return new Date(milliseconds);
    }

    /**
     * 批量解析多个payload
     *
     * @param payloads base64编码的payload字符串列表
     * @return 数据点列表的列表
     */
    public static List<List<DataPointBean>> batchParsePayloads(List<String> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<DataPointBean>> results = new ArrayList<>();
        for (String payload : payloads) {
            try {
                List<DataPointBean> dataPoints = dataPointsFromBase64(payload);
                results.add(dataPoints);
            } catch (Exception e) {
                log.error("解析payload失败：{}", e.getMessage());
                results.add(new ArrayList<>()); // 添加空列表以保持顺序
            }
        }

        return results;
    }
}
