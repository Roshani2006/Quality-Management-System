@ui
Feature: Document lifecycle
  A configured QMS test user can create and process a controlled document.

  Scenario: Create, approve, version, and audit a document
    Given I open the configured document workspace
    When I create the configured test document
    Then its configured draft status is displayed
    When I submit the document for approval
    Then its configured pending-approval status is displayed
    When I approve the document
    Then its configured approved status is displayed
    When I open the document version history
    Then at least one document version is visible
    When I open the document audit trail
    Then at least one audit event is visible
    Then configured database integrity validation succeeds when enabled
