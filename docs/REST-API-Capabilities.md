* [REST API](#rest)
  * [Authentication](#authentication)
  * [Get/scanresults](#getscanresults)
  * [JSON Output](#json)

## <a name="rest">REST API</a>
### <a name="authentication">Authentication</a>
CxFlow REST endpoints are authenticated using a Shared API Key agreed to by the client and server.
<br>The Shared API Key needs to be :
  * configured in the "cx-flow" section of the CxFlow Sever Configuration file (see CxFlow Configuration), and
  * supplied as the value of the "x-cx-token" header for each REST API request
####Example
<br>If the client and server agree to use "1F4QipGtiR2Ub68ABEYxd" as the Shared API Key in a specific CxFlow deployment, the "cx-flow" section of the configuration (yaml) file will look like this:
```
...
cx-flow:
  # Agreed upon Shared API Key
  token: 1F4QipGtiR2Ub68ABEYxd
...
```
and a REST API invocation will need to provide the same Shared API token in the "x-cx-token" header:
```

...
Host: somehost:80
Accept: application/json
x-cx-token: 1F4QipGtiR2Ub68ABEYxdd
Content-Type: application/json
...
```
#### Authentication Failure
If the expected and provided API keys do not match, the CxFlow server will respond with HTTP 403 Forbidden.

### <a name="getscanresults">GET /scanresults</a>
CxFlow provides a REST API to retrieve the Checkmarx Scan Results and return it as a JSON string. The endpoint requires the projectName as well as the teamPath query parameters. 
<br>The endpoint always fetches the latest scan results for the requested project.
```
GET /scanresults?project={projectName}&team={teamPath}
```
The projectName is the name of the Checkmarx Project and teamPath is the full Checkmarx Team Path.
#### Example
To fetch the latest scan results of a Checkmarx Project named 'DVJA', that lives under the team 'CxServer\\North America\\Engineering\\Development', submit the following GET request:
```
GET /scanresults?project=dvja&team=CxServer\North America\Engineering\Development
HTTP/1.1
Host: someserver:xx
Accept: application/json
x-cx-token: some-shared-api-token
Content-Type: application/json
```
#### Optional Query Parameters
In addition to the project and team parameters, the /scanresults endpoint also respects/handles the following CxFlow parameters:
<br>(see CxFlow Configuration for how these parameters are used)
Configuration | Description
------------ | -------------
application | Override the application name, which is directly linked to Jira and other defect management implementations for tracking purposes.
severity | Override the severity filters.  For multiple severity simply list multiple times.  i.e. severity=High&severity=Medium
cwe | Override the cwe filters.  For multiple cwe simply list multiple times.  i.e. cwe=89&cwe=79
category | Override the category filters.  For multiple category simply list multiple times.  i.e. category=Stored_XSS&category=SQL_Injection
status | Override the status filters (Confirmed/Urgent)
assignee | Override the assignee (Jira)
override | Override a complete JSON blob
bug | Override the default configured bug tracker

### <a name="json">JSON Output</a>
The /scanresults endpoint produces a JSON document that represents the data pertinent to the requested project's scan. 
<br>Example
```
{
    "osa": null,
    "projectId": "1",
    "team": "Project Alpha Team",
    "project": "WebGoat.NET",
    "link": "http://WIN-7IT7ABBICI5/CxWebClient/ViewerMain.aspx?scanid=1000000&amp;projectid=1",
    "files": "253",
    "loc": "28653",
    "scanType": "Full",
    "additionalDetails": {
        "numFailedLoc": "12",
        "scanRiskSeverity": "100",
        "scanId": "1000000",
        "scanStartDate": "Monday, February 18, 2019 1:16:54 PM",
        "customFields": {
            "VZ_FIELD": "PROJGOAT8",
            "VAST_ID": "376467",
            "WHATEVER_FIELD": "V1.2"
        },
        "scanRisk": "100"
    },
    "output": null,
    "xissues": [
        {
            "vulnerability": "Reflected_XSS_All_Clients",
            "similarityId": "1967498910",
            "cwe": "79",
            "cve": null,
            "description": "Method btnGO_Click at line 31 of WebGoat\\Content\\EncryptVSEncode.aspx.cs gets user input for the Text element. This element’s value then flows through the code without being properly sanitized or validated and is eventually displayed to the user in method MakeRow at line 58 of WebGoat\\Content\\EncryptVSEncode.aspx.cs. This may enable a Cross-Site-Scripting attack.      \n",
            "language": "CSharp",
            "severity": "High",
            "link": "http://WIN-7IT7ABBICI5/CxWebClient/ViewerMain.aspx?scanid=1000000&amp;projectid=1&amp;pathid=65",
            "filename": "WebGoat/Content/EncryptVSEncode.aspx.cs",
            "gitUrl": "",
            "osaDetails": null,
            "details": {
                "38": "            string secret = txtString.Text;"
            },
            "additionalDetails": {
                "recommendedFix": "http//WIN-7IT7ABBICI5/CxWebClient/ScanQueryDescription.aspx?queryID=427&queryVersionCode=54386807&queryTitle=Reflected_XSS_All_Clients",
                "categories": "PCI DSS v3.2;PCI DSS (3.2) - 6.5.7 - Cross-site scripting (XSS),OWASP Top 10 2013;A3-Cross-Site Scripting (XSS),FISMA 2014;System And Information Integrity,NIST SP 800-53;SI-15 Information Output Filtering (P0),OWASP Top 10 2017;A7-Cross-Site Scripting (XSS)",
                "results": [
                    {
                        "sink": {
                            "file": "WebGoat/Content/EncryptVSEncode.aspx.cs",
                            "line": "67",
                            "column": "16",
                            "object": "Text"
                        },
                        "state": "0",
                        "source": {
                            "file": "WebGoat/Content/EncryptVSEncode.aspx.cs",
                            "line": "38",
                            "column": "39",
                            "object": "Text"
                        }
                    }
                ]
            }
        },
        {
            "vulnerability": "Reflected_XSS_All_Clients",
            "similarityId": "-650601855",
            "cwe": "79",
            "cve": null,
            "description": "Method Page_Load at line 14 of WebGoat\\Content\\HeaderInjection.aspx.cs gets user input for the ToString element. This element’s value then flows through the code without being properly sanitized or validated and is eventually displayed to the user in method Page_Load at line 14 of WebGoat\\Content\\HeaderInjection.aspx.cs. This may enable a Cross-Site-Scripting attack.      \n",
            "language": "CSharp",
            "severity": "High",
            "link": "http://WIN-7IT7ABBICI5/CxWebClient/ViewerMain.aspx?scanid=1000000&amp;projectid=1&amp;pathid=66",
            "filename": "WebGoat/Content/HeaderInjection.aspx.cs",
            "gitUrl": "",
            "osaDetails": null,
            "details": {
                "33": "            lblHeaders.Text = Request.Headers.ToString().Replace(\"&\", \"<br />\");",
                "26": "                newHeader.Add(\"newHeader\", Request.QueryString[\"Header\"]);"
            },
            "additionalDetails": {
                "recommendedFix": "http//WIN-7IT7ABBICI5/CxWebClient/ScanQueryDescription.aspx?queryID=427&queryVersionCode=54386807&queryTitle=Reflected_XSS_All_Clients",
                "categories": "PCI DSS v3.2;PCI DSS (3.2) - 6.5.7 - Cross-site scripting (XSS),OWASP Top 10 2013;A3-Cross-Site Scripting (XSS),FISMA 2014;System And Information Integrity,NIST SP 800-53;SI-15 Information Output Filtering (P0),OWASP Top 10 2017;A7-Cross-Site Scripting (XSS)",
                "results": [
                    {
                        "sink": {
                            "file": "WebGoat/Content/HeaderInjection.aspx.cs",
                            "line": "33",
                            "column": "24",
                            "object": "Text"
                        },
                        "state": "0",
                        "source": {
                            "file": "WebGoat/Content/HeaderInjection.aspx.cs",
                            "line": "33",
                            "column": "47",
                            "object": "ToString"
                        }
                    },
                    {
                        "sink": {
                            "file": "WebGoat/Content/HeaderInjection.aspx.cs",
                            "line": "33",
                            "column": "24",
                            "object": "Text"
                        },
                        "state": "0",
                        "source": {
                            "file": "WebGoat/Content/HeaderInjection.aspx.cs",
                            "line": "33",
                            "column": "39",
                            "object": "Headers"
                        }
                    },
                    {
                        "sink": {
                            "file": "WebGoat/Content/HeaderInjection.aspx.cs",
                            "line": "27",
                            "column": "38",
                            "object": "newHeader"
                        },
                        "state": "0",
                        "source": {
                            "file": "WebGoat/Content/HeaderInjection.aspx.cs",
                            "line": "26",
                            "column": "52",
                            "object": "QueryString_Header"
                        }
                    }
                ]
            }
        }
    ]
}
```