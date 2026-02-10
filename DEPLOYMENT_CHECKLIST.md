# 🚀 部署检查清单

> 在推送代码到GitHub和部署到服务器前，请按此清单检查

---

## ✅ 推送到GitHub前检查

### 1. 检查敏感文件是否被排除

```bash
# 查看将要提交的文件
git status

# ❌ 如果看到以下文件，不要提交！
# - backend/src/main/resources/application-prod.yml
# - docs/ 目录下的任何文件

# ✅ 应该看到的文件
# - backend/src/main/resources/application.yml
# - backend/src/main/resources/application-dev.yml
# - backend/src/main/resources/application-prod.yml.template
```

### 2. 验证 .gitignore 是否生效

```bash
# 检查 application-prod.yml 是否被忽略
git check-ignore backend/src/main/resources/application-prod.yml

# 应该输出：backend/src/main/resources/application-prod.yml
# 如果没有输出，说明文件会被上传，需要检查 .gitignore
```

### 3. 检查配置文件内容

```bash
# 查看 application.yml 是否包含敏感信息
cat backend/src/main/resources/application.yml | grep -E "password|secret"

# ✅ 应该只看到占位符或开发环境的值
# ❌ 不应该看到真实的生产环境密码
```

---

## 📤 推送代码

```bash
# 1. 添加文件
git add .

# 2. 再次检查
git status

# 3. 提交
git commit -m "feat: 添加后台管理系统基础架构"

# 4. 推送
git push origin main
```

---

## 🖥️ 服务器部署检查

### 1. 拉取代码前备份配置

```bash
# 在服务器上执行
cd /www/my-blog

# 备份生产环境配置
cp backend/src/main/resources/application-prod.yml ~/application-prod.yml.backup

# 记录备份时间
echo "备份时间: $(date)" >> ~/application-prod.yml.backup
```

### 2. 拉取代码

```bash
# 拉取最新代码
git pull

# 如果提示冲突，执行：
# git stash
# git pull
# git stash pop
```

### 3. 恢复生产环境配置

```bash
# 检查配置文件是否存在
if [ ! -f "backend/src/main/resources/application-prod.yml" ]; then
    echo "⚠️ 生产环境配置不存在，从备份恢复"
    cp ~/application-prod.yml.backup backend/src/main/resources/application-prod.yml
fi

# 验证配置文件
cat backend/src/main/resources/application-prod.yml | grep -E "bloguser|MyBlog2026"
```

### 4. 编译和部署

```bash
cd backend

# 编译
mvn clean package -DskipTests

# 重启服务
sudo systemctl restart myblog-backend

# 查看状态
sudo systemctl status myblog-backend

# 查看日志
sudo journalctl -u myblog-backend -f
```

---

## 🔍 部署后验证

### 1. 检查应用是否启动

```bash
# 查看进程
ps aux | grep blog-backend

# 查看端口
netstat -tlnp | grep 8080

# 查看日志
tail -f /www/my-blog/backend/logs/backend.log
```

### 2. 检查配置是否生效

```bash
# 查看日志中的配置信息
grep "profiles are active" /www/my-blog/backend/logs/backend.log

# 应该看到：The following profiles are active: prod
```

### 3. 测试API

```bash
# 测试健康检查
curl http://localhost:8080/api/articles

# 测试登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 4. 检查Redis连接

```bash
redis-cli
> PING
> KEYS *
> exit
```

### 5. 检查MySQL连接

```bash
mysql -u bloguser -pMyBlog2026 blogdb -e "SHOW TABLES;"
```

---

## ⚠️ 常见错误处理

### 错误1：配置文件冲突

```
error: Your local changes to the following files would be overwritten by merge:
    backend/src/main/resources/application-prod.yml
```

**解决**:
```bash
# 暂存本地配置
git stash

# 拉取代码
git pull

# 恢复本地配置
git stash pop
```

---

### 错误2：Redis连接失败

```
Unable to connect to Redis
```

**解决**:
```bash
# 检查Redis是否启动
sudo systemctl status redis

# 启动Redis
sudo systemctl start redis

# 测试连接
redis-cli ping
```

---

### 错误3：MySQL连接失败

```
Communications link failure
```

**解决**:
```bash
# 检查MySQL是否启动
sudo systemctl status mysqld

# 启动MySQL
sudo systemctl start mysqld

# 测试连接
mysql -u bloguser -pMyBlog2026 -e "SELECT 1;"
```

---

### 错误4：端口被占用

```
Port 8080 is already in use
```

**解决**:
```bash
# 查找占用端口的进程
sudo lsof -i :8080

# 杀死进程
sudo kill -9 <PID>

# 或者重启服务
sudo systemctl restart myblog-backend
```

---

## 📋 快速命令参考

### 本地开发
```bash
# 启动开发环境
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 服务器部署
```bash
# 完整部署流程
cd /www/my-blog
cp backend/src/main/resources/application-prod.yml ~/backup/
git pull
cp ~/backup/application-prod.yml backend/src/main/resources/
cd backend
mvn clean package -DskipTests
sudo systemctl restart myblog-backend
sudo journalctl -u myblog-backend -f
```

### 查看日志
```bash
# 应用日志
tail -f /www/my-blog/backend/logs/backend.log

# systemd日志
sudo journalctl -u myblog-backend -f

# Nginx日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

## 🎯 安全检查清单

- [ ] application-prod.yml 不在 git status 中
- [ ] .gitignore 包含 application-prod.yml
- [ ] docs/ 目录不会被上传
- [ ] application.yml 不包含真实密码
- [ ] JWT密钥足够复杂（至少32位）
- [ ] 数据库密码足够强
- [ ] 服务器上的配置文件已备份
- [ ] Redis密码已设置（如果需要）
- [ ] 防火墙规则已配置
- [ ] SSL证书已配置（如果使用HTTPS）

---

**使用建议**: 
1. 每次部署前打印此清单
2. 逐项检查并打勾
3. 确保所有项目都通过后再部署

**最后更新**: 2026年2月10日
