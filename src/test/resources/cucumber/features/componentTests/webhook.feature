@Skip @WebHookFeature @ComponentTest
Feature: Check Component tests Webhook functionality

  Scenario: High load on CxFlow due to frequent WebHook requests from GitHub
    Given source is GitHub
    And CxFlow is running as a service
    And webhook is configured for push event
    When GitHub sends WebHook requests to CxFlow 2 times per second
    Then 95% of the requests are answered in 200 ms or less
    And the rest 5% of the requests are answered in between 200 ms and 500 ms