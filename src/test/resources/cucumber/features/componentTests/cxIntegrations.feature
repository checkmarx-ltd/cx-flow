@Cx-integrations
Feature: Cx-Flow components tests integration with Cx-integrations

  Scenario: Cx-Go configuration is getting override when read-multi-tenant-configuration flag in cx-integrations configuration section set to true
    Given read-multi-tenant-configuration flag is set to true
    When cx-flow getting a new event
    Then scanRequest is getting populated with cx-go new configuration