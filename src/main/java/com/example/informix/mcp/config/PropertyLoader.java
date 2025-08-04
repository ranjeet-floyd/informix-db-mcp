package com.example.informix.mcp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for loading properties from application.properties file.
 * Uses plain Java Properties API without Spring dependencies.
 */
public class PropertyLoader {
    private static final Logger LOGGER = Logger.getLogger(PropertyLoader.class.getName());
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";
    
    private final Properties properties;

    /**
     * Loads properties from default application.properties file.
     */
    public PropertyLoader() {
        this(DEFAULT_PROPERTIES_FILE);
    }
    
    /**
     * Loads properties from a specified properties file.
     * 
     * @param propertiesFile the name of the properties file in the classpath
     */
    public PropertyLoader(String propertiesFile) {
        properties = loadPropertiesFromFile(propertiesFile);
    }
    
    /**
     * Loads properties from a file in the classpath.
     * 
     * @param propertiesFile the name of the properties file in the classpath
     * @return loaded Properties object
     */
    private Properties loadPropertiesFromFile(String propertiesFile) {
        Properties props = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
            if (inputStream != null) {
                props.load(inputStream);
                LOGGER.info("Successfully loaded " + propertiesFile);
            } else {
                LOGGER.warning("Could not find " + propertiesFile + " in classpath");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading " + propertiesFile, e);
        }
        return props;
    }

    /**
     * Gets a property value as String or returns default if not found.
     * Also checks environment variables for overrides.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return property value or default if not found
     */
    public String getProperty(String key, String defaultValue) {
        // First check environment variables (environment has priority)
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null) {
            return envValue;
        }
        
        // Then check properties file
        String value = properties.getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a property value as int or returns default if not found or cannot be parsed.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return property value as int or default if not found or cannot be parsed
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOGGER.warning("Could not parse integer property: " + key + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Gets a property value as boolean or returns default if not found.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return property value as boolean or default if not found
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, null);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    /**
     * Gets a property value as long or returns default if not found or cannot be parsed.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return property value as long or default if not found or cannot be parsed
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key, null);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                LOGGER.warning("Could not parse long property: " + key + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    /**
     * Gets a property value as double or returns default if not found or cannot be parsed.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return property value as double or default if not found or cannot be parsed
     */
    public double getDoubleProperty(String key, double defaultValue) {
        String value = getProperty(key, null);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                LOGGER.warning("Could not parse double property: " + key + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    /**
     * Gets a property value as a string array by splitting on a delimiter.
     * 
     * @param key the property key
     * @param delimiter the delimiter to split the value on
     * @param defaultValue the default value
     * @return property value as string array or default if not found
     */
    public String[] getArrayProperty(String key, String delimiter, String[] defaultValue) {
        String value = getProperty(key, null);
        if (value != null) {
            return value.split(delimiter);
        }
        return defaultValue;
    }

    /**
     * Returns the loaded Properties object.
     *
     * @return the loaded Properties object
     */
    public Properties getProperties() {
        return properties;
    }
}
