@CxGoIntegrationTests @IntegrationTest
Feature: Cx-Flow CxGo Integration tests


  @CxGoPullIntegrationTests
  Scenario Outline: Test CxFlow pull request scan flow with CxGo
    Given SCM type is <scm>
    And Pull Request is opened in repo
    And Thresholds set to <exceeded>
    Then CxFlow initiate scan in CxGo
    And Pull request comments updated in repo and status is <pull request status>

    Examples:
      | scm      | exceeded       | pull request status  |
      | Github   | false          | success              |


  @CxGoPushIntegrationTests
  Scenario Outline: Test CxFlow push event scan flow with CxGo
    Given SCM type is <scm>
    And Push event is sent to cxflow
    And Filters set to <filters>
    Then CxFlow initiate scan in CxGo
    And bugTracker will be updated with tickets for filtered findings by <filters>

    Examples:
      | scm      | filters |
      | Github   | filters |
      | Gitlab   | filters |

