#!/bin/bash

# 个人博客项目启动脚本 (Linux/Mac)
# 使用方法: ./start.sh

echo "========================================="
echo "  个人博客项目启动脚本"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Java
echo -n "检查 Java 环境... "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo -e "${GREEN}✓ Java $JAVA_VERSION${NC}"
    else
        echo -e "${RED}✗ Java 版本过低，需要 17+${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ 未安装 Java${NC}"
    exit 1
fi

# 检查 Node.js
echo -n "检查 Node.js 环境... "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 18 ]; then
        echo -e "${GREEN}✓ Node.js v$(node -v | cut -d'v' -f2)${NC}"
    else
        echo -e "${RED}✗ Node.js 版本过低，需要 18+${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ 未安装 Node.js${NC}"
    exit 1
fi

# 检查 Maven
echo -n "检查 Maven 环境... "
if command -v mvn &> /dev/null; then
    echo -e "${GREEN}✓ Maven $(mvn -v | head -n 1 | cut -d' ' -f3)${NC}"
else
    echo -e "${RED}✗ 未安装 Maven${NC}"
    exit 1
fi

echo ""
echo "========================================="
echo "  环境检查通过，开始启动项目..."
echo "========================================="
echo ""

# 创建日志目录
mkdir -p logs

# 启动后端
echo -e "${YELLOW}[1/3] 启动后端服务...${NC}"
cd backend
mvn spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "后端 PID: $BACKEND_PID"
cd ..

# 等待后端启动
echo -n "等待后端启动"
for i in {1..30}; do
    if curl -s http://localhost:8080/api/categories > /dev/null 2>&1; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 安装前端依赖（如果需要）
if [ ! -d "frontend/node_modules" ]; then
    echo -e "${YELLOW}[2/3] 安装前端依赖...${NC}"
    cd frontend
    npm install
    cd ..
fi

# 启动前端
echo -e "${YELLOW}[3/3] 启动前端服务...${NC}"
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "前端 PID: $FRONTEND_PID"
cd ..

# 保存 PID 到文件
echo "$BACKEND_PID" > logs/backend.pid
echo "$FRONTEND_PID" > logs/frontend.pid

echo ""
echo "========================================="
echo -e "  ${GREEN}项目启动成功！${NC}"
echo "========================================="
echo ""
echo "访问地址:"
echo "  前端: http://localhost:5173"
echo "  后端: http://localhost:8080"
echo "  H2控制台: http://localhost:8080/h2-console"
echo ""
echo "日志文件:"
echo "  后端: logs/backend.log"
echo "  前端: logs/frontend.log"
echo ""
echo "停止服务:"
echo "  运行: ./stop.sh"
echo ""
echo "========================================="
