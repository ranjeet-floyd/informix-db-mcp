package com.example.informix.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Informix Database MCP Server
 * 
 * This server implements the Model Context Protocol (MCP) specification
 * to provide database operations for Informix databases.
 */
public class InformixMCPServer {
    
    private static final Logger LOGGER = Logger.getLogger(InformixMCPServer.class.getName());
    
    private String protocolVersion = "2024-11-05";
    private String serverName = "informix-mcp-server";
    private String serverVersion = "1.0.0";
    private int queryTimeout = 30;
    private int maxRows = 10000;
    private boolean readonlyEnforced = false;
    private boolean readonlyMode = false;
    
    private final ObjectMapper objectMapper;
    private final Map<String, Connection> connections;
    private final DatabaseConfig config;
    private final BufferedReader stdin;
    private final PrintWriter stdout;
    
    public InformixMCPServer(DatabaseConfig config) {
        this.objectMapper = new ObjectMapper();
        this.connections = new ConcurrentHashMap<>();
        this.config = config;
        this.stdin = new BufferedReader(new InputStreamReader(System.in));
        this.stdout = new PrintWriter(System.out, true);
    }
    
    /**
     * Database configuration class
     */
    public static class DatabaseConfig {
        private final String url;
        private final String username;
        private final String password;
        private final String driverClass;
        
        public DatabaseConfig(String host, int port, String database, 
                            String username, String password) {
            String serverName = System.getenv("INFORMIX_SERVER");
            if (serverName == null) {
                serverName = "default"; // default value
            }
            this.url = String.format("jdbc:informix-sqli://%s:%d/%s:INFORMIXSERVER=%s", 
                                   host, port, database, serverName);
            this.username = username;
            this.password = password;
            this.driverClass = "com.informix.jdbc.IfxDriver";
        }
        
        public DatabaseConfig(String host, int port, String database, 
                            String username, String password, String serverName) {
            this.url = String.format("jdbc:informix-sqli://%s:%d/%s:INFORMIXSERVER=%s", 
                                   host, port, database, serverName);
            this.username = username;
            this.password = password;
            this.driverClass = "com.informix.jdbc.IfxDriver";
        }
        
        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getDriverClass() { return driverClass; }
    }
    
    /**
     * Main server loop
     */
    public void start() {
        try {
            // Load Informix JDBC driver
            Class.forName(config.getDriverClass());
            LOGGER.info("Informix MCP Server started");
            
            String line;
            while ((line = stdin.readLine()) != null) {
                try {
                    JsonNode request = objectMapper.readTree(line);
                    JsonNode response = handleRequest(request);
                    stdout.println(objectMapper.writeValueAsString(response));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing request: " + e.getMessage(), e);
                    JsonNode errorResponse = createErrorResponse(-1, "Internal server error", 
                        "Failed to process request: " + e.getMessage());
                    stdout.println(objectMapper.writeValueAsString(errorResponse));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error in server", e);
        } finally {
            cleanup();
        }
    }
    
    /**
     * Handle incoming MCP requests
     */
    private JsonNode handleRequest(JsonNode request) {
        String method = request.get("method").asText();
        JsonNode params = request.get("params");
        int id = request.has("id") ? request.get("id").asInt() : -1;
        
        try {
            switch (method) {
                case "initialize":
                    return handleInitialize(id, params);
                case "tools/list":
                    return handleToolsList(id);
                case "tools/call":
                    return handleToolsCall(id, params);
                case "resources/list":
                    return handleResourcesList(id);
                case "resources/read":
                    return handleResourcesRead(id, params);
                default:
                    return createErrorResponse(id, "Method not found", "Unknown method: " + method);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error handling request: " + method, e);
            return createErrorResponse(id, "Internal error", e.getMessage());
        }
    }
    
    /**
     * Handle initialize request
     */
    private JsonNode handleInitialize(int id, JsonNode params) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", protocolVersion);
        
        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        result.set("serverInfo", serverInfo);
        
        ObjectNode capabilities = objectMapper.createObjectNode();
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        result.set("capabilities", capabilities);
        
        return createSuccessResponse(id, result);
    }
    
    /**
     * Handle tools/list request
     */
    private JsonNode handleToolsList(int id) {
        ArrayNode tools = objectMapper.createArrayNode();
        
        // Query tool
        ObjectNode queryTool = objectMapper.createObjectNode();
        queryTool.put("name", "query");
        queryTool.put("description", "Execute SQL queries on Informix database (supports all SQL operations)");
        
        ObjectNode querySchema = objectMapper.createObjectNode();
        querySchema.put("type", "object");
        ObjectNode queryProps = objectMapper.createObjectNode();
        
        ObjectNode sqlProp = objectMapper.createObjectNode();
        sqlProp.put("type", "string");
        sqlProp.put("description", "SQL query to execute");
        queryProps.set("sql", sqlProp);
        
        ObjectNode readOnlyProp = objectMapper.createObjectNode();
        readOnlyProp.put("type", "boolean");
        readOnlyProp.put("description", "Whether query is read-only (default: false, allows all operations)");
        queryProps.set("readonly", readOnlyProp);
        
        querySchema.set("properties", queryProps);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("sql");
        querySchema.set("required", required);
        
        queryTool.set("inputSchema", querySchema);
        tools.add(queryTool);
        
        // Schema tool
        ObjectNode schemaTool = objectMapper.createObjectNode();
        schemaTool.put("name", "describe_table");
        schemaTool.put("description", "Get table structure and metadata");
        
        ObjectNode schemaToolSchema = objectMapper.createObjectNode();
        schemaToolSchema.put("type", "object");
        ObjectNode schemaProps = objectMapper.createObjectNode();
        
        ObjectNode tableProp = objectMapper.createObjectNode();
        tableProp.put("type", "string");
        tableProp.put("description", "Table name to describe");
        schemaProps.set("table_name", tableProp);
        
        schemaToolSchema.set("properties", schemaProps);
        ArrayNode schemaRequired = objectMapper.createArrayNode();
        schemaRequired.add("table_name");
        schemaToolSchema.set("required", schemaRequired);
        
        schemaTool.set("inputSchema", schemaToolSchema);
        tools.add(schemaTool);
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", tools);
        
        return createSuccessResponse(id, result);
    }
    
    /**
     * Handle tools/call request
     */
    private JsonNode handleToolsCall(int id, JsonNode params) throws SQLException {
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");
        
        switch (toolName) {
            case "query":
                return handleQueryTool(id, arguments);
            case "describe_table":
                return handleDescribeTableTool(id, arguments);
            default:
                return createErrorResponse(id, "Unknown tool", "Tool not found: " + toolName);
        }
    }
    
    /**
     * Handle query tool execution
     */
    private JsonNode handleQueryTool(int id, JsonNode arguments) throws SQLException {
        String sql = arguments.get("sql").asText();
        boolean readonly = arguments.has("readonly") ? arguments.get("readonly").asBoolean() : false;
        
        // Additional SQL injection check
        if (containsSqlInjection(sql)) {
            return createErrorResponse(id, "Security violation", 
                "Possible SQL injection detected");
        }
        
        Connection conn = getConnection();
        
        try {
            if (readonly && !isReadOnlyQuery(sql)) {
                return createErrorResponse(id, "Write operation not allowed", 
                    "Query appears to modify data but readonly=true");
            }
            
            ArrayNode results = objectMapper.createArrayNode();
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // Get column names
                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                }
                
                // Process results
                while (rs.next()) {
                    ObjectNode row = objectMapper.createObjectNode();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = columnNames.get(i - 1);
                        Object value = rs.getObject(i);
                        if (value != null) {
                            row.put(columnName, value.toString());
                        } else {
                            row.putNull(columnName);
                        }
                    }
                    results.add(row);
                }
            }
            
            ObjectNode result = objectMapper.createObjectNode();
            result.set("results", results);
            result.put("rowCount", results.size());
            
            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            if (results.size() > 0) {
                try {
                    textContent.put("text", String.format("Query executed successfully. Rows returned: %d\n\nResults:\n%s", 
                        results.size(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results)));
                } catch (Exception e) {
                    textContent.put("text", String.format("Query executed successfully. Rows returned: %d\n\nResults: %s", 
                        results.size(), results.toString()));
                }
            } else {
                textContent.put("text", "Query executed successfully. Rows returned: 0");
            }
            content.add(textContent);
            
            ObjectNode toolResult = objectMapper.createObjectNode();
            toolResult.set("content", content);
            toolResult.put("isError", false);
            
            return createSuccessResponse(id, toolResult);
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "SQL execution error", e);
            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", "SQL Error: " + e.getMessage());
            content.add(textContent);
            
