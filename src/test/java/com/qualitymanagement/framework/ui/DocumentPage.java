package com.qualitymanagement.framework.ui;

import com.qualitymanagement.framework.config.QmsConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

public class DocumentPage extends BasePage {
    @ConfiguredBy("ui.titleInputSelector")
    private WebElement titleInput;
    @ConfiguredBy("ui.fileInputSelector")
    private WebElement fileInput;
    @ConfiguredBy("ui.createButtonSelector")
    private WebElement createButton;
    @ConfiguredBy("ui.statusSelector")
    private WebElement status;
    @ConfiguredBy("ui.submitForApprovalSelector")
    private WebElement submitForApprovalButton;
    @ConfiguredBy("ui.approveSelector")
    private WebElement approveButton;
    @ConfiguredBy("ui.versionHistoryButtonSelector")
    private WebElement versionHistoryButton;
    @ConfiguredBy("ui.versionEntriesSelector")
    private WebElement versionEntries;
    @ConfiguredBy("ui.auditTrailButtonSelector")
    private WebElement auditTrailButton;
    @ConfiguredBy("ui.auditEntriesSelector")
    private WebElement auditEntries;

    public DocumentPage(WebDriver driver) {
        super(driver);
    }

    public void openWorkspace() {
        driver.get(QmsConfig.require("ui.workspaceUrl"));
    }

    public void createConfiguredDocument() {
        titleInput.sendKeys(QmsConfig.require("ui.documentTitle"));
        File document = new File(QmsConfig.require("ui.documentFile"));
        if (!document.isFile()) {
            throw new IllegalStateException("Configured UI document file does not exist: " + document);
        }
        fileInput.sendKeys(document.getAbsolutePath());
        click(createButton);
    }

    public String status() {
        return visibleText(status);
    }

    public void waitForStatus(String expectedStatus) {
        wait.until(ExpectedConditions.textToBePresentInElement(status, expectedStatus));
    }

    public void submitForApproval() {
        click(submitForApprovalButton);
    }

    public void approve() {
        click(approveButton);
    }

    public void openVersionHistory() {
        click(versionHistoryButton);
    }

    public boolean hasVersionEntry() {
        return wait.until(ExpectedConditions.visibilityOf(versionEntries)).isDisplayed();
    }

    public void openAuditTrail() {
        click(auditTrailButton);
    }

    public boolean hasAuditEntry() {
        return wait.until(ExpectedConditions.visibilityOf(auditEntries)).isDisplayed();
    }
}
