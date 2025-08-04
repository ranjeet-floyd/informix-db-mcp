package com.example.informix.mcp.config;

import java.util.logging.Logger;

/**
 * Configuration class for the InformixMCPServer.
 * Centralizes all configuration settings and provides access to them.
 */
public class ServerConfig {
    private static final Logger LOGGER = Logger.getLogger(ServerConfig.class.getName());
    
    // Database configuration
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    private final String dbServerName;
    
    // Connection pool settings
    private final boolean connectionPoolEnabled;
    private final int connectionPoolMaxSize;
    private final int connectionPoolMinSize;
    private final int connectionPoolTimeout;
    
    // Server information
    private final String serverName;
    private final String serverVersion;
    private final String protocolVersion;
    
    // Query settings
    private final int queryTimeout;
    private final int maxRows;
    private final boolean readonlyEnforced;
    
    // Security settings
    private final boolean readonlyMode;
    
    /**
     * Creates a new ServerConfig instance by loading settings from the provided PropertyLoader.
     * 
     * @param propertyLoader the property loader to use
     */
    public ServerConfig(PropertyLoader propertyLoader) {
        // Load database configuration
        this.dbHost = propertyLoader.getProperty("informix.host", "localhost");
        this.dbPort = propertyLoader.getIntProperty("informix.port", 9088);
        this.dbName = propertyLoader.getProperty("informix.database", "testdb");
        this.dbUsername = propertyLoader.getProperty("informix.username", "informix");
        this.dbPassword = propertyLoader.getProperty("informix.password", "informix");
        this.dbServerName = propertyLoader.getProperty("informix.server", "ol_informix1210");
        
        // Load connection pool settings
        this.connectionPoolEnabled = propertyLoader.getBooleanProperty("db.connection.pool.enabled", false);
        this.connectionPoolMaxSize = propertyLoader.getIntProperty("db.connection.pool.max.size", 10);
        this.connectionPoolMinSize = propertyLoader.getIntProperty("db.connection.pool.min.size", 1);
        this.connectionPoolTimeout = propertyLoader.getIntProperty("db.connection.pool.timeout", 30000);
        
        // Load server information
        this.serverName = propertyLoader.getProperty("mcp.server.name", "informix-mcp-server");
        this.serverVersion = propertyLoader.getProperty("mcp.server.version", "1.0.0");
        this.protocolVersion = propertyLoader.getProperty("mcp.protocol.version", "2024-11-05");
        
        // Load query settings
        this.queryTimeout = propertyLoader.getIntProperty("query.timeout.seconds", 30);
        this.maxRows = propertyLoader.getIntProperty("query.max.rows", 10000);
        this.readonlyEnforced = propertyLoader.getBooleanProperty("query.readonly.enforced", false);
        
        // Load security settings
        this.readonlyMode = propertyLoader.getBooleanProperty("security.readonly.mode", false);
        
        LOGGER.info("Server configuration loaded successfully");
    }
    
    /**
     * Creates a new ServerConfig instance with default settings.
     * 
     * @return a new ServerConfig instance
     */
    public static ServerConfig createDefault() {
        return new ServerConfig(new PropertyLoader());
    }
    
    /**
     * Creates a new ServerConfig instance using a custom property file.
     * 
     * @param propertyFile the name of the property file to load
     * @return a new ServerConfig instance
     */
    public static ServerConfig fromPropertyFile(String propertyFile) {
        return new ServerConfig(new PropertyLoader(propertyFile));
    }
    
    /**
     * Creates a new ServerConfig instance using an existing PropertyLoader.
     * 
     * @param propertyLoader the PropertyLoader to use
     * @return a new ServerConfig instance
     */
    public static ServerConfig fromPropertyLoader(PropertyLoader propertyLoader) {
        return new ServerConfig(propertyLoader);
    }

    // Getters for all properties
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public String getDbServerName() { return dbServerName; }
    public boolean isConnectionPoolEnabled() { return connectionPoolEnabled; }
    public int getConnectionPoolMaxSize() { return connectionPoolMaxSize; }
    public int getConnectionPoolMinSize() { return connectionPoolMinSize; }
    public int getConnectionPoolTimeout() { return connectionPoolTimeout; }
    public String getServerName() { return serverName; }
    public String getServerVersion() { return serverVersion; }
    public String getProtocolVersion() { return protocolVersion; }
    public int getQueryTimeout() { return queryTimeout; }
    public int getMaxRows() { return maxRows; }
    public boolean isReadonlyEnforced() { return readonlyEnforced; }
    public boolean isReadonlyMode() { return readonlyMode; }
    
    /**
     * Returns a string representation of this configuration (excluding sensitive information).
     */
    @Override
    public String toString() {
        return "ServerConfig{" +
            "dbHost='" + dbHost + '\'' +
            ", dbPort=" + dbPort +
            ", dbName='" + dbName + '\'' +
            ", dbUsername='" + dbUsername + '\'' +
            ", dbServerName='" + dbServerName + '\'' +
            ", connectionPoolEnabled=" + connectionPoolEnabled +
            ", connectionPoolMaxSize=" + connectionPoolMaxSize +
            ", connectionPoolMinSize=" + connectionPoolMinSize +
            ", serverName='" + serverName + '\'' +
            ", serverVersion='" + serverVersion + '\'' +
            ", protocolVersion='" + protocolVersion + '\'' +
            ", queryTimeout=" + queryTimeout +
            ", maxRows=" + maxRows +
            ", readonlyEnforced=" + readonlyEnforced +
            ", readonlyMode=" + readonlyMode +
            '}';
    }
}
