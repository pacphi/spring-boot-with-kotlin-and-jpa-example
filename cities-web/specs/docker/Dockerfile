FROM bellsoft/liberica-openjdk-alpine:21
VOLUME /tmp
ARG JAR_FILE
ENV JAVA_OPTS=""

# Create non-root user
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Create necessary directories
RUN mkdir -p /home/app/.cache && chown -R app:app /home/app

# Copy application jar
ADD ${JAR_FILE} app.jar
RUN chown app:app app.jar

# Switch to non-root user
USER 1000:1000

ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar