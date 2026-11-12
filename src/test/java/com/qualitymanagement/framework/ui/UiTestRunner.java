package com.qualitymanagement.framework.ui;

import com.qualitymanagement.framework.config.QmsConfig;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.qualitymanagement.framework.ui.steps",
        plugin = {"pretty", "json:target/cucumber-report.json"}
)
public class UiTestRunner extends AbstractTestNGCucumberTests {
    private static final String[] REQUIRED_UI_CONFIGURATION = {
            "ui.workspaceUrl", "ui.documentTitle", "ui.documentFile",
            "ui.titleInputSelector", "ui.fileInputSelector", "ui.createButtonSelector",
            "ui.statusSelector", "ui.submitForApprovalSelector", "ui.approveSelector",
            "ui.versionHistoryButtonSelector", "ui.versionEntriesSelector",
            "ui.auditTrailButtonSelector", "ui.auditEntriesSelector",
            "ui.draftStatus", "ui.pendingApprovalStatus", "ui.approvedStatus"
    };

    @BeforeClass
    public void requireUiConfiguration() {
        if (!QmsConfig.hasAll(REQUIRED_UI_CONFIGURATION)) {
            throw new SkipException("UI scenarios skipped: configure QMS_UI_* values for the target application.");
        }
        if (QmsConfig.isTrue("db.validateAfterWorkflow", false)
                && !QmsConfig.hasAll("db.jdbcUrl", "db.validationQuery", "db.expectedValue")) {
            throw new IllegalStateException("SQL validation is enabled but its JDBC URL, query, or expected value is missing.");
        }
    }

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
