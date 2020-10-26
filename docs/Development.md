[[/Images/dev1.png]]
* Request details are mapped to a ScanRequest Object, which is referenced through every step and can have elements accessible in the bug tracker 
  * If the request is received from a Repo Controller or Command line option for scanning the scan will be initiated in Checkmarx, and results will be retrieved once the scan is complete.  If the bug-tracker is specified as NONE, it will not wait for results, and the process is complete
* Results are mapped from Checkmarx to a ScanResult object, and this is passed to the ResultService for processing.
  * If a bug tracker other than JIRA, NONE, EMAIL, then the IssueService will invoke the bug-tracker that is specified that is in the bug-tracker-impl list.  This value is directly linked to the name of a spring bean that performs the logic.
[[/Images/dev2.png]]


## Creating a Custom Bug Tracker
1. Create a Spring Boot Service Bean that implements the following Interface: 
```

package com.checkmarx.flow.custom;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.exception.MachinaException;
 
import java.util.List;
 
public interface IssueTracker {
    void init(ScanRequest request, ScanResults results) throws MachinaException;
    void complete(ScanRequest request, ScanResults results) throws MachinaException;
    String getFalsePositiveLabel() throws MachinaException;
    List<Issue> getIssues(ScanRequest request) throws MachinaException;
    Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException;
    void closeIssue(Issue issue, ScanRequest request) throws MachinaException;
    Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException;
    String getIssueKey(Issue issue, ScanRequest request);
    String getXIssueKey(ScanResults.XIssue issue, ScanRequest request);
    boolean isIssueClosed(Issue issue);
    boolean isIssueOpened(Issue issue);
}
```
2. Ensure the Spring Boot Service Bean has a valid name that you will use to reference in the yaml configuration as a bug-tracker-impl - the following shows the Json service bean: 
```

package com.checkmarx.flow.custom;
 
import com.checkmarx.flow.dto.Issue;
...
...
 
@Service("Json")
public class JsonIssueTracker implements IssueTracker {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JsonIssueTracker.class);
    private final JsonProperties properties;
 
    @ConstructorProperties({"properties"})
    public JsonIssueTracker(JsonProperties properties) {
        this.properties = properties;
    }
....
....
}
```
3. Ensure the functions are functions are implemented accordingly
4. Add the bean to the cx-flow yaml config block - you can see the reference the Json bean below: 
```
cx-flow:
  contact: admin@cx.com
  bug-tracker: Json
  bug-tracker-impl:
    - Json
```
5. Ensure the bug-tracker is specified to be your new bean, if you wish for this to be the default.  It can also be provided as an override through webhook url paramater (bug=Json), or on the command line (--cx-flow.bug-tracker=Json).  
<br>**Note** This bean is case sensitive

## Source
### Packages
Package | Description
------------ | -------------
com.checkmarx.flow.config | All bean configurations and Property file POJO mappings.
com.checkmarx.flow.controller | All HTTP Endpoints.  GitHub/GitLab/Bitbucket WebHook services.
com.checkmarx.flow.dto | Sub-packages contain all DTO objects for Checkmarx, GitHub, GitLab, etc.
com.checkmarx.flow.exception | Exceptions
com.checkmarx.flow.filter | Specify any filters applied to Web Traffic flow.  Currently passthrough, but can be used for IP filtering.
com.checkmarx.flow.custom | Contains any "custom" bean bug tracker implementations and property classes
com.checkmarx.flow.service | Core logic.  Each Issue tracker has a Service along with a main flowService, which drives the overall flow.
com.checkmarx.flow.utils | Utilities package

### Services
Service | Description
------------ | -------------
CxService | SOAP Based API Client for Checkmarx
CxLegacyService | REST Based API Client for Checkmarx
JiraIssueService | REST Based API Client for Jira (JRJC - Jira Java REST Client)
GitHubService | REST Based API Client GitHub
GitLabService | REST Based API Client GitLab
ADOService | REST Based API Client Azure DevOps
EmailService | Email (SMTP) client
FlowService | Main Service driving integrations with other Service components

### Controllers
Controller | Description
------------ | -------------
GitHubController | Ping, Push, Pull  (TBD) event HTTP listeners
GitLabController | Push, Merge (TBD) event HTTP listeners
BitbucketController | Push event HTTP listener
ADOController | Push/Pull event HTTP listener
FlowController | Unused, but intended for Call-back implementations, rest invocation to retrieve results for a given project based on bug tracker implementation