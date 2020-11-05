@CxGoIntegrationTests @IntegrationTest
Feature: Cx-Flow CxGo Integration tests


  @CxGoIntegrationTests
  Scenario Outline: Test CxFlow basic scan flow with CxGo
    Given SCM type is <scm>
    Given Pull Request is opened in repo
    And configured scanner in cxflow configuration are <scanner>
    Then CxFlow initiate scan in CxGo
    And <bugTrackerType> bugTracker will be updated with tickets for <scanner> findings
    And Pull request is updated in <scm> repo

    Examples:
      | scm      | scanner      | bugTrackerType   |
      | Github  | SAST + SCA    | Jira             |

