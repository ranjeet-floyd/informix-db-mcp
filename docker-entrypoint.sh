#!/bin/sh
set -e

# Check required environment variables
if [ -z "$INFORMIX_HOST" ]; then
  echo "Error: INFORMIX_HOST environment variable is not set"
  exit 1
fi

if [ -z "$INFORMIX_DATABASE" ]; then
  echo "Error: INFORMIX_DATABASE environment variable is not set"
  exit 1
fi

if [ -z "$INFORMIX_USERNAME" ]; then
  echo "Error: INFORMIX_USERNAME environment variable is not set"
  exit 1
fi

if [ -z "$INFORMIX_PASSWORD" ]; then
  echo "Error: INFORMIX_PASSWORD environment variable is not set"
  exit 1
fi

# Log startup information
echo "Starting Informix MCP Server..."
echo "Connecting to Informix at $INFORMIX_HOST:$INFORMIX_PORT"
echo "Database: $INFORMIX_DATABASE"
echo "Log level: $LOG_LEVEL"

# Start the Java application
exec java $JAVA_OPTS \
  -Dlogback.configurationFile=file:/app/logback.xml \
  -Dlog.dir=$LOG_DIR \
  -jar informix-mcp-server.jar
