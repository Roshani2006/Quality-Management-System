# Configurable QMS end-to-end test framework

This repository contains a Java 8-compatible Maven test harness for a document/quality-management application. The repository did not include an application, API specification, database schema, credentials, or existing CI configuration when this framework was added. Consequently, the tests use **no assumed routes, selectors, field names, or credentials**: supply the target system's real contract through configuration before enabling integration tests.

## What is included

- Selenium WebDriver + Cucumber/Gherkin lifecycle scenario for document creation, approval, status checks, version history, and audit trail.
- Page Objects created through Selenium `PageFactory`, configurable CSS selectors, and a thread-local Chrome/Firefox `DriverFactory`.
- RestAssured contract tests for document upload, CSV-driven status transitions, metadata state verification, and search.
- TestNG suite with parallel Cucumber scenarios; reusable CSV, JSON, and XLSX data loaders.
- ExtentReports HTML output and failure screenshots; Cucumber JSON output.
- GitHub Actions CI. Optional AWS S3 artifact upload uses GitHub OIDC and is disabled unless a bucket variable is set.
- Optional read-only SQL scalar validation after completed UI/API workflows using a caller-supplied JDBC driver, query, and expected value.

## Requirements

- JDK 8 or later and Maven 3.6+.
- For UI tests: a supported browser and matching browser driver (`chromedriver` or `geckodriver`) on `PATH`, or configure `QMS_SELENIUM_DRIVER_PATH`.
- A disposable QMS test environment, test account/session, test document, actual API endpoint/payload contract, and permitted test data. UI tests assume the configured workspace URL is already authenticated; the application’s login/SSO flow is not known here.
- For SQL checks: the correct JDBC driver dependency on the test classpath, plus a read-only database account.

## Run locally

Run the framework checks; application-dependent suites are reported as skipped until their required configuration is present:

```shell
mvn test
```

Set configuration as `QMS_*` environment variables, `-Dqms.<key>=value` system properties, or values in an untracked `src/test/resources/config.properties` (copy `config.properties.example` first). System properties take precedence over environment variables, then the properties file. Never commit secrets, local config, or real controlled documents.

### UI configuration

Configure `QMS_UI_WORKSPACE_URL`, `QMS_UI_DOCUMENT_TITLE`, `QMS_UI_DOCUMENT_FILE`, all selectors listed in `config.properties.example`, and the actual draft, pending-approval, and approved labels. Selectors are CSS selectors for the application's real markup. Set `QMS_BROWSER` to `chrome` (default) or `firefox`, `QMS_BROWSER_HEADLESS` to `true`/`false`, and optionally `QMS_SELENIUM_DRIVER_PATH` to an installed matching driver. The feature is a representative lifecycle contract, not an assertion that every QMS uses these exact labels or controls.

### API configuration

Configure `QMS_API_BASE_URL`, `QMS_API_UPLOAD_METHOD`, `QMS_API_UPLOAD_PATH`, `QMS_API_UPLOAD_BODY_FILE`, `QMS_API_UPLOAD_CONTENT_TYPE`, `QMS_API_UPLOAD_ID_JSON_PATH`, `QMS_API_TRANSITIONS_FILE`, `QMS_API_METADATA_PATH_TEMPLATE`, `QMS_API_SEARCH_PATH`, `QMS_API_SEARCH_QUERY_PARAMETER`, `QMS_API_SEARCH_RESULTS_JSON_PATH`, and `QMS_API_SEARCH_QUERY`. Set `QMS_API_BEARER_TOKEN` only when the target API uses bearer authentication; otherwise adapt `DocumentApiContractTest.request()` to the real auth scheme.

The upload sends the configured file as a raw request body using the configured HTTP method; each transition row sends its configured body file as a raw request body. The transitions CSV must have `method,pathTemplate,bodyFile,contentType,expectedStatusJsonPath,expectedStatus` columns, with paths accepting RestAssured's `{documentId}` path parameter. An optional `QMS_API_NEGATIVE_CASES_FILE` CSV can exercise negative and boundary inputs; it requires `category,caseName,method,pathTemplate,bodyFile,contentType,expectedStatusCode` columns. All configured file paths must exist on the local runner or be provisioned by CI. Use actual endpoints, payloads, response JSON paths, and statuses from the target API documentation. APIs using multipart uploads need a small adaptation to the request construction; neither routes nor request formats are presumed.

### SQL and AWS (optional)

Set `QMS_DB_VALIDATE_AFTER_WORKFLOW=true`, `QMS_DB_JDBC_URL`, optional `QMS_DB_USERNAME`/`QMS_DB_PASSWORD`, `QMS_DB_VALIDATION_QUERY`, and `QMS_DB_EXPECTED_VALUE` to run a read-only scalar SQL check after the configured UI lifecycle and/or API workflow. The query and schema are intentionally not supplied; the query must be one `SELECT` statement and may use a single `?` parameter for the document ID in the API workflow. Because the UI flow does not assume how the application exposes document IDs, its query must not require that parameter. Add the target database JDBC driver dependency to `pom.xml`. The check does not run unless explicitly enabled and a workflow reaches its final verification.

To publish artifacts from Actions to S3, configure repository variable `TEST_ARTIFACTS_S3_BUCKET`, optional `TEST_ARTIFACTS_S3_PREFIX`, `AWS_REGION`, and secret `AWS_ROLE_ARN`. The role must trust this repository's GitHub Actions OIDC identity and allow writes to the target prefix. Without a bucket, the S3 steps do not run. The ordinary Actions artifact upload remains enabled.

## Reports

- `target/extent-report.html` — TestNG Extent report, including failure screenshots when captured.
- `target/cucumber-report.json` — Cucumber scenario results.
- `target/surefire-reports/` — TestNG/Maven test results.
- `target/screenshots/` — UI failure screenshots.
