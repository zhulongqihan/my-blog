#!/bin/bash

# 生产环境部署脚本
# 功能：使用生产环境配置启动应用

echo "=========================================="
echo "开始部署生产环境"
echo "=========================================="

# 检查生产环境配置文件是否存在
if [ ! -f "src/main/resources/application-prod.yml" ]; then
    echo "❌ 错误：生产环境配置文件不存在！"
    echo "请先创建 src/main/resources/application-prod.yml"
    echo "可以参考 application-prod.yml.template 模板"
    exit 1
fi

echo "✅ 生产环境配置文件检查通过"

# 清理旧的构建产物
echo "🧹 清理旧的构建产物..."
mvn clean

# 编译打包（跳过测试）
echo "📦 开始编译打包..."
mvn package -DskipTests -Pprod

if [ $? -ne 0 ]; then
    echo "❌ 编译失败！"
    exit 1
fi

echo "✅ 编译成功！"

# 检查JAR文件是否生成
JAR_FILE="target/blog-backend-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR文件不存在：$JAR_FILE"
    exit 1
fi

echo "✅ JAR文件生成成功：$JAR_FILE"

# 启动应用（使用生产环境配置）
echo "🚀 启动应用（生产环境）..."
echo "使用配置：application-prod.yml"
echo "=========================================="

# 方式1：直接启动（前台运行）
# java -jar -Dspring.profiles.active=prod $JAR_FILE

# 方式2：后台启动（推荐）
nohup java -jar -Dspring.profiles.active=prod $JAR_FILE > logs/backend.log 2>&1 &

echo "✅ 应用已启动！"
echo "日志文件：logs/backend.log"
echo "查看日志：tail -f logs/backend.log"
echo "=========================================="
