# Use Walmart Azul Zulu OpenJDK distroless image
FROM openjdk:17

# Set working directory
WORKDIR /app

# Create non-root user for security first
RUN groupadd -r mcpuser && useradd -r -g mcpuser mcpuser

# Create logs directory
RUN mkdir -p /app/logs

# Copy the executable JAR file, config and scripts
ARG JAR_FILE=target/informix-mcp-server-1.0.0-jar-with-dependencies.jar
COPY ${JAR_FILE} informix-mcp-server.jar
COPY src/main/resources/logback.xml /app/logback.xml
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# Set proper permissions
RUN chown -R mcpuser:mcpuser /app

# Install required packages
# RUN apt-get update && apt-get install -y \
#     curl \
#     procps \
#     && rm -rf /var/lib/apt/lists/*

# Make entrypoint script executable
RUN chmod +x /app/docker-entrypoint.sh

# Switch to non-root user
USER mcpuser

# Set default environment variables
ENV LOG_LEVEL=INFO
ENV LOG_DIR=/app/logs
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV INFORMIX_PORT=9088
# The following variables must be provided at runtime:
# - INFORMIX_HOST
# - INFORMIX_DATABASE
# - INFORMIX_USERNAME
# - INFORMIX_PASSWORD

# Health check - check if Java process is running
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD pgrep -f "java.*informix-mcp-server" > /dev/null || exit 1

# No need to expose port as MCP server uses stdin/stdout communication

# Set the entry point
ENTRYPOINT ["/app/docker-entrypoint.sh"]

# Labels for metadata
LABEL maintainer="your-email@example.com"
LABEL version="1.0.0"
LABEL description="Informix MCP Server - Read-only database operations via Model Context Protocol"
LABEL org.opencontainers.image.title="Informix MCP Server"
LABEL org.opencontainers.image.description="Model Context Protocol server for Informix database"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="Example Corp"