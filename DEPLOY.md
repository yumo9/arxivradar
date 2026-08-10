# 部署到腾讯云服务器

**目标机器**:腾讯云 · 北京 · 2C2G · Ubuntu · `101.42.41.78`
**部署方式**:全 Docker,轩辕镜像加速
**结果**:后端 API 挂在 `http://101.42.41.78:8080`

## 前置约束(先了解,免得踩坑)

| 事项 | 状态 |
|---|---|
| arxiv.org 直连 | ⚠️ 北京机器偶尔超时,已内置抓取失败不崩的兜底 |
| api.anthropic.com 直连 | ❌ 完全不通,后端只用 DeepSeek/Qwen(Anthropic 只在前端"自带 key"模式可用) |
| HTTPS | ❌ 无域名走 HTTP,前端 surge.sh 因 mixed-content 无法调用;前端要本地跑联调 |
| Postgres/Redis 端口 | 只监听 127.0.0.1,不暴露公网 |

---

## 首次部署(约 15 分钟)

### 1. SSH 上服务器

```bash
ssh root@101.42.41.78
```

### 2. 装 Docker + Docker Compose

```bash
apt update && apt install -y curl git ca-certificates
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker
docker --version && docker compose version
```

### 3. 配置 Docker 走轩辕镜像加速(可选但推荐)

```bash
bash <(wget -qO- https://xuanyuan.cloud/docker.sh)
# 按提示配置好后 systemctl restart docker
```

如果不做这一步,`docker-compose.prod.yml` 里已经把 image 地址写成 `docker.xuanyuan.run/library/*`,拉取时会自动走轩辕。区别:配置了加速器后,你在服务器上 `docker pull nginx` 这样的命令也会加速。

### 4. 拉代码

```bash
mkdir -p /opt && cd /opt
# 用你的仓库地址替换。假设你把这个 backend 目录推到了 GitHub:
git clone https://github.com/<YOUR_USER>/arxiv-radar-backend.git
cd arxiv-radar-backend
```

如果暂时没推到 git,也可以用 scp 上传:

```bash
# 在本地 Mac 上执行
cd /Users/liangxueping.3/Desktop/resume
tar czf /tmp/backend.tar.gz --exclude='target' --exclude='.git' -C arxiv-radar-backend .
scp /tmp/backend.tar.gz root@101.42.41.78:/tmp/

# 服务器上
mkdir -p /opt/arxiv-radar-backend && cd /opt/arxiv-radar-backend
tar xzf /tmp/backend.tar.gz
```

### 5. 生成密码并写 .env.prod

```bash
cp .env.prod.example .env.prod
# 生成两条随机密码
DB_PWD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)
REDIS_PWD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)
sed -i "s|CHANGE_ME_16CHARS|$DB_PWD|" .env.prod
# 只替换第一个已经被换掉的,再替换 REDIS 那一行的占位
sed -i "s|CHANGE_ME_16CHARS|$REDIS_PWD|" .env.prod
cat .env.prod   # 确认无 CHANGE_ME 残留
```

### 6. 一键部署

```bash
./deploy.sh
```

**首次运行**要 3-5 分钟(下载基础镜像 + Maven 依赖 + 编译),后续更新只要 30 秒。

看到 `部署完成 🎉` 即成功。

### 7. 腾讯云安全组放行 8080

去腾讯云控制台:**云服务器 → 安全组 → 修改规则 → 入站规则**,添加:

| 协议 | 端口 | 来源 | 备注 |
|---|---|---|---|
| TCP | 8080 | 0.0.0.0/0 | ArxivRadar API |

**不要**放行:5432(Postgres)、6379(Redis)、其他 Docker 端口。

### 8. 从外部验证

在你的 Mac 上执行:

```bash
curl http://101.42.41.78:8080/actuator/health
# 期望: {"status":"UP"}

curl http://101.42.41.78:8080/api/v1/meta
# 期望: {"totalCount":50,"lastFetchedAt":null,"categories":[...]}

curl 'http://101.42.41.78:8080/api/v1/papers?size=3' | head -c 500
# 期望: {"content":[...],"totalElements":50,...}
```

Swagger UI:浏览器打开 `http://101.42.41.78:8080/swagger-ui.html`

---

## 日常运维

```bash
# 查看日志
docker compose -f docker-compose.prod.yml logs -f app

# 只看 app 最近 100 行
docker logs --tail 100 arxivradar-app

# 重启 app(不重启 DB/Redis,不丢数据)
docker compose -f docker-compose.prod.yml restart app

# 全部停止
docker compose -f docker-compose.prod.yml down

# 全部停止 + 删数据卷(会清空数据库,慎用)
docker compose -f docker-compose.prod.yml down -v

# 进 Postgres
docker exec -it arxivradar-postgres psql -U arxivradar arxivradar

# 进 Redis
docker exec -it arxivradar-redis redis-cli -a "$(grep REDIS_PASSWORD .env.prod | cut -d= -f2)"
```

---

## 更新代码

在服务器上执行:

```bash
cd /opt/arxiv-radar-backend
./deploy.sh
```

deploy.sh 会自动:git pull → 重新构建镜像 → 滚动重启 → 健康检查。

若不是 git 仓库,先用 scp 上传新代码再跑 deploy.sh。

---

## 前端如何调这个后端

因为你没域名 + 没 HTTPS:

- ❌ `https://arxiv-radar-demo.surge.sh` **不能**调 `http://101.42.41.78:8080`(浏览器混合内容拒绝)
- ✅ 本地 `npm run dev` 起前端,能调:因为本地也是 HTTP

**开发用**:改前端 `.env.local`:

```env
VITE_API_BASE=http://101.42.41.78:8080
```

**未来上线**:买域名 + 装 Nginx + Certbot 证书 → 前端切到 `https://api.你的域名/`。这一步等你想做时再说,不影响现在跑通。

---

## 卡壳排查

| 现象 | 检查 |
|---|---|
| `deploy.sh` 报 "app 60 秒内未就绪" | `docker logs arxivradar-app --tail 100` 看堆栈,常见:DB 连接失败(密码没同步)、arxiv 抓取阻塞(网络问题) |
| `curl` 从外网访问 8080 卡死 | 腾讯云安全组没开 8080;或者 `ufw` 挡了(`ufw status`,`ufw allow 8080`) |
| Postgres 起不来 | 数据卷权限:`docker compose -f docker-compose.prod.yml down -v` 再 up |
| 镜像拉不动 | 检查轩辕镜像配置是否生效:`docker info | grep -A5 "Registry Mirrors"` |
| `arxiv.org` 抓取超时 | 已内置兜底,不影响启动。手动重试:`curl -X POST http://localhost:8080/api/v1/admin/refresh`(需要在服务器上执行) |
| 磁盘满 | `docker system prune -af` 清老镜像 |

---

## 硬件容量说明

2C2G 机器上各组件粗算内存占用:

| 组件 | 内存 |
|---|---|
| App(JVM `-Xmx1024m`) | 峰值 1.2GB |
| Postgres | 200MB |
| Redis | 50MB |
| Ubuntu 系统 | ~300MB |
| **总计** | ~1.75GB |

内存吃紧但够用。**不要**在这台机器再跑其他重服务(比如 MySQL / Elasticsearch / Java IDE)。如果 OOM,可以把 JVM 调到 `-Xmx768m`:

```bash
# 在 docker-compose.prod.yml 的 app 服务里加:
environment:
  JAVA_OPTS: "-Xms256m -Xmx768m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
```
