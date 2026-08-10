# ArxivRadar Backend

AI 论文情报站的 Java 后端服务。给 [ArxivRadar 前端](../ArxivRadar) 提供论文数据、LLM 翻译/精读代理等能力。

## 技术栈

| 层 | 选择 |
|---|---|
| 语言 | JDK 17 |
| 框架 | Spring Boot 3.3.5 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | PostgreSQL 16 |
| 缓存 | Redis 7 |
| 数据迁移 | Flyway |
| API 文档 | SpringDoc OpenAPI |
| 构建 | Maven 3.9 (含 wrapper) |
| 部署 | Docker + docker-compose |

## 快速开始

### 前置依赖

- JDK 17 (`java -version` 应显示 17.x)
- Docker Desktop 已启动

不需要预装 Maven — 项目自带 `./mvnw` wrapper。

### 1. 拉起 Postgres + Redis

```bash
docker compose up -d
```

首次会拉镜像,约 30 秒。跑完 `docker compose ps` 应看到两个容器都 `healthy`。

### 2. 启动后端

```bash
./mvnw spring-boot:run
```

启动日志里应看到 Flyway 执行了 `V1__init_schema`。

### 3. 验证

| 项目 | 命令 / URL | 预期 |
|---|---|---|
| 健康检查 | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| API 文档 | 浏览器打开 `http://localhost:8080/swagger-ui.html` | 展示空的 API 文档页 |
| DB 表存在 | `docker exec -it arxivradar-postgres psql -U arxivradar -d arxivradar -c "\\dt"` | 看到 `paper` 表和 `flyway_schema_history` |

## 目录结构

```
arxiv-radar-backend/
├── pom.xml                          # Maven 依赖
├── mvnw, mvnw.cmd                    # Maven wrapper
├── docker-compose.yml                # 本地依赖(Postgres + Redis)
├── .env.example                      # 环境变量模板
├── src/main/java/com/arxivradar/
│   ├── ArxivRadarApplication.java    # 主类
│   ├── config/                       # Cors / MP / OpenAPI 配置
│   └── mapper/                       # Phase 1 填充
└── src/main/resources/
    ├── application.yml               # 应用配置
    └── db/migration/                 # Flyway 迁移脚本
```

## 常用命令

```bash
# 编译 & 打包
./mvnw clean package

# 运行 (dev)
./mvnw spring-boot:run

# 只跑单元测试
./mvnw test

# 停止 Postgres + Redis
docker compose down

# 清空 Postgres + Redis 数据(慎用)
docker compose down -v
```

## 环境变量

见 `.env.example`。默认值都写在 `application.yml` 里,本地跑不用改。

## 路线图

- [x] **Phase 0**: 项目骨架、Docker 环境、Swagger、Flyway
- [ ] **Phase 1**: arXiv 数据管道(拉取、存 DB、定时更新)+ 论文分页/搜索 API
- [ ] **Phase 2**: LLM 代转(翻译、精读)+ Redis 幂等缓存 + 双重限流
- [ ] Phase 3(可选): 用户系统 / 真实 AI 评分 / 全文搜索
