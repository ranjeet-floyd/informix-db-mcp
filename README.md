# Informix MCP Server

A Model Context Protocol (MCP) server implementation for read-only access to Informix databases. This server provides a standardized interface for AI assistants and other MCP clients to query Informix databases safely.

## Features

- **Read-Only Operations**: Strictly enforced read-only access with connection-level and application-level protection
- **MCP Protocol Compliance**: Full implementation of the Model Context Protocol specification
- **Comprehensive Logging**: Structured logging with configurable levels and file rotation
- **Docker Support**: Ready-to-use Docker containers with docker-compose setup
- **Security First**: Non-root container execution and input validation
- **Production Ready**: Health checks, monitoring, and error handling

## Supported Operations

- `SELECT` statements for data querying
- `WITH` clauses for complex queries (CTEs)
- `SHOW` commands for database introspection
- `DESCRIBE`/`DESC` commands for schema information
- `EXPLAIN` statements for query analysis

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Access to an Informix database
- Informix JDBC driver (included in dependencies)

### Environment Variables

Set the following required environment variables:

```bash
export INFORMIX_HOST=<INFORMIX_HOST>
export INFORMIX_PORT=<INFORMIX_PORT>
export INFORMIX_DATABASE=<INFORMIX_DATABASE>
export INFORMIX_USERNAME=<INFORMIX_USERNAME>
export INFORMIX_PASSWORD=<INFORMIX_PASSWORD>
export INFORMIX_SERVER=<INFORMIX_SERVER>