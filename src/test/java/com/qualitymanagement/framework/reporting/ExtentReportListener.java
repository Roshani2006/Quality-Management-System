package com.qualitymanagement.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.qualitymanagement.framework.ui.DriverFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExtentReportListener implements ITestListener {
    private static final ExtentReports REPORTS = createReports();
    private final ConcurrentMap<String, ExtentTest> tests = new ConcurrentHashMap<String, ExtentTest>();

    @Override
    public void onTestStart(ITestResult result) {
        tests.put(key(result), REPORTS.createTest(result.getMethod().getQualifiedName()));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        test(result).pass("Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = test(result).fail(result.getThrowable());
        File screenshot = DriverFactory.getLastScreenshot();
        if (screenshot != null && screenshot.isFile()) {
            try {
                test.addScreenCaptureFromPath(screenshot.getAbsolutePath());
            } catch (Exception e) {
                test.warning("Could not attach failure screenshot: " + e.getMessage());
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (result.getThrowable() == null) {
            test(result).skip("Skipped");
        } else {
            test(result).skip(result.getThrowable());
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        REPORTS.flush();
    }

    private ExtentTest test(ITestResult result) {
        ExtentTest test = tests.get(key(result));
        if (test == null) {
            test = REPORTS.createTest(result.getMethod().getQualifiedName());
            tests.put(key(result), test);
        }
        return test;
    }

    private String key(ITestResult result) {
        return Integer.toString(System.identityHashCode(result));
    }

    private static ExtentReports createReports() {
        ExtentSparkReporter reporter = new ExtentSparkReporter("target/extent-report.html");
        ExtentReports reports = new ExtentReports();
        reports.attachReporter(reporter);
        return reports;
    }
}
