# Configuration System

This document describes the configuration system for the Informix MCP Server.

## Overview

The configuration system is designed to be modular, flexible, and easy to use. It consists of two main components:

1. **PropertyLoader**: Low-level class for loading properties from files
2. **ServerConfig**: High-level class that provides a structured configuration object

## PropertyLoader

The `PropertyLoader` class is responsible for loading properties from the `application.properties` file and providing methods to access them. It supports:

- Loading from custom property files
- Automatic environment variable overrides
- Type conversion (string, int, boolean, long, double, array)
- Default values

### Usage

```java
// Create a PropertyLoader with default application.properties
PropertyLoader loader = new PropertyLoader();

// Get properties with different types
String host = loader.getProperty("informix.host", "localhost");
int port = loader.getIntProperty("informix.port", 9088);
boolean enabled = loader.getBooleanProperty("feature.enabled", false);
String[] operations = loader.getArrayProperty("allowed.operations", ",", new String[]{"SELECT"});
```

## ServerConfig

The `ServerConfig` class provides a higher-level abstraction over the raw properties. It:

- Loads and validates all configuration settings at startup
- Provides strongly-typed accessors for all settings
- Groups related settings logically
- Implements sensible defaults (allows all SQL operations by default)
- Supports optional read-only mode for restricted environments

### Usage

```java
// Create a ServerConfig with defaults
ServerConfig config = ServerConfig.createDefault();

// Access configuration settings through type-safe getters
String dbHost = config.getDbHost();
int queryTimeout = config.getQueryTimeout();
boolean isReadOnly = config.isReadonlyMode();
```

## Configuration Precedence

Configuration values are resolved in the following order (highest precedence first):

1. Environment variables
2. Properties in application.properties
3. Default values

## Extension Points

The configuration system can be extended by:

1. Adding new properties to application.properties
2. Adding corresponding getters in ServerConfig
3. Using the new configuration values in the application
