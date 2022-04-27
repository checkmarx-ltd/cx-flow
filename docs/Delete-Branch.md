* [Deleting CxSAST Project upon Branch Deletion](#deleteProject)
* [CxFlow Branch Configuration](#branchConfiguration)
* [Using the deletion feature together with configuration-as-code](#deletionWithCxConfig)
* [References](#references)
* [SCMs](#scms)
  * [GitHub](#github)
  * [Azure Devops](#ado)
  * [GitLab](#gitlab)
  * [BitBucket](#bitbucket)

## <a name="deleteProject">Deleting CxSAST Project upon Branch Deletion</a>

CxFlow is able to create a new CxSAST project when initiating scan, if the project doesn't exist yet.

When working with CxFlow in web service mode and using webhook events to trigger CxSAST scan, CxFlow will create a new CxSAST project for each SCM feature branch which open a pull request into scanned branch. This might cause flood of projects on CxSAST for feature branch and unexpected consumption of licenses.

In order to overcome this, CxFlow can automatically delete CxSAST project upon branch deletion.

### <a name="branchConfiguration">CxFlow Branch Configuration</a>

If the branch is a scanned branch - named under ‘cx-flow.branches’ section - CxFlow will not delete the Checkmarx project when the branch is deleted.  Therefore, if you want to delete your Checkmarx Project, the branch name cannot be under this section.

### <a name="deletionWithCxConfig">Using the deletion feature together with configuration-as-code</a>
Suppose we use configuration-as-code to define CxSAST project name in a feature branch. When the feature branch is deleted, CxFlow won’t be able to determine a correct project name for deletion, since the config-as-code has already been deleted with the branch.

If a project name mismatch occurs as described above, you can still use the project deletion feature together with config-as-code. To do this, define the following property in the github section of CxFlow yml file: 

```yaml
use-config-as-code-from-default-branch: true 
```

This will make CxFlow to always read configuration-as-code from repository default branch

### <a name="references">References</a>

* CxFlow pull request #383

* CxFlow GitHub issue: #345

### <a name="scms">SCMs</a>

#### <a name="github">GitHub</a>

To enable the deletion feature for GitHub webhook events, add registration to ‘Branch or tag deletion’ in the webhook events registration:

[[/Images/github-branch-delete.png|github delete webhook event]]


#### <a name="ado">Azure Devops</a>

* Uses a service hook on “Code pushed” event

* Uses the /ado/push endpoint

The branch deletion will trigger a PUSH event in ADO

To enable the deletion feature for Azure webhook events you need to add this property under  azure section to CxFlow yml file:  deleteCxProject: true

Suppose we use configuration-as-code to define CxSAST project name in a feature branch. When the feature branch is deleted, CxFlow won’t be able to determine a correct project name for deletion, since the config-as-code has already been deleted with the branch

In case of Azure, in order to overcome it, when CxFlow detect ADO push-DELETE event, CxFlow will try to read configuration as code from repo Default branch as defined in the ADO event. In case ado delete event doesn't contain default branch, CxFlow will read the configuration from current branch

This raise the following limitation:

<u>Limitation:</u>  when using configuration-as-code to define CxSAST project name, CxSAST project will be deleted only if the user delete the branch by merge the PR:

[[/Images/ADO__merge_and_delete.png|github delete webhook event]]

if the user delete feature-branch manually without complete pull request - CxFlow can’t read project name from default branch and will not delete the corresponding CxSAST project

[[/Images/ADO_delete_branch.png|github delete webhook event]]

#### <a name="gitlab">GitLab</a>

GitLab does not support webhook delete events therefore CxFlow does not support GitLab branch deletion.

#### <a name="bitbucket">Bitbucket Server</a>

* Uses the webhook PUSH event
* When an unprotected branch is deleted BitBucket server sends a PUSH event of type DELETE.

Bitbucket Server will delete a SAST project either using the PUSH webhook event or using the Post Webhooks plugin.  The current implementation is limited in that:

* Project delete not work if using Config-As-Code given the settings for team and/or project name have been deleted from the branch.
* Project delete will work if the project name is calculated or scripted and the team assigned to the project matches the default team in the CxFlow YAML configuration.

**Bitbucket Cloud**

Bitbucket cloud currently does not support deleting project in CxSAST when unprotected branch is deleted.
