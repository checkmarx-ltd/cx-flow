@AzureDevOps
@PublishProcessing
@Integration
@Create_issue
@WebHook
Feature: GitHub webhook requests should support publishing issues to Azure DevOps [ADO]
  Check the general ability to perform the flow described above.
  The detailed logic of opening/closing issues is covered in another feature file.

  Scenario Outline: Publishing caused by GitHub webhook request
    Given issue tracker is ADO
    And ADO doesn't contain any issues
    And CxFlow filters are disabled
    When GitHub notifies CxFlow about a "<webhook event>"
    And SAST scan returns a report with 1 finding
    And CxFlow publishes the report
    Then ADO contains 1 issue

    Examples:
      | webhook event |
      | pull request  |
      | push          |
