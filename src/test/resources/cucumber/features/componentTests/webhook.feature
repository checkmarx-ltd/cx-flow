@WebHookFeature @ComponentTest
Feature: Check Component tests Webhook functionality

  Scenario: High load on CxFlow due to frequent WebHook requests from GitHub
    Given version control provider is GitHub
    Given CxFlow is running as a service
    And webhook is configured for push event
    When GitHub sends WebHook requests to CxFlow 2 times per second
    Then each of the requests is answered in at most 500 ms