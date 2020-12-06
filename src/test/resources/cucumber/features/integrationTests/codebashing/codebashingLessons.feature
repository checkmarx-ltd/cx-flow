@CodeBashingIntegrationTests @IntegrationTest
Feature: Cx-Flow integration with CodeBashing APIs

  @CodeBashingIntegrationTests
  Scenario Outline: Testing CxFlow add the correct lesson path per CWE
    Given CxFlow uses Bug-Tracker type <bugTracker type>
    And CodeBashing tenant base url <CodeBashing Configuration Exist>
    When CxFlow parsing SAST results
    And CxFlow finds a ticket with specific <type> and <CWE>
    Then CxFlow should add the correct <lesson path> to the ticket

    Examples:
      | CodeBashing Configuration Exist   | bugTracker type  |   type                       |  CWE     | lesson path                                           |
      | true                              | JIRA             |   SQL_Injection              |  89      | courses/java/lessons/sql_injection                    |
      | true                              | JIRA             |   HTTP_Response_Splitting    |  113     | default                                               |
      | true                              | JIRA             |   Reflected_XSS              |  79      | courses/java/lessons/reflected_xss                    |
      | false                             | JIRA             |   SQL_Injection              |  89      | default                                               |
      | true                              | GitLab           |   SQL_Injection              |  89      | courses/java/lessons/sql_injection                    |
      | true                              | GitLab           |   HTTP_Response_Splitting    |  113     | default                                               |
      | true                              | GitLab           |   Reflected_XSS              |  79      | courses/java/lessons/reflected_xss                    |
      | false                             | GitLab           |   SQL_Injection              |  89      | default                                               |

