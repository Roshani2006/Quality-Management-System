package com.qualitymanagement.framework.db;

import com.qualitymanagement.framework.config.QmsConfig;
import org.testng.Assert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class DatabaseIntegrityValidator {
    private DatabaseIntegrityValidator() {
    }

    public static void validate(String documentId) throws Exception {
        String query = QmsConfig.require("db.validationQuery").trim();
        if (!query.regionMatches(true, 0, "select", 0, "select".length()) || query.indexOf(';') >= 0) {
            throw new IllegalArgumentException("The SQL validation query must be one SELECT statement.");
        }
        int parameter = query.indexOf('?');
        if (parameter >= 0 && query.indexOf('?', parameter + 1) >= 0) {
            throw new IllegalArgumentException("The SQL validation query supports at most one document ID parameter.");
        }
        if (parameter >= 0 && (documentId == null || documentId.trim().isEmpty())) {
            throw new IllegalArgumentException("A document ID is required for the configured SQL query.");
        }

        String url = QmsConfig.require("db.jdbcUrl");
        String username = QmsConfig.get("db.username");
        String password = QmsConfig.get("db.password");
        if (username != null && password == null) {
            throw new IllegalStateException("QMS_DB_PASSWORD is required when QMS_DB_USERNAME is set.");
        }
        try (Connection connection = username == null
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, username, password)) {
            connection.setReadOnly(true);
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if (parameter >= 0) {
                    statement.setString(1, documentId);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    Assert.assertTrue(resultSet.next(), "Configured SQL integrity query returned no rows");
                    Assert.assertEquals(resultSet.getString(1), QmsConfig.require("db.expectedValue"),
                            "Configured SQL integrity value did not match");
                }
            }
        }
    }
}