            ObjectNode toolResult = objectMapper.createObjectNode();
            toolResult.set("content", content);
            toolResult.put("isError", true);
            
            return createSuccessResponse(id, toolResult);
        }
    }
    
    /**
     * Check if a SQL query is read-only
     * @param sql The SQL statement to check
     * @return true if the query is read-only, false otherwise
     */
    private boolean isReadOnlyQuery(String sql) {
        // If readonly mode is disabled globally, allow all operations
        if (!readonlyMode) {
            return false; // Allow all operations
        }
        
        // If readonly mode is enabled, only allow specific read-only operations
        String sqlUpper = sql.trim().toUpperCase();
        
        return sqlUpper.startsWith("SELECT") || 
               sqlUpper.startsWith("WITH") ||
               sqlUpper.startsWith("SHOW") ||
               sqlUpper.startsWith("DESCRIBE") ||
               sqlUpper.startsWith("DESC") ||
               sqlUpper.startsWith("EXPLAIN");
    }
    
    /**
     * Basic check for potential SQL injection patterns
     * @param sql SQL query to check
     * @return true if potential SQL injection is detected
     */
    private boolean containsSqlInjection(String sql) {
        String sqlLower = sql.toLowerCase();
        
        // Check for common SQL injection patterns
        return sqlLower.contains("--") ||
               sqlLower.contains(";") ||
               sqlLower.matches(".*\\s+or\\s+['\"\\d].*") ||
               sqlLower.contains("union select") ||
               sqlLower.contains("drop table") ||
               sqlLower.contains("drop database") ||
               sqlLower.contains("truncate table") ||
               sqlLower.contains("delete from") ||
               sqlLower.contains("insert into") ||
               sqlLower.contains("xp_cmdshell");
    }

    /**
     * Handle describe table tool
     */
    private JsonNode handleDescribeTableTool(int id, JsonNode arguments) throws SQLException {
        String tableName = arguments.get("table_name").asText();
        Connection conn = getConnection();
        
        try {
            StringBuilder description = new StringBuilder();
            description.append("Table: ").append(tableName).append("\n\n");
            
            // Get table structure
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null)) {
                description.append("Columns:\n");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dataType = columns.getString("TYPE_NAME");
                    int columnSize = columns.getInt("COLUMN_SIZE");
                    String nullable = columns.getString("IS_NULLABLE");
                    String defaultValue = columns.getString("COLUMN_DEF");
                    
                    description.append(String.format("  %s: %s(%d) %s%s\n",
                        columnName, dataType, columnSize,
                        "YES".equals(nullable) ? "NULL" : "NOT NULL",
                        defaultValue != null ? " DEFAULT " + defaultValue : ""));
                }
            }
            
            // Get primary keys
            try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName.toUpperCase())) {
                List<String> pkColumns = new ArrayList<>();
                while (primaryKeys.next()) {
                    pkColumns.add(primaryKeys.getString("COLUMN_NAME"));
                }
                if (!pkColumns.isEmpty()) {
                    description.append("\nPrimary Key: ").append(String.join(", ", pkColumns)).append("\n");
                }
            }
            
            // Get indexes
            try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName.toUpperCase(), false, false)) {
                Map<String, List<String>> indexMap = new HashMap<>();
                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    String columnName = indexes.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) {
                        indexMap.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
                    }
                }
                if (!indexMap.isEmpty()) {
                    description.append("\nIndexes:\n");
                    for (Map.Entry<String, List<String>> entry : indexMap.entrySet()) {
                        description.append(String.format("  %s: %s\n", 
                            entry.getKey(), String.join(", ", entry.getValue())));
                    }
                }
            }
            
            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", description.toString());
            content.add(textContent);
            
            ObjectNode toolResult = objectMapper.createObjectNode();
            toolResult.set("content", content);
            toolResult.put("isError", false);
            
            return createSuccessResponse(id, toolResult);
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error describing table", e);
            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", "Error describing table: " + e.getMessage());
            content.add(textContent);
            
            ObjectNode toolResult = objectMapper.createObjectNode();
            toolResult.set("content", content);
            toolResult.put("isError", true);
            
            return createSuccessResponse(id, toolResult);
        }
    }
    
    /**
     * Handle resources/list request
     */
    private JsonNode handleResourcesList(int id) {
        ArrayNode resources = objectMapper.createArrayNode();
        
        ObjectNode tablesResource = objectMapper.createObjectNode();
        tablesResource.put("uri", "informix://tables");
        tablesResource.put("name", "Database Tables");
        tablesResource.put("description", "List of all tables in the database");
        tablesResource.put("mimeType", "application/json");
        resources.add(tablesResource);
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("resources", resources);
        
        return createSuccessResponse(id, result);
    }
    
    /**
     * Handle resources/read request
     * @throws JsonProcessingException 
     */
    private JsonNode handleResourcesRead(int id, JsonNode params) throws SQLException, JsonProcessingException {
        String uri = params.get("uri").asText();
        
        if ("informix://tables".equals(uri)) {
            Connection conn = getConnection();
            ArrayNode tables = objectMapper.createArrayNode();
            
            try {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        ObjectNode table = objectMapper.createObjectNode();
                        table.put("name", rs.getString("TABLE_NAME"));
                        table.put("schema", rs.getString("TABLE_SCHEM"));
                        table.put("type", rs.getString("TABLE_TYPE"));
                        tables.add(table);
                    }
                }
            } catch (SQLException e) {
                return createErrorResponse(id, "Database error", e.getMessage());
            }
            
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            content.put("uri", uri);
            content.put("mimeType", "application/json");
            content.put("text", objectMapper.writeValueAsString(tables));
            contents.add(content);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.set("contents", contents);
            
            return createSuccessResponse(id, result);
        }
        
        return createErrorResponse(id, "Resource not found", "Unknown resource: " + uri);
    }
    
    /**
     * Get database connection (creates if doesn't exist)
     */
    private Connection getConnection() throws SQLException {
        String key = "default";
        Connection conn = connections.get(key);
        
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(
                config.getUrl(), 
                config.getUsername(), 
                config.getPassword()
            );
            connections.put(key, conn);
            LOGGER.info("Created new database connection");
        }
        
        return conn;
    }
    
    /**
     * Create success response
     */
    private JsonNode createSuccessResponse(int id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.set("result", result);
        return response;
    }
    
    /**
     * Create error response
     */
    private JsonNode createErrorResponse(int id, String message, String data) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", -32000);
        error.put("message", message);
        if (data != null) {
            error.put("data", data);
        }
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.set("error", error);
        return response;
    }
    
    /**
     * Cleanup resources
     */
    private void cleanup() {
        for (Connection conn : connections.values()) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        connections.clear();
    }
    
    // Getter and setter methods
    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getServerVersion() { return serverVersion; }
    public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
    
    public int getQueryTimeout() { return queryTimeout; }
    public void setQueryTimeout(int queryTimeout) { this.queryTimeout = queryTimeout; }
    
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
    
    public boolean isReadonlyEnforced() { return readonlyEnforced; }
    public void setReadonlyEnforced(boolean readonlyEnforced) { this.readonlyEnforced = readonlyEnforced; }
    
    public boolean isReadonlyMode() { return readonlyMode; }
    public void setReadonlyMode(boolean readonlyMode) { this.readonlyMode = readonlyMode; }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        // Load server configuration from properties file
        com.example.informix.mcp.config.ServerConfig serverConfig = 
            com.example.informix.mcp.config.ServerConfig.createDefault();
        
        // Create database configuration
        DatabaseConfig dbConfig = new DatabaseConfig(
            serverConfig.getDbHost(),
            serverConfig.getDbPort(),
            serverConfig.getDbName(),
            serverConfig.getDbUsername(),
            serverConfig.getDbPassword(),
            serverConfig.getDbServerName()
        );
        
        // Create and configure server
        InformixMCPServer server = new InformixMCPServer(dbConfig);
        
        // Configure server settings from the ServerConfig
        server.setServerName(serverConfig.getServerName());
        server.setServerVersion(serverConfig.getServerVersion());
        server.setProtocolVersion(serverConfig.getProtocolVersion());
        server.setQueryTimeout(serverConfig.getQueryTimeout());
        server.setMaxRows(serverConfig.getMaxRows());
        server.setReadonlyEnforced(serverConfig.isReadonlyEnforced());
        server.setReadonlyMode(serverConfig.isReadonlyMode());
        
        // Start the server
        server.start();
    }
}