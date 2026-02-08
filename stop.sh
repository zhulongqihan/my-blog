#!/bin/bash

# 个人博客项目停止脚本 (Linux/Mac)
# 使用方法: ./stop.sh

echo "========================================="
echo "  停止个人博客项目"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 停止后端
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    echo -n "停止后端服务 (PID: $BACKEND_PID)... "
    if kill -0 $BACKEND_PID 2>/dev/null; then
        kill $BACKEND_PID
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${YELLOW}进程不存在${NC}"
    fi
    rm logs/backend.pid
else
    echo -e "${YELLOW}未找到后端 PID 文件${NC}"
fi

# 停止前端
if [ -f "logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    echo -n "停止前端服务 (PID: $FRONTEND_PID)... "
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        kill $FRONTEND_PID
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${YELLOW}进程不存在${NC}"
    fi
    rm logs/frontend.pid
else
    echo -e "${YELLOW}未找到前端 PID 文件${NC}"
fi

echo ""
echo -e "${GREEN}项目已停止${NC}"
echo ""
