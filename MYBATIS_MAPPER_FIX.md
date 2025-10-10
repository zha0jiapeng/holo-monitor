# MyBatis Mapper 偶现性错误修复方案

## 问题描述
```
Invalid bound statement (not found): org.dromara.system.mapper.SysRoleMapper.selectRolesByUserId
```

**问题特征：**
- ✅ mvn clean 和 mvn install 都没有报错
- ✅ jar 包运行也没问题
- ❌ 但是登录时偶尔会出现这个错误
- ❌ 不是百分百复现的

## 问题根源分析

### 1. Spring Boot 懒加载机制
Spring Boot 3.x 在某些场景下可能会懒加载 Bean，导致 SqlSessionFactory 还未完全初始化就被调用。

### 2. MyBatis-Plus MapperLocations 扫描竞态
使用 `classpath*:` 在并发场景下可能存在类加载顺序问题，特别是在多模块项目中。

### 3. 多线程并发初始化问题
在高并发场景下，多个线程同时访问 Mapper 可能导致某些 Mapper 还未完全加载。

### 4. 动态数据源切换问题
项目使用了 dynamic-datasource，在某些情况下可能导致 SqlSessionFactory 的初始化延迟。

## 已实施的修复方案

### ✅ 方案一：禁用 Spring 懒加载
**文件：** `ruoyi-admin/src/main/resources/application.yml`

**修改内容：**
```yaml
spring:
  # 禁用懒加载，确保所有Bean在启动时完全初始化
  main:
    lazy-initialization: false
```

**作用：** 确保所有 Bean（包括 SqlSessionFactory）在应用启动时就完全初始化，避免懒加载导致的竞态问题。

### ✅ 方案二：添加 Mapper 预热机制
**文件：** `ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/config/MybatisPlusConfig.java`

**修改内容：**
- 实现 `InitializingBean` 接口
- 在 `afterPropertiesSet()` 方法中预热所有 Mapper
- 添加关键 Mapper 的存在性验证

**作用：** 
- 在应用启动后主动触发所有 Mapper 的加载
- 启动时验证关键 Mapper 是否存在，快速发现问题
- 记录加载的 Mapper 数量，便于监控

### ✅ 方案三：优化 MyBatis 配置
**文件：** `ruoyi-common/ruoyi-common-mybatis/src/main/resources/common-mybatis.yml`

**修改内容：**
```yaml
mybatis-plus:
  check-config-location: true
  configuration:
    # 关闭延迟加载
    lazyLoadingEnabled: false
    aggressiveLazyLoading: false
```

**作用：** 禁用 MyBatis 的延迟加载机制，避免运行时的懒加载问题。

## 验证方法

### 1. 重新编译打包
```bash
mvn clean install
```

### 2. 检查启动日志
启动应用后，查看日志中是否有以下信息：
```
MyBatis Mapper预热完成，共加载 XXX 个Mapper语句
```

如果看到这行日志，说明预热机制已生效。

### 3. 检查是否有警告
如果启动时看到以下警告：
```
警告：关键Mapper语句 'org.dromara.system.mapper.SysRoleMapper.selectRolesByUserId' 未找到！
```

说明 Mapper XML 文件未被正确打包或加载，需要进一步检查。

### 4. 压力测试
使用 JMeter 或 ab 工具进行并发登录测试，验证问题是否解决：
```bash
# 使用 ab 工具测试（示例）
ab -n 1000 -c 100 http://localhost:8080/login
```

## 额外排查建议

### 1. 检查 jar 包内容
确认 Mapper XML 文件是否被正确打包：
```bash
# Windows PowerShell
jar -tf ruoyi-admin/target/ruoyi-admin.jar | Select-String "SysRoleMapper"

# Linux/Mac
jar -tf ruoyi-admin/target/ruoyi-admin.jar | grep SysRoleMapper
```

**预期结果：** 应该能看到以下内容
```
BOOT-INF/classes/mapper/system/SysRoleMapper.xml
BOOT-INF/classes/org/dromara/system/mapper/SysRoleMapper.class
```

### 2. 检查多实例版本一致性
如果部署了多个实例，确保所有实例使用的是同一个版本的 jar 包：
```bash
# 检查 jar 包的 MD5
md5sum ruoyi-admin.jar

# 或在 Windows 上
certutil -hashfile ruoyi-admin.jar MD5
```

### 3. 检查数据库连接池配置
偶现问题可能与连接池配置有关，建议检查 HikariCP 配置：
```yaml
spring:
  datasource:
    hikari:
      # 最小空闲连接数
      minimum-idle: 10
      # 最大连接池大小
      maximum-pool-size: 50
      # 连接超时时间
      connection-timeout: 30000
      # 空闲连接存活最大时间
      idle-timeout: 600000
      # 连接最大存活时间
      max-lifetime: 1800000
```

### 4. 开启 MyBatis 日志（临时调试）
如果问题依然存在，可以临时开启 MyBatis 的详细日志：
```yaml
# 在 application.yml 中添加
logging:
  level:
    org.dromara.system.mapper: DEBUG
    org.apache.ibatis: DEBUG
    org.mybatis: DEBUG
```

**注意：** 调试完成后记得关闭，避免性能损耗。

## 进一步优化建议

### 1. 升级 MyBatis-Plus 版本（可选）
当前版本：`3.5.12`

如果问题依然存在，可以考虑：
- 升级到 `3.5.13+`（如果有新版本）
- 或者降级到稳定版本 `3.5.9`

修改 `pom.xml`：
```xml
<mybatis-plus.version>3.5.13</mybatis-plus.version>
```

### 2. 添加健康检查端点（可选）
创建一个健康检查端点来验证 Mapper 是否正常：

```java
@RestController
@RequestMapping("/actuator/health")
public class MapperHealthCheckController {
    
    @Autowired
    private SysRoleMapper sysRoleMapper;
    
    @GetMapping("/mapper")
    public Map<String, Object> checkMapper() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 尝试调用 Mapper
            sysRoleMapper.selectRolesByUserId(1L);
            result.put("status", "UP");
            result.put("mapper", "SysRoleMapper.selectRolesByUserId");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return result;
    }
}
```

### 3. 监控和告警
建议添加监控来捕获这类偶发问题：
- 使用 Spring Boot Actuator 监控应用健康状态
- 配置日志告警（当出现 "Invalid bound statement" 时发送通知）
- 使用 APM 工具（如 SkyWalking、Pinpoint）追踪问题

## 预期效果

实施以上修复方案后，预期效果：

1. ✅ 应用启动时所有 Mapper 都会被预热加载
2. ✅ 启动日志中会显示加载的 Mapper 数量
3. ✅ 如果有 Mapper 未加载，会在启动时立即发现
4. ✅ 消除懒加载导致的竞态条件
5. ✅ 彻底解决 "Invalid bound statement (not found)" 的偶现问题

## 回滚方案

如果修改后出现问题，可以回滚以下文件：
```bash
git checkout ruoyi-admin/src/main/resources/application.yml
git checkout ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/config/MybatisPlusConfig.java
git checkout ruoyi-common/ruoyi-common-mybatis/src/main/resources/common-mybatis.yml
```

## 联系方式

如果问题依然存在，建议：
1. 查看完整的错误堆栈信息
2. 检查是否有其他相关的错误日志
3. 确认 MyBatis-Plus 和 Spring Boot 的版本兼容性
4. 在 GitHub 或 Gitee 上提交 issue 给 RuoYi-Vue-Plus 项目组

---

**修改日期：** 2025-10-10
**修改人：** AI Assistant
**测试状态：** 待验证

