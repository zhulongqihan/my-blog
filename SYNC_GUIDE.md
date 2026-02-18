# 📤 代码同步到GitHub指南

> **最后更新**: 2026年2月18日  
> **用途**: 将本地修改推送到GitHub仓库

---

## 🚀 快速同步命令

### 1️⃣ 查看修改了什么
```powershell
cd F:\myblog
git status
```

### 2️⃣ 添加所有修改
```powershell
git add .
```

### 3️⃣ 提交修改（附带说明）
```powershell
git commit -m "fix: 修复RedisConfig与Spring Boot 3.x兼容性问题"
```

### 4️⃣ 推送到GitHub
```powershell
git push origin main
```

---

## 📋 详细步骤说明

### 步骤1：进入项目目录
```powershell
cd F:\myblog
```

### 步骤2：查看修改状态
```powershell
git status
```

**预期输出示例：**
```
On branch main
Your branch is up to date with 'origin/main'.

Changes not staged for commit:
  modified:   backend/src/main/java/com/myblog/config/RedisConfig.java
  modified:   README.md
  modified:   docs/后台管理系统完整实施指南.md
```

### 步骤3：添加修改到暂存区

**添加所有修改：**
```powershell
git add .
```

**或者只添加特定文件：**
```powershell
git add backend/src/main/java/com/myblog/config/RedisConfig.java
git add README.md
git add docs/后台管理系统完整实施指南.md
```

### 步骤4：提交修改

```powershell
git commit -m "fix: 修复RedisConfig与Spring Boot 3.x兼容性问题"
```

**提交信息规范：**
- `feat:` - 新功能
- `fix:` - 修复bug
- `docs:` - 文档更新
- `style:` - 代码格式（不影响功能）
- `refactor:` - 代码重构
- `test:` - 测试相关
- `chore:` - 构建/工具相关

**示例：**
```powershell
# 新功能
git commit -m "feat: 添加文章管理接口"

# 修复bug
git commit -m "fix: 修复RedisConfig配置错误"

# 文档更新
git commit -m "docs: 更新部署指南"

# 多行提交信息
git commit -m "fix: 修复RedisConfig配置错误

- 将Jackson2JsonRedisSerializer改为GenericJackson2JsonRedisSerializer
- 删除过时的setObjectMapper调用
- 解决Spring Boot 3.x兼容性问题
- 修复服务器启动失败问题"
```

### 步骤5：推送到GitHub

```powershell
git push origin main
```

**预期输出：**
```
Enumerating objects: 11, done.
Counting objects: 100% (11/11), done.
Delta compression using up to 8 threads
Compressing objects: 100% (6/6), done.
Writing objects: 100% (6/6), 2.34 KiB | 2.34 MiB/s, done.
Total 6 (delta 4), reused 0 (delta 0), pack-reused 0
To https://github.com/zhulongqihan/my-blog.git
   abc1234..def5678  main -> main
```

---

## 🔧 常见问题

### ❌ 问题1：推送失败（需要登录）

**错误信息：**
```
remote: Support for password authentication was removed on August 13, 2021.
fatal: Authentication failed
```

**解决方法：**
使用GitHub Personal Access Token：
```powershell
# 设置远程仓库URL（使用Token）
git remote set-url origin https://YOUR_TOKEN@github.com/zhulongqihan/my-blog.git
```

### ❌ 问题2：推送被拒绝（远程有更新）

**错误信息：**
```
! [rejected]        main -> main (fetch first)
error: failed to push some refs
```

**解决方法：**
```powershell
# 先拉取远程更新
git pull origin main

# 如果有冲突，解决后再推送
git push origin main
```

### ❌ 问题3：有未提交的修改

**错误信息：**
```
error: Your local changes to the following files would be overwritten by merge
```

**解决方法：**
```powershell
# 方案1：先提交本地修改
git add .
git commit -m "保存本地修改"
git pull origin main

# 方案2：暂存本地修改
git stash
git pull origin main
git stash pop
```

---

## 📦 本次修改说明

### 修改文件列表：
1. ✅ `backend/src/main/java/com/myblog/config/RedisConfig.java`
   - 将 `Jackson2JsonRedisSerializer` 改为 `GenericJackson2JsonRedisSerializer`
   - 删除过时的 `setObjectMapper()` 调用
   - 解决Spring Boot 3.x兼容性问题

2. ✅ `README.md`
   - 更新最后修改日期为2026年2月18日
   - 版本号升级到v1.1.1
   - 添加2026-02-18开发日志

3. ✅ `docs/后台管理系统完整实施指南.md`
   - 添加RedisConfig错误排查步骤
   - 补充常见错误6的详细解决方案

### 推荐的提交信息：
```powershell
git commit -m "fix: 修复RedisConfig与Spring Boot 3.x兼容性问题

- 将Jackson2JsonRedisSerializer改为GenericJackson2JsonRedisSerializer
- 删除过时的setObjectMapper()调用
- 解决服务器启动失败问题(status=1/FAILURE)
- 更新README和部署文档
- 添加详细故障排查步骤"
```

---

## 🎯 完整同步流程

```powershell
# 1. 进入项目目录
cd F:\myblog

# 2. 查看修改状态
git status

# 3. 添加所有修改
git add .

# 4. 提交修改
git commit -m "fix: 修复RedisConfig与Spring Boot 3.x兼容性问题

- 将Jackson2JsonRedisSerializer改为GenericJackson2JsonRedisSerializer
- 删除过时的setObjectMapper()调用
- 解决服务器启动失败问题(status=1/FAILURE)
- 更新README和部署文档
- 添加详细故障排查步骤"

# 5. 推送到GitHub
git push origin main

# 6. 验证推送成功
# 访问 https://github.com/zhulongqihan/my-blog
# 查看最新提交记录
```

---

## ✅ 验证推送成功

1. 访问GitHub仓库：https://github.com/zhulongqihan/my-blog
2. 查看最新提交记录
3. 确认文件已更新
4. 查看提交时间和说明

---

## 🚀 服务器同步代码

推送到GitHub后，在服务器执行：

```bash
# SSH连接服务器
ssh root@118.31.221.81

# 进入项目目录
cd /www/my-blog

# 拉取最新代码
git pull origin main

# 重新编译
cd backend
mvn clean package -DskipTests

# 重启服务
sudo systemctl restart myblog-backend

# 查看日志
sudo journalctl -u myblog-backend -f --no-pager
```

---

> 💡 **提示**: 每次修改代码后，都应该按照这个流程同步到GitHub，保持代码版本一致性。
