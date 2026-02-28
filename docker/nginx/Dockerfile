# ========== Stage 1: 构建 React 前台 ==========
FROM node:18-alpine AS frontend-builder

WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --silent
COPY frontend/ ./
RUN npm run build

# ========== Stage 2: 构建 Vue 管理后台 ==========
FROM node:18-alpine AS admin-builder

WORKDIR /build/admin
COPY admin-frontend/package.json admin-frontend/package-lock.json* ./
RUN npm ci --silent
COPY admin-frontend/ ./
RUN npm run build

# ========== Stage 3: Nginx 运行 ==========
FROM nginx:alpine

LABEL maintainer="zhulongqihan"
LABEL description="MyBlog Nginx - React frontend + Vue admin"

# 移除默认配置
RUN rm -rf /etc/nginx/conf.d/default.conf /usr/share/nginx/html/*

# 复制自定义 nginx 配置
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf

# 复制前台构建产物
COPY --from=frontend-builder /build/frontend/dist/ /usr/share/nginx/html/

# 复制管理后台构建产物
COPY --from=admin-builder /build/admin/dist/ /usr/share/nginx/html/admin/

# 创建上传文件目录
RUN mkdir -p /usr/share/nginx/uploads

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 --start-period=5s \
  CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
