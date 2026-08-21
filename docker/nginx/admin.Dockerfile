# 独立构建管理后台，避免后台 Vite 构建阻塞公开前台发布。
FROM node:20-alpine AS admin-builder

WORKDIR /build/admin
COPY admin-frontend/package.json admin-frontend/package-lock.json* ./
RUN npm ci --silent
COPY admin-frontend/ ./
RUN npm run build

FROM nginx:alpine

LABEL maintainer="zhulongqihan"
LABEL description="MyBlog Vue admin static service"

COPY --from=admin-builder /build/admin/dist/ /usr/share/nginx/html/
COPY docker/nginx/admin-nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 --start-period=5s \
  CMD wget -qO- http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
