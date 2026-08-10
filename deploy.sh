#!/usr/bin/env bash
# 一键部署脚本。用法(在服务器上,已 cd 到项目目录):
#     ./deploy.sh
# 首次运行前:
#     1) cp .env.prod.example .env.prod
#     2) 编辑 .env.prod 填入随机密码

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()   { echo -e "${GREEN}[deploy]${NC} $*"; }
warn()  { echo -e "${YELLOW}[warn]${NC}  $*"; }
error() { echo -e "${RED}[error]${NC} $*" >&2; }

# 1. 校验 .env.prod
if [[ ! -f .env.prod ]]; then
    error ".env.prod 不存在。请先复制模板:cp .env.prod.example .env.prod && 编辑填入密码"
    exit 1
fi

# 校验密码占位符已改
if grep -q "CHANGE_ME" .env.prod; then
    error ".env.prod 里还有 CHANGE_ME 占位符,请先改成真实密码"
    exit 1
fi

# 2. 拉取最新代码(如果是 git 仓库)
if [[ -d .git ]]; then
    log "拉取最新代码..."
    git pull --ff-only
fi

# 3. 停旧容器 & 构建 & 启动
log "构建镜像并启动..."
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build

# 4. 等健康检查
log "等待 app 服务健康..."
for i in {1..60}; do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' arxivradar-app 2>/dev/null || echo "starting")
    if [[ "$STATUS" == "healthy" ]]; then
        log "app 健康就绪 ✓"
        break
    fi
    if [[ $i -eq 60 ]]; then
        error "app 60 秒内未就绪,最后日志:"
        docker logs --tail 50 arxivradar-app
        exit 1
    fi
    sleep 2
done

# 5. 冒烟测试
log "冒烟测试 /actuator/health"
curl -fsS http://localhost:8080/actuator/health | grep -q '"UP"' && log "✓ health UP"

log "冒烟测试 /api/v1/meta"
curl -fsS http://localhost:8080/api/v1/meta | head -c 200 || true
echo

log "部署完成 🎉"
echo
echo "查看日志:    docker compose -f docker-compose.prod.yml logs -f app"
echo "停止所有:    docker compose -f docker-compose.prod.yml down"
echo "重启 app:    docker compose -f docker-compose.prod.yml restart app"
