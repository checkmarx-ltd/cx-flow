## CxGO
A specific build is available for CxGO integration.  It is also bundled within the releases under Checkmarx-LTD GitHub organization.

<br>[https://github.com/checkmarx-ltd/cx-flow/releases/latest (cxgo-x.x.x.jar)](https://github.com/checkmarx-ltd/cx-flow/releases/latest)
<br>This specific build is bundled to leverage the following SDK to interface with CxGO:
<br>[https://github.com/checkmarx-ts/cxod-spring-boot-java-sdk - Connect to preview ](https://github.com/checkmarx-ts/cxod-spring-boot-java-sdk)

Configuration options for CxFlow using CxGO are identical <br>with the exception of the checkmarx configuration block, which should look like the following:
```
checkmarx:
  client-secret: xxxx
  base-url: https://api.checkmarx.net
  portal-url: https://cloud.checkmarx.net
  # CxOD Business unit that will contain the project/application/scan
  team: \Demo\CxFlow\
  url: ${checkmarx.base-url}
  multi-tenant: true
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
java -jar <cx-flow-cxgo.jar> --spring.config.location=application.yml --scan --cx-team="\my\bu\ba" --cx-project="Myprj" --app=AppID
```
CLI execution mode is supported, and 
  * --scan indicates that the source will be zipped/scanned.  
  * --cx-team value must be the path where the project will be created - inclusive of the business application.  
  * --cx-project will be the project name created under the team path.
  * --app is required, but is only downstream when bug trackers are configured for CxFlow
