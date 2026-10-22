# Supply a verified, locally present runtime image. No dependency downloads or build on host.
ARG RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}
COPY blog-backend.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
