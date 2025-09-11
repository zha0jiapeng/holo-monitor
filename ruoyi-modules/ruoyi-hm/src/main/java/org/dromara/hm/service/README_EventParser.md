# Events解析器使用说明

## 概述

这个模块提供了完全对应JavaScript中`getEventListAsync`方法功能的Java实现，可以解析SD400MP系统的events响应数据，输出与JavaScript相同结构的结果。

## 核心类说明

### EventParserService
主要的解析服务类，提供`parseEvents`方法来解析events响应。

**主要功能：**
- 解析PDE类信息
- 获取设备和测点名称映射
- 处理事件payload数据
- 处理卫星数据
- 构建与JavaScript相同的数据结构

### 相关实体类

#### MPEventList
事件列表容器，包含：
- `Map<Integer, MPPdeClassInfo> pdClasses` - PDE类信息映射
- `Map<Long, String> namesEq` - 设备名称映射
- `Map<Long, String> namesTp` - 测点名称映射  
- `Map<String, MPEventGroup> groups` - 事件分组映射

#### MPEventGroup
事件分组，包含：
- `String key` - 分组键
- `MPTag tag` - 标签信息
- `MPTag satelliteTag` - 卫星标签
- `List<MPEvent> events` - 事件列表
- `Map<Long, MPDisplaySettings> displaySettings` - 显示设置

#### MPEvent
单个事件，包含：
- `MPEventGroup group` - 所属分组
- `Integer state` - 状态值
- `Date start` - 开始时间
- `Date end` - 结束时间
- `Long equipmentId` - 设备ID
- `Long testpointId` - 测点ID
- `Double satelliteValue` - 卫星值

## 使用方式

### 1. 在任务中使用（如ReportSyncExecutor）

```java
@Autowired
private EventParserService eventParserService;

public void processEvents() {
    // 获取events数据
    JSONObject events = SD400MPUtils.events("37", "2025-01-01T05:53:35.224Z", 
                                           "2025-09-30T05:53:35.224Z", testpoints, true);
    
    // 解析events数据
    if (events != null && events.getInt("code") == 200) {
        MPEventList eventList = eventParserService.parseEvents(events);
        if (eventList != null) {
            // 处理解析后的数据
            processEventList(eventList);
        }
    }
}

private void processEventList(MPEventList eventList) {
    // 遍历所有事件分组
    eventList.getGroups().forEach((key, group) -> {
        log.info("事件分组: {}, 事件数量: {}", key, group.getEvents().size());
        
        // 遍历分组中的所有事件
        group.getEvents().forEach(event -> {
            String equipmentName = eventList.getNamesEq().get(event.getEquipmentId());
            String testpointName = eventList.getNamesTp().get(event.getTestpointId());
            
            // 处理单个事件
            log.info("事件 - 设备: {} ({}), 测点: {} ({}), 状态: {}, 时间: {}", 
                    equipmentName, event.getEquipmentId(),
                    testpointName, event.getTestpointId(),
                    event.getState(), event.getStart());
        });
    });
}
```

### 2. 通过REST API测试

#### 解析events数据
```http
POST /hm/eventParser/parseEvents
Content-Type: application/x-www-form-urlencoded

idEquipment=37&from=2025-01-01T05:53:35.224Z&to=2025-09-30T05:53:35.224Z&withConnectionState=true
```

#### 获取原始events数据（调试用）
```http
POST /hm/eventParser/getRawEvents
Content-Type: application/x-www-form-urlencoded

idEquipment=37&from=2025-01-01T05:53:35.224Z&to=2025-09-30T05:53:35.224Z&withConnectionState=true
```

## 数据流程

1. **获取events原始数据** - 通过`SD400MPUtils.events()`获取JSON响应
2. **解析PDE类信息** - 调用`/api/pdeClasses`获取分类信息
3. **收集ID信息** - 从响应中提取设备和测点ID
4. **获取名称映射** - 并行调用`/api/nameseq`和`/api/namestp`
5. **处理事件数据** - 遍历设备和测点，解析payload数据
6. **构建结果对象** - 创建与JavaScript相同结构的`MPEventList`

## 与JavaScript的对应关系

| JavaScript | Java |
|------------|------|
| `getEventListAsync()` | `EventParserService.parseEvents()` |
| `MPEventList` | `MPEventList` |
| `MPEventGroup` | `MPEventGroup` |
| `MPEvent` | `MPEvent` |
| `MPBinaryConverter.dataPointsFromBase64()` | `MPBinaryConverter.dataPointsFromBase64()` |
| `this.tags.get()` | `getTagsMapping()` (需要实现) |

## 注意事项

1. **标签映射** - 目前`getTagsMapping()`方法返回空映射，需要根据实际情况实现标签获取逻辑
2. **并行请求** - 当前实现是串行的，可以优化为并行请求以提高性能
3. **错误处理** - 包含完善的异常处理和日志记录
4. **内存使用** - 大量数据时注意内存使用情况

## 扩展功能

可以基于解析后的数据实现：
- 事件统计分析
- 实时告警
- 趋势分析
- 报表生成
- 数据持久化

## 调试建议

1. 使用`/getRawEvents`接口查看原始数据结构
2. 检查日志中的解析过程信息
3. 验证时间格式和设备ID的正确性
4. 确认payload数据不为空
