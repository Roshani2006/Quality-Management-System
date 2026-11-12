package com.qualitymanagement.framework.ui;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, 15);
        PageFactory.initElements(new ConfiguredElementLocatorFactory(driver), this);
    }

    protected void click(org.openqa.selenium.WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    protected String visibleText(org.openqa.selenium.WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element)).getText().trim();
    }
}
