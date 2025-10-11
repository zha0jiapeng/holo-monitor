# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **4D Holographic Monitoring System (4D全息监测系统)** built on top of **RuoYi-Vue-Plus** (v5.4.1) - a Spring Boot 3.4 enterprise application framework with multi-tenancy support. The project is named "holo-monitor" and includes custom monitoring modules for holographic sensor data management.

**Base Framework**: Dromara RuoYi-Vue-Plus
**Primary Language**: Java 17+
**Spring Boot**: 3.4.7
**Architecture**: Modular microservice-ready monolith with pluggable extensions

## Project Structure

```
holo-monitor/
├── ruoyi-admin/          # Main application entry point (DromaraApplication.java)
├── ruoyi-common/         # Common utilities and shared components (26+ modules)
│   ├── ruoyi-common-core/       # Core utilities, includes SD400MPUtils
│   ├── ruoyi-common-mybatis/    # MyBatis-Plus configurations
│   ├── ruoyi-common-redis/      # Redisson client
│   ├── ruoyi-common-satoken/    # Sa-Token auth
│   └── ...                      # Other common modules
├── ruoyi-modules/        # Business modules
│   ├── ruoyi-system/     # System management (users, roles, menus, etc.)
│   ├── ruoyi-hm/         # **Custom holographic monitoring module**
│   ├── ruoyi-generator/  # Code generator
│   ├── ruoyi-demo/       # Demo examples
│   ├── ruoyi-job/        # SnailJob task scheduler
│   └── ruoyi-workflow/   # Warm-Flow workflow engine
├── ruoyi-extend/         # Extended modules (monitor-admin, snailjob-server)
└── script/               # Deployment scripts (Docker, shell scripts)
```

## Build & Run Commands

### Development Environment

```bash
# Build the project (skips tests by default)
mvn clean package

# Build with specific profile
mvn clean package -P dev
mvn clean package -P prod

# Run the application (default port 8080)
cd ruoyi-admin/target
java -jar ruoyi-admin.jar

# Or use the startup script
cd script/bin
./ry.sh start      # Start
./ry.sh stop       # Stop
./ry.sh restart    # Restart
./ry.sh status     # Check status
```

### Testing

```bash
# Run tests for specific environment
mvn test -P dev
mvn test -P prod

# Tests are tagged by environment (@Tag annotation)
# - @Tag("dev") runs with -P dev
# - @Tag("prod") runs with -P prod
# - @Tag("exclude") are always excluded
```

### Docker Deployment

```bash
# Start all services (MySQL, Redis, Nginx)
cd script/docker
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## Maven Profiles

- **local**: Local development (not in version control)
- **dev**: Development environment (default, active: true)
- **prod**: Production environment

Active profile is set in `ruoyi-admin/src/main/resources/application.yml`:
```yaml
spring:
  profiles:
    active: prod  # Currently set to prod
```

## Key Technologies & Frameworks

### Core Stack
- **Web Container**: Undertow (high-performance XNIO-based)
- **ORM**: MyBatis-Plus 3.5.12 (with multi-datasource support)
- **Cache**: Redisson 3.50.0 (Redis client)
- **Authentication**: Sa-Token 1.44.0 with JWT
- **API Docs**: SpringDoc (OpenAPI 3.0) with javadoc integration
- **Database**: Supports MySQL, PostgreSQL, Oracle, SQL Server
- **Primary Key**: Snowflake ID (ASSIGN_ID strategy)

### Important Plugins & Tools
- **dynamic-datasource**: Multi-datasource with runtime switching
- **Lock4j**: Distributed locks via Redisson
- **SnailJob**: Distributed task scheduler
- **Warm-Flow**: Workflow engine (http://warm-flow.cn/)
- **p6spy**: SQL monitoring and logging
- **FastExcel**: Excel import/export (formerly Alibaba EasyExcel)
- **MapStruct Plus**: Object mapping
- **Hutool**: Utility library (5.8.38)

### Multi-Tenancy
- Configurable via `tenant.enable` in application.yml (currently: **false**)
- Excluded tables defined in `tenant.excludes`

## Custom Module: ruoyi-hm (Holographic Monitoring)

### Purpose
The `ruoyi-hm` module handles sensor data collection, hierarchical equipment management, and integration with the SD400MP monitoring platform.

### Key Components

#### Controllers
- `StatisticsController`: Statistical data and sensor grouping
- `HierarchyController`: Equipment hierarchy management
- `Sd400mpController`: SD400MP platform integration
- `HierarchyPropertyController`: Equipment property management
- `HierarchyTypePropertyController`: Equipment type property definitions

#### Core Service: `HierarchyServiceImpl`
Manages hierarchical equipment structures with advanced features:
- **Batch property loading**: Avoids N+1 query problems
- **Sensor data processing**: Integration with SD400MP via `processSensorData()`
- **Alarm filtering**: `showAllFlag` parameter controls sensor visibility
  - `showAllFlag=true`: Show all sensors
  - `showAllFlag=false`: Show only sensors with `alarmType > 0`
- **AlarmType assignment**: Extracted from `dataSet` where `key="sys:st"`
- **Recursive hierarchy traversal**: Optimized with `getAllChildrenOptimized()`

### SD400MP Integration

The `SD400MPUtils` class (in `ruoyi-common-core`) provides API integration with the SD400MP monitoring platform:

#### Token Management
- Uses local caching with 10-minute TTL
- Thread-safe token refresh with double-checked locking
- Configuration: `GlbProperties.uri`, `GlbProperties.username`, `GlbProperties.password`

#### Key API Methods
```java
SD400MPUtils.getToken()                    // Authentication token
SD400MPUtils.testpointFind(String code)    // Find test point by code
SD400MPUtils.data(Long id, List tags, ts)  // Get sensor data
SD400MPUtils.equipmentList(String id, ...) // Equipment hierarchy
SD400MPUtils.events(...)                   // Event list
SD400MPUtils.dataset(id, time)             // Dataset retrieval
SD400MPUtils.file(Long equipmentId)        // File management
```

#### DataSet Structure
Sensor data from SD400MP includes a `dataSet` array with objects like:
```json
{
  "dt": "2025-09-25T21:15:00",
  "id": 30723,
  "key": "sys:st",     // System status key
  "type": 6,
  "val": "1"           // Used for alarmType assignment
}
```

## Database Configuration

### Primary Configuration
Located in `application-dev.yml` or `application-prod.yml` (based on active profile).

### MyBatis-Plus Settings
```yaml
mybatis-plus:
  enableLogicDelete: true
  mapperPackage: org.dromara.**.mapper
  mapperLocations: classpath*:mapper/**/*Mapper.xml
  typeAliasesPackage: org.dromara.**.domain
  global-config:
    dbConfig:
      idType: ASSIGN_ID  # Snowflake ID generation
