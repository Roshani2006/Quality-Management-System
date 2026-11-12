package com.qualitymanagement.framework.ui;

import com.qualitymanagement.framework.config.QmsConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class DriverFactory {
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<WebDriver>();
    private static final ThreadLocal<File> LAST_SCREENSHOT = new ThreadLocal<File>();

    private DriverFactory() {
    }

    public static WebDriver getDriver() {
        if (DRIVER.get() == null) {
            String driverPath = QmsConfig.get("selenium.driverPath");
            if (driverPath != null) {
                String browser = QmsConfig.get("browser");
                System.setProperty("firefox".equalsIgnoreCase(browser)
                        ? "webdriver.gecko.driver" : "webdriver.chrome.driver", driverPath);
            }

            String browser = QmsConfig.get("browser");
            if ("firefox".equalsIgnoreCase(browser)) {
                FirefoxOptions options = new FirefoxOptions();
                if (QmsConfig.isTrue("browser.headless", true)) {
                    options.addArguments("-headless");
                }
                DRIVER.set(new FirefoxDriver(options));
            } else if (browser == null || "chrome".equalsIgnoreCase(browser)) {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                if (QmsConfig.isTrue("browser.headless", true)) {
                    options.addArguments("--headless", "--window-size=1440,1000");
                }
                DRIVER.set(new ChromeDriver(options));
            } else {
                throw new IllegalArgumentException("Unsupported qms.browser: " + browser);
            }
        }
        return DRIVER.get();
    }

    public static File captureScreenshot() {
        WebDriver driver = DRIVER.get();
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        File directory = new File("target/screenshots");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create screenshot directory: " + directory);
        }
        File destination = new File(directory, "failure-" + UUID.randomUUID() + ".png");
        try {
            Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(),
                    destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save failure screenshot", e);
        }
        LAST_SCREENSHOT.set(destination);
        return destination;
    }

    public static File getLastScreenshot() {
        return LAST_SCREENSHOT.get();
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
        }
        DRIVER.remove();
    }
}
