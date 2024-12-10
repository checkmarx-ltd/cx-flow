
# CXFlow GitLab MR Scan Comment Issue

In CXFlow, if the scan comment is reflecting the **repository creator** (instead of the Merge Request (MR) creator), this behavior likely arises because the token being used is associated with the person or account that initially created the repository. This means that all comments from the scan would be posted under that user's identity.

## Suggested Solution to Fix the Issue:

### 1. Use a Separate GitLab Account for Checkmarx:
- Create a **dedicated GitLab account** in the name of **Checkmarx** (or another service name) specifically for CI/CD pipeline scans.
- This will ensure that all comments are posted under the "Checkmarx" account, making it easier to identify that the comment comes from the automated scanning process and not the person who created the repository.

### 2. Pass the Token via Pipeline:
- Configure the **pipeline to pass the token** for the Checkmarx service account when running scans. This can be done by setting the **CI/CD pipeline token** to use the Checkmarx account's token instead of the repository creator's token.
- This way, all scan-related comments will reflect the Checkmarx account, making it clear that the scan was performed by the tool, not by the repo owner.

## Benefits:
- **Clear identification**: Using a separate account like "Checkmarx" will clearly show that the comments come from the automated scanning tool, not a specific user.
- **Simplified management**: Centralizing scan comments under one service account makes it easier to track automated actions.

This approach will help resolve the issue of scan comments reflecting the repository creator, providing better transparency in the process.
