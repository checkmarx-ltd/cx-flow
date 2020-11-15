

## CxGO
CxGo is a AWS multi tenant Checkmarx application which can scan both CxSAST and CxSCA

Starting from CxFlow 1.6.12, the CxGo scanner is integrated within the cx-flow-x.x.x.jar.
The application should be started by running cx-flow-x.x.x.jar while supplying the cxgo section in application.yml.

java -jar cx-flow-x.x.x.jar [params]

CxGO should look like the following:

```
cx-flow:
   enabled-vulnerability-scanners:
     - cxgo

cxgo:
  client-secret: xxxx
  base-url: https://api.checkmarx.net
  portal-url: https://cloud.checkmarx.net
  # CxOD Business unit that will contain the project/application/scan
  team: \Demo\CxFlow\
  url: ${cxgo.base-url}
  multi-tenant: false
  configuration: Default Configuration
  #
  ## Available Scan defaults
  #
  #   - CXOD_MOBILE_NATIVE = 1;
  #   - CXOD_MOBILE_WEB_BASED = 2;
  #   - CXOD_DESKTOP_NATIVE = 3;
  #   - CXOD_DESKTOP_WEB = 4;
  #   - CXOD_API = 5;
  #   - CXOD_FRONTEND = 6;
  #   - CXOD_BACKEND = 7;
  #   - CXOD_LAMBDA = 8;
  #   - CXOD_CLI = 9;
  #   - CXOD_SERVICE = 10;
  #   - CXOD_SMART_DEVICE = 11;
  #   - CXOD_OTHER = 12;
  scan-preset: 1,2,3,4,5,9
```
Parameter | Description
------------ | -------------
client-secret | API token generated from CxGO
team | This is the parent business unit path within CxGO.  A business application will be created (or reused if it exists) under this business unit that is based on the namespace of the repository.  Projects will then be created under this Business Application.  Applicable for WebHook execution mode.
scan-preset | CSV of scanning rules

### CLI Execution
```
java -jar <cx-flow-x.x.x.jar> --spring.config.location=application.yml --scan --cx-team="\my\bu\ba" --cx-project="Myprj" --app=AppID
```
CLI execution mode is supported
  * --scan indicates that the source will be zipped/scanned  
  * --cx-team value must be the path where the project will be created. The path includes business unit and application 
  * --cx-project will be the project name created under the team path
  * --app is required, but is only downstream when bug trackers are configured for CxFlow
  
  

### Filters
you can use filter configuration to filter SAST\SCA results. the configuration format is backward compatible with original SAST & SCA configurations:
to add SAST filters use 

#### SAST filters
```
cx-flow:
  filter-severity:
  filter-category:
  filter-cwe:
  filter-state:
```
for more details refer to [configuration page](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration#main-global-properties)

#### SCA filters
```
sca:
  filter-severity:
  filter-score:
```
for more details refer to [SCA filters](https://github.com/checkmarx-ltd/cx-flow/wiki/Integration-with-CxSCA#filterss)


### Thresholds
you can use thresholds to set the count of vulnerabilities which approve or fail a pull request 

```
cx-flow:
  thresholds:
    high: 10
    medium: 10
    low: 10
```

[CxFlow Thresholds](https://github.com/checkmarx-ltd/cx-flow/wiki/Thresholds-and-policies#thresholds-vs-basic-filters)
