ARG RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}
RUN rm -rf /usr/share/nginx/html/*
COPY dist/ /usr/share/nginx/html/
