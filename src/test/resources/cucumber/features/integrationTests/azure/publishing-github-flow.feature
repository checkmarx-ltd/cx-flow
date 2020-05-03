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
    Then ADO contains <issue count> issues

    Examples:
      | webhook event | issue count |
      | pull request  | 0           |
      | push          | 1           |
  # For the 'pull request' case, we expect ADO issue count = 0, because in this case the issues should be published
  # into GitHub itself (see comments in GitHub controller).