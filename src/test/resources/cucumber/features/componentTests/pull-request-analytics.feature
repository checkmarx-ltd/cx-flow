@ComponentTest @PullRequestAnalyticsFeature
Feature: Analytics report should be logged correctly when CxFlow approves or fails a pull request

  Scenario Outline: Analytics logging with regards to thresholds
    Given thresholds are configured as HIGH: <thr_high>, MEDIUM: <thr_medium>, LOW: <thr_low>
    And filters are disabled
    When pull request is created for a repo with URL: "https://github.com/example-org/test-repo" in GitHub
    And SAST returns scan ID: <scan ID> and finding count per severity: HIGH: <f_high>, MEDIUM: <f_medium>, LOW: <f_low>
    Then in analytics report, the operation is "Pull Request"
    And pullRequestStatus is "<status>"
    And repoUrl is encrypted as "0iM+sQuMxUSLLz1yHxqZugoiOtt7BJThuArY+G37SqXRzGQabQH/iqaHm9xpn8Sx"
    And scanInitiator is "CX", scanId is <scan ID>, pullRequestStatus is "<status>"
    And findingsPerSeverity are HIGH: <f_high>, MEDIUM: <f_medium>, LOW: <f_low>
    And thresholds are HIGH: <thr_high>, MEDIUM: <thr_medium>, LOW: <thr_low>

    Examples:
      | scan ID | thr_high | thr_medium | thr_low | f_high | f_medium | f_low | status  |
      | 3452124 | 2        | 5          | 10      | 4      | 2        | 12    | FAILURE |
      | 937582  | 2        | 5          | 10      | 1      | 3        | 8     | SUCCESS |

# Example of analytics log record for a pull request:
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