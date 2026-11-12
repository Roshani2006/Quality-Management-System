package com.qualitymanagement.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class QmsConfig {
    private static final Properties FILE_PROPERTIES = loadProperties();

    private QmsConfig() {
    }

    public static String get(String key) {
        String systemValue = System.getProperty("qms." + key);
        if (hasText(systemValue)) {
            return systemValue.trim();
        }

        String environmentValue = System.getenv(environmentName(key));
        if (hasText(environmentValue)) {
            return environmentValue.trim();
        }

        String fileValue = FILE_PROPERTIES.getProperty("qms." + key);
        return hasText(fileValue) ? fileValue.trim() : null;
    }

    public static String require(String key) {
        String value = get(key);
        if (!hasText(value)) {
            throw new IllegalStateException("Missing required configuration: qms." + key
                    + " (system property, environment variable, or config.properties)");
        }
        return value;
    }

    public static boolean hasAll(String... keys) {
        for (String key : keys) {
            if (!hasText(get(key))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isTrue(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private static String environmentName(String key) {
        return "QMS_" + key.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('.', '_').replace('-', '_').toUpperCase();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = QmsConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read config.properties", e);
        }
        return properties;
    }
}
