@ComponentTest
Feature: Analytics report should be logged correctly when CxFlow approves or fails a pull request

  Scenario Outline: Analytics logging when thresholds are exceeded
    Given thresholds are configured as HIGH: 2, MEDIUM: 5, LOW: 10
    And severity filters allow all values
    When pull request is created for a repo with URL: "https://github.com/cxflowtestuser/Cx-FlowRepo" in GitHub
    And SAST returns scan ID: <scan ID> and finding count per severity: HIGH: <fc_high>, MEDIUM: <fc_medium>, LOW: <fc_low>
    Then in analytics report, the operation is "Pull Request"
    And pullRequestStatus is "<status>"
    And repoUrl is encrypted as "<encryptedRepoUrl>"
    And scanInitiator is "CX", scanId is <scan ID>, pullRequestStatus is "failure",
    And findingsPerSeverity are HIGH: <fc_high>, MEDIUM: <fc_medium>, LOW: <fc_low>
    And thresholds are HIGH: <thr_high>, MEDIUM: <thr_medium>, LOW: <thr_low>

    Examples:
      | scan ID | fc_high | fc_medium | fc_low | status  | encryptedRepoUrl                                                 |
      | 3452124 | 4       | 2         | 12     | failure | 0iM+sQuMxUSLLz1yHxqZup7bXXzEyALQr+XClITaj2TbuhbwilUhQlt0SgjfDOBj |
      | 937582  | 1       | 3         | 8      | success | 0iM+sQuMxUSLLz1yHxqZup7bXXzEyALQr+XClITaj2TbuhbwilUhQlt0SgjfDOBj |

# Example of analytics log record:
# {
#     "timestamp": "2020-03-26 15:12:19.967",
#     "Pull Request": {
#         "repoUrl": "0iM+sQuMxUSLLz1yHxqZup7bXXzEyALQr+XClITaj2TbuhbwilUhQlt0SgjfDOBj",
#         "scanInitiator": "SAST",
#         "scanId": 82919892,
#         "pullRequestStatus": "failure",
#         "findingsPerSeverity": {
#             "MEDIUM": 42,
#             "LOW": 4,
#             "HIGH": 2
#         },
#         "thresholds": {
#             "MEDIUM": 3,
#             "LOW": 8,
#             "HIGH": 1
#         }
#     }
# }