# Informix MCP Server

A Model Context Protocol (MCP) server implementation for read-only access to Informix databases. This server provides a standardized interface for AI assistants and other MCP clients to query Informix databases safely.

## Features

- **Full SQL Operations Support**: Supports all SQL operations (SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, etc.)
- **Configurable Security**: Optional read-only mode for restricted environments
- **MCP Protocol Compliance**: Full implementation of the Model Context Protocol specification
- **Comprehensive Logging**: Structured logging with configurable levels and file rotation
- **Docker Support**: Ready-to-use Docker containers with docker-compose setup
- **Security First**: Non-root container execution and input validation
- **Production Ready**: Health checks, monitoring, and error handling
- **Modular Configuration**: Flexible property management with file and environment variable support

## Supported Operations

The server supports all SQL operations when readonly mode is disabled (default):

**Read Operations:**
- `SELECT` statements for data querying
- `WITH` clauses for complex queries (CTEs)
- `SHOW` commands for database introspection
- `DESCRIBE`/`DESC` commands for schema information
- `EXPLAIN` statements for query analysis

**Write Operations:**
- `INSERT` statements for adding data
- `UPDATE` statements for modifying data
- `DELETE` statements for removing data
- `CREATE` statements for creating database objects
- `ALTER` statements for modifying database objects
- `DROP` statements for removing database objects

**Note:** You can enable readonly mode via configuration to restrict operations to read-only queries.

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Access to an Informix database
- Informix JDBC driver (included in dependencies)

### Configuration

The server uses a modular configuration approach with two layers:

1. **PropertyLoader**: Loads raw properties from application.properties file
2. **ServerConfig**: Provides a structured configuration object with proper typing

#### Configuring with application.properties

Edit the `src/main/resources/application.properties` file to set your database connection and server parameters:

```properties
# Database Configuration
informix.host=localhost
informix.port=23302
informix.database=testdb
informix.username=dbuser
informix.password=dbpwd
informix.server=server

# Server Configuration
mcp.server.name=informix-mcp-server
mcp.server.version=1.0.0
mcp.protocol.version=2024-11-05

# Additional Settings
query.timeout.seconds=30
query.max.rows=10000
query.readonly.enforced=false
security.readonly.mode=false

# Connection Pool Settings (if enabled)
db.connection.pool.enabled=false
db.connection.pool.max.size=10
db.connection.pool.min.size=1
db.connection.pool.timeout=30000
```

#### Environment Variables Override

Environment variables automatically override the corresponding properties in application.properties. Set any of the following variables to override configuration:

```bash
export INFORMIX_HOST=<INFORMIX_HOST>
export INFORMIX_PORT=<INFORMIX_PORT>
export INFORMIX_DATABASE=<INFORMIX_DATABASE>
export INFORMIX_USERNAME=<INFORMIX_USERNAME>
export INFORMIX_PASSWORD=<INFORMIX_PASSWORD>
export INFORMIX_SERVER=<INFORMIX_SERVER>