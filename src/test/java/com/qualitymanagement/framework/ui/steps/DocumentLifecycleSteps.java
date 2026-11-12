package com.qualitymanagement.framework.ui.steps;

import com.qualitymanagement.framework.config.QmsConfig;
import com.qualitymanagement.framework.db.DatabaseIntegrityValidator;
import com.qualitymanagement.framework.ui.DocumentPage;
import com.qualitymanagement.framework.ui.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class DocumentLifecycleSteps {
    private DocumentPage documentPage;

    @Given("I open the configured document workspace")
    public void openWorkspace() {
        documentPage = new DocumentPage(DriverFactory.getDriver());
        documentPage.openWorkspace();
    }

    @When("I create the configured test document")
    public void createDocument() {
        documentPage.createConfiguredDocument();
    }

    @Then("its configured draft status is displayed")
    public void verifyDraftStatus() {
        assertStatus("ui.draftStatus");
    }

    @When("I submit the document for approval")
    public void submitForApproval() {
        documentPage.submitForApproval();
    }

    @Then("its configured pending-approval status is displayed")
    public void verifyPendingApprovalStatus() {
        assertStatus("ui.pendingApprovalStatus");
    }

    @When("I approve the document")
    public void approveDocument() {
        documentPage.approve();
    }

    @Then("its configured approved status is displayed")
    public void verifyApprovedStatus() {
        assertStatus("ui.approvedStatus");
    }

    @When("I open the document version history")
    public void openVersionHistory() {
        documentPage.openVersionHistory();
    }

    @Then("at least one document version is visible")
    public void verifyVersionHistory() {
        Assert.assertTrue(documentPage.hasVersionEntry(), "No version history entry was found");
    }

    @When("I open the document audit trail")
    public void openAuditTrail() {
        documentPage.openAuditTrail();
    }

    @Then("at least one audit event is visible")
    public void verifyAuditTrail() {
        Assert.assertTrue(documentPage.hasAuditEntry(), "No audit trail event was found");
    }

    @Then("configured database integrity validation succeeds when enabled")
    public void verifyDatabaseIntegrityAfterWorkflow() throws Exception {
        if (QmsConfig.isTrue("db.validateAfterWorkflow", false)) {
            DatabaseIntegrityValidator.validate(null);
        }
    }

    private void assertStatus(String configKey) {
        String expectedStatus = QmsConfig.require(configKey);
        documentPage.waitForStatus(expectedStatus);
        Assert.assertEquals(documentPage.status(), expectedStatus);
    }

    @After
    public void captureFailureAndCloseBrowser(io.cucumber.java.Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                DriverFactory.captureScreenshot();
            }
        } finally {
            DriverFactory.quitDriver();
        }
    }
}
