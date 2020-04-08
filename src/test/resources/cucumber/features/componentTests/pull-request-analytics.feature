@ComponentTest @PullRequestAnalyticsFeature
Feature: Analytics report should be logged correctly when CxFlow approves or fails a pull request

  Scenario Outline: Analytics logging with regards to thresholds
    Given thresholds are configured as HIGH: <thr_high>, MEDIUM: <thr_medium>, LOW: <thr_low>
    And filters are disabled
    When pull request is created for a repo with URL: "<repo url>" in GitHub
    And SAST returns scan ID: <scan ID> and finding count per severity: HIGH: <f_high>, MEDIUM: <f_medium>, LOW: <f_low>
    Then in analytics report, the operation is "Pull Request"
    And pullRequestStatus is "<status>"
    And repoUrl contains encrypted "<repo url>"
    And scanInitiator is "SAST", scanId is "<scan ID>"
    And findingsMap is HIGH: <f_high>, MEDIUM: <f_medium>, LOW: <f_low>
    And thresholds are HIGH: <thr_high>, MEDIUM: <thr_medium>, LOW: <thr_low>

    Examples:
      | scan ID | thr_high | thr_medium | thr_low | f_high | f_medium | f_low | status  | repo url                                 |
      | 3452124 | 2        | 5          | 10      | 4      | 2        | 12    | FAILURE | https://github.com/example-org/test-repo |
      | 937582  | 2        | 5          | 10      | 1      | 3        | 8     | SUCCESS | https://github.com/example-org/test-repo |

# {
#     "timestamp": "2020-03-31 13:26:05.533",
#     "Pull Request": {
#         "projectName": null,
#         "repoUrl": "0iM+sQuMxUSLLz1yHxqZugoiOtt7BJThuArY+G37SqXRzGQabQH/iqaHm9xpn8Sx",
#         "scanInitiator": "SAST",
#         "scanId": "937582",
#         "pullRequestStatus": {
#             "message": "SUCCESS"
#         },
#         "findingsMap": {
#             "HIGH": 1,
#             "MEDIUM": 3,
#             "LOW": 8
#         },
#         "thresholds": {
#             "LOW": 10,
#             "HIGH": 2,
#             "MEDIUM": 5
#         }
#     }
# }