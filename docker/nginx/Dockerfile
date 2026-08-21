# ========== Stage 1: 构建 React 前台 ==========
FROM node:20-alpine AS frontend-builder

WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --silent
COPY frontend/ ./
RUN npm run build

# ========== Stage 2: Nginx 运行 ==========
FROM nginx:alpine

LABEL maintainer="zhulongqihan"
LABEL description="MyBlog Nginx - React frontend + Vue admin"

# 移除默认配置
RUN rm -rf /etc/nginx/conf.d/default.conf /usr/share/nginx/html/*

# 使用模板渲染 nginx 配置，运行时从环境变量注入 DashScope Key
ENV NGINX_ENVSUBST_OUTPUT_DIR=/etc/nginx
COPY docker/nginx/nginx.conf /etc/nginx/templates/nginx.conf.template

# 复制前台构建产物；管理后台由独立 admin 服务构建和提供。
COPY --from=frontend-builder /build/frontend/dist/ /usr/share/nginx/html/

# 创建上传文件目录
RUN mkdir -p /usr/share/nginx/uploads

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 --start-period=5s \
  CMD wget -qO- http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
