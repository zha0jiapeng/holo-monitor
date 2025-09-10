# MP Payload 解析工具

## 概述

这个模块提供了解析 MP 系统 base64 编码 payload 数据的工具类，完全对应 JavaScript 中 `MPBinaryConverter.dataPointsFromBase64` 方法的功能。

## 文件说明

### MPBinaryConverter.java
核心工具类，用于解析 base64 编码的 payload 数据。

**主要方法：**
- `dataPointsFromBase64(String payload)`: 解析单个 payload
- `batchParsePayloads(List<String> payloads)`: 批量解析多个 payload

**使用示例：**
```java
// 解析单个payload
List<DataPointBean> dataPoints = MPBinaryConverter.dataPointsFromBase64("your_base64_payload");

// 批量解析
List<List<DataPointBean>> batchResults = MPBinaryConverter.batchParsePayloads(payloadList);
```

### DataPointBean.java
数据点实体类，包含时间戳和数值两个字段。

**字段：**
- `Date time`: 时间戳
- `Double value`: 数值

### MPBinaryConverterTest.java
测试类，提供基本的测试用例。

## 技术细节

### 数据格式
每个数据点在二进制中占用 16 字节：
- 前 8 字节：long 类型的时间 ticks（小端字节序）
- 后 8 字节：double 类型的数值（小端字节序）

### 时间转换
使用 .NET DateTime 的纪元偏移量进行时间转换：
- 纪元偏移：621355968000000000L
- 每毫秒 ticks 数：10000L

### 字节序
使用小端字节序（LITTLE_ENDIAN）以确保与 JavaScript 实现一致。

## 集成使用

在 `ReportSyncExecutor.java` 中已经集成了 payload 解析功能：

```java
// 获取事件数据
JSONObject events = SD400MPUtils.events("1", "2025-01-01T05:53:35.224Z", "2025-09-30T05:53:35.224Z", mpidMultipleJson, true);

// 解析payload
if (events != null && events.getInt("code") == 200) {
    parseEventPayloads(events);
}
```

## 异常处理

工具类包含完善的异常处理：
- 空 payload 检查
- base64 解码异常处理
- 数据解析异常处理

## 性能考虑

- 使用 `ByteBuffer` 进行高效的二进制数据处理
- 避免不必要的对象拷贝
- 支持批量处理以提高效率

## 注意事项

1. 确保输入的 payload 是有效的 base64 字符串
2. 时间戳会自动转换为本地时区的时间
3. 空 payload 会返回空列表，不会抛出异常
4. 解析失败的 payload 会在日志中记录错误信息

## 依赖

- `lombok`: 用于自动生成 getter/setter 方法
- `slf4j`: 用于日志记录
