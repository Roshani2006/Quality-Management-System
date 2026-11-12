package com.qualitymanagement.framework.api;

import com.qualitymanagement.framework.config.QmsConfig;
import com.qualitymanagement.framework.data.TestDataLoader;
import com.qualitymanagement.framework.db.DatabaseIntegrityValidator;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class DocumentApiContractTest {
    private String documentId;

    private static final String[] REQUIRED_CONFIGURATION = {
            "api.baseUrl", "api.uploadMethod", "api.uploadPath", "api.uploadBodyFile", "api.uploadContentType",
            "api.uploadIdJsonPath", "api.transitionsFile", "api.metadataPathTemplate",
            "api.searchPath", "api.searchQueryParameter", "api.searchResultsJsonPath",
            "api.searchQuery"
    };

    @BeforeClass
    public void uploadTestDocument() {
        if (!QmsConfig.hasAll(REQUIRED_CONFIGURATION)) {
            throw new SkipException("API contract tests skipped: configure QMS_API_* values for the target API.");
        }
        if (QmsConfig.isTrue("db.validateAfterWorkflow", false)
                && !QmsConfig.hasAll("db.jdbcUrl", "db.validationQuery", "db.expectedValue")) {
            throw new IllegalStateException("SQL validation is enabled but its JDBC URL, query, or expected value is missing.");
        }
        File body = new File(QmsConfig.require("api.uploadBodyFile"));
        if (!body.isFile()) {
            throw new IllegalStateException("Configured API upload body file does not exist: " + body);
        }
        Response response = request()
                .contentType(QmsConfig.require("api.uploadContentType"))
                .body(body)
                .request(QmsConfig.require("api.uploadMethod"), QmsConfig.require("api.uploadPath"));
        Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "Document upload failed with HTTP " + response.statusCode());
        documentId = response.jsonPath().getString(QmsConfig.require("api.uploadIdJsonPath"));
        Assert.assertNotNull(documentId, "Upload response did not contain the configured document ID field");
        Assert.assertFalse(documentId.trim().isEmpty(), "Upload response returned an empty document ID");
    }

    @DataProvider(name = "configuredTransitions")
    public Object[][] configuredTransitions() throws Exception {
        if (!QmsConfig.hasAll(REQUIRED_CONFIGURATION)) {
            return new Object[0][0];
        }
        List<Map<String, String>> cases = TestDataLoader.csv(new File(QmsConfig.require("api.transitionsFile")));
        if (cases.isEmpty()) {
            throw new IllegalStateException("The configured API transitions CSV must contain at least one case.");
        }
        Object[][] data = new Object[cases.size()][1];
        for (int i = 0; i < cases.size(); i++) {
            data[i][0] = cases.get(i);
        }
        return data;
    }

    @Test(dataProvider = "configuredTransitions")
    public void appliesConfiguredStatusTransition(Map<String, String> transition) {
        String path = requiredCaseValue(transition, "pathTemplate");
        String bodyPath = requiredCaseValue(transition, "bodyFile");
        String contentType = requiredCaseValue(transition, "contentType");
        String statusPath = requiredCaseValue(transition, "expectedStatusJsonPath");
        String expectedStatus = requiredCaseValue(transition, "expectedStatus");
        File body = new File(bodyPath);
        if (!body.isFile()) {
            throw new IllegalStateException("Transition body file does not exist: " + body);
        }

        Response transitionResponse = request()
                .pathParam("documentId", documentId)
                .contentType(contentType)
                .body(body)
                .request(requiredCaseValue(transition, "method"), path);
        Assert.assertTrue(transitionResponse.statusCode() >= 200 && transitionResponse.statusCode() < 300,
                "Transition failed with HTTP " + transitionResponse.statusCode());

        Response metadata = request()
                .pathParam("documentId", documentId)
                .get(QmsConfig.require("api.metadataPathTemplate"));
        Assert.assertTrue(metadata.statusCode() >= 200 && metadata.statusCode() < 300,
                "Metadata lookup failed with HTTP " + metadata.statusCode());
        Assert.assertEquals(metadata.jsonPath().getString(statusPath), expectedStatus,
                "Document state did not match the configured transition result");
    }

    @Test
    public void rejectsConfiguredNegativeAndBoundaryInputs() throws Exception {
        String casesFile = QmsConfig.get("api.negativeCasesFile");
        if (casesFile == null) {
            throw new SkipException("Negative/boundary API cases skipped: configure QMS_API_NEGATIVE_CASES_FILE.");
        }
        List<Map<String, String>> cases = TestDataLoader.csv(new File(casesFile));
        if (cases.isEmpty()) {
            throw new IllegalStateException("The configured negative/boundary CSV must contain at least one case.");
        }
        for (Map<String, String> testCase : cases) {
            String category = requiredCaseValue(testCase, "category");
            Assert.assertTrue("negative".equalsIgnoreCase(category) || "boundary".equalsIgnoreCase(category),
                    "Case category must be 'negative' or 'boundary'");
            String bodyPath = requiredCaseValue(testCase, "bodyFile");
            File body = new File(bodyPath);
            if (!body.isFile()) {
                throw new IllegalStateException("Negative/boundary body file does not exist: " + body);
            }
            Response response = request()
                    .pathParam("documentId", documentId)
                    .contentType(requiredCaseValue(testCase, "contentType"))
                    .body(body)
                    .request(requiredCaseValue(testCase, "method"), requiredCaseValue(testCase, "pathTemplate"));
            Assert.assertEquals(response.statusCode(),
                    Integer.parseInt(requiredCaseValue(testCase, "expectedStatusCode")),
                    requiredCaseValue(testCase, "caseName"));
        }
    }

    @Test(dependsOnMethods = "appliesConfiguredStatusTransition")
    public void findsUploadedDocumentInConfiguredSearch() throws Exception {
        Response response = request()
                .queryParam(QmsConfig.require("api.searchQueryParameter"), QmsConfig.require("api.searchQuery"))
                .get(QmsConfig.require("api.searchPath"));
        Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "Document search failed with HTTP " + response.statusCode());
        List<String> resultIds = response.jsonPath().getList(QmsConfig.require("api.searchResultsJsonPath"));
        Assert.assertNotNull(resultIds, "Search response did not contain the configured result list");
        Assert.assertTrue(resultIds.contains(documentId), "Search results did not contain uploaded document " + documentId);
        if (QmsConfig.isTrue("db.validateAfterWorkflow", false)) {
            DatabaseIntegrityValidator.validate(documentId);
        }
    }

    private RequestSpecification request() {
        RequestSpecification request = given().baseUri(QmsConfig.require("api.baseUrl"));
        String token = QmsConfig.get("api.bearerToken");
        return token == null ? request : request.auth().oauth2(token);
    }

    private String requiredCaseValue(Map<String, String> transition, String key) {
        String value = transition.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing '" + key + "' in configured transitions CSV");
        }
        return value.trim();
    }
}
