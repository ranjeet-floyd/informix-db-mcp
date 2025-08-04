# VS Code MCP Configuration for Informix Database

## Prerequisites
1. Docker and Docker Compose installed and running
2. VS Code with MCP extension installed
3. Network access to the Informix database server

## Setup Instructions

### 1. Start the MCP Server
```bash
cd <path>/informix-db-mcp
docker-compose up -d
```

### 2. Verify the Server is Running
```bash
docker-compose ps
docker-compose logs --tail 10
```

### 3. Configure VS Code MCP Settings

Add the following to your VS Code `settings.json` file:

```json
{
  "mcp.servers": {
    "informix": {
      "command": "docker",
      "args": [
        "exec",
        "-i",
        "informix-mcp-server",
        "java",
        "-jar",
        "informix-mcp-server.jar"
      ]
    }
  }
}
```

### 4. Test the Connection

You can test the MCP server manually with:

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}' | docker exec -i informix-mcp-server java -jar informix-mcp-server.jar
```

### 5. Available Tools

The MCP server provides these tools:

1. **query** - Execute read-only SQL queries
   - Parameters: `sql` (string), `readonly` (boolean, default: true)
   
2. **describe_table** - Get table structure and metadata
   - Parameters: `table_name` (string)

### 6. Available Resources

- **informix://tables** - List all tables in the database

## Usage Examples

### Query Tool
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "query",
    "arguments": {
      "sql": "SELECT * FROM your_table LIMIT 10",
      "readonly": true
    }
  }
}
```

### Describe Table Tool
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "describe_table",
    "arguments": {
      "table_name": "your_table_name"
    }
  }
}
```

## Security Features

- ✅ Read-only enforcement
- ✅ SQL injection protection
- ✅ Environment variable configuration
- ✅ Non-root container execution
- ✅ Input validation

## Troubleshooting

### Container not starting
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Database connection issues
Check the logs:
```bash
docker-compose logs -f
```

Common issues:
- Incorrect INFORMIX_SERVER name
- Network connectivity
- Database credentials

### Stop the server
```bash
docker-compose down
```

## Environment Variables

The following environment variables are configured in `docker-compose.yml`:

- `INFORMIX_HOST` - Database host
- `INFORMIX_PORT` - Database port (default: 23302)
- `INFORMIX_DATABASE` - Database name
- `INFORMIX_USERNAME` - Database username
- `INFORMIX_PASSWORD` - Database password
- `INFORMIX_SERVER` - Informix server name
- `LOG_LEVEL` - Logging level (default: INFO)
- `JAVA_OPTS` - JVM options (default: -Xms256m -Xmx512m)