```

### Excluded Tables (Multi-tenant)
See `tenant.excludes` in application.yml for tables that bypass tenant filtering (includes `hm_*` tables).

## Code Generation

The framework includes a powerful code generator:

1. Design database table structure
2. Access code generator module (typically at `/tool/gen`)
3. Configure generation options (supports multi-datasource)
4. Generate CRUD code with:
   - Controller, Service, ServiceImpl
   - Mapper XML and interface
   - Domain/VO/BO objects
   - Frontend pages (Vue 3 + TypeScript + ElementPlus)

Generated code follows MyBatis-Plus and SpringDoc conventions.

## API Documentation

Access SpringDoc UI at: `http://localhost:8080/doc.html` (when enabled)

API groups defined in application.yml:
- 1.演示模块 (Demo)
- 2.通用模块 (Common)
- 3.系统模块 (System)
- 4.代码生成模块 (Generator)
- 5.工作流模块 (Workflow)
- **6.全息监测模块 (Holographic Monitoring - org.dromara.hm)**

Authentication: Uses `Authorization` header (Sa-Token token name).

## Security & Authentication

### Sa-Token Configuration
```yaml
sa-token:
  token-name: Authorization
  is-concurrent: true        # Allow concurrent logins
  is-share: false           # Each login gets unique token
  jwt-secret-key: [configured]
```

### API Encryption
- **Enabled**: Global API encryption is ON (`api-decrypt.enabled: true`)
- Uses RSA for key exchange + AES for body encryption
- Keys defined in `api-decrypt.publicKey` and `api-decrypt.privateKey`

### Excluded Paths
See `security.excludes` in application.yml for unauthenticated endpoints.

## Important Development Notes

### Data Permissions
- Implemented via MyBatis-Plus plugins
- SQL is automatically modified to filter by department/role
- Configure via annotations on Mapper methods

### Logging
- Configuration: `logback-plus.xml`
- Log level: `@logging.level@` (info for dev/prod, warn for Spring)
- Console logs: `./logs/sys-console.log`

### Annotation Processors
The project uses multiple annotation processors (configured in root pom.xml):
- Lombok
- MapStruct Plus
- Therapi Javadoc (for SpringDoc)
- Spring Boot Configuration Processor

### Code Style
- Strictly follows Alibaba Java Coding Guidelines
- Use project's `.editorconfig` for consistent formatting
- Prefer MyBatis-Plus entity operations over raw SQL

## Working with Hierarchies

When modifying hierarchy-related code:

1. **Avoid N+1 queries**: Use `batchLoadProperties()` or `batchLoadPropertiesOptimized()`
2. **Sensor filtering**: Always respect `showAllFlag` parameter
3. **AlarmType source**: Must be extracted from `dataSet[key="sys:st"].val`
4. **Recursive operations**: Use `getAllChildrenOptimized()` for better performance
5. **Type resolution**: Use `setTypeKeysForHierarchyList()` to populate typeKey fields

### Example: Sensor Data Processing Flow
```
1. Query sensors → 2. Load SD400MP data → 3. Extract sys:st → 4. Filter by showAllFlag → 5. Return results
```

## Common Patterns

### Response Wrapper
All API responses use `R<T>` generic wrapper:
```java
return R.ok(data);        // Success
return R.fail("error");   // Failure
```

### Pagination
```java
TableDataInfo<Vo> result = service.queryPageList(bo, pageQuery);
```

### Caching
Use Spring Cache annotations with extended features:
```java
@Cacheable(value = "cache:key", ttl = 3600)
```

## Monitoring & Observability

- **Spring Boot Admin**: Available for service monitoring
- **Actuator endpoints**: Exposed at `/actuator/*`
- **Health check**: `/actuator/health` (shows ALWAYS)
- **WebSocket**: Enabled at `/resource/websocket` (for real-time updates)
- **SSE**: Disabled by default (path: `/resource/sse`)

## References

- **Official Documentation**: https://plus-doc.dromara.org
- **Initialization Guide**: https://plus-doc.dromara.org/#/ruoyi-vue-plus/quickstart/init
- **Deployment Guide**: https://plus-doc.dromara.org/#/ruoyi-vue-plus/quickstart/deploy
- **Warm-Flow Workflow**: http://warm-flow.cn/
- **GitHub**: https://github.com/dromara/RuoYi-Vue-Plus
- **Gitee**: https://gitee.com/dromara/RuoYi-Vue-Plus

## Git Workflow

Current branch: `master`
Main branch for PRs: `main`

Recent commits indicate active development on:
- Alarm statistics by equipment point (设备点)
- Real-time alarm interface modifications
- Substation and right bank additions
