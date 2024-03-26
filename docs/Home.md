> **Notice :** This is to inform you that the cxFlow will not be compatible to the old Java versions (version 8 and 11) beginning April 1 2024.
The customers need to upgrade the Java version in the CxFlow server to a version greater than or equal to Java 17 (less than 20)
After the upgrade, the customers need to use the CxFlow Java11.jar file 

# CxFlow
[[/Images/cxLogo.PNG]]
<br>[What is it?](#whatisit)
<br>[Quick Start](#quickstart)



## <a name="whatisit">What is it?</a>
CxFlow is a Spring Boot application that can run anywhere Java is installed. CxFlow glues together Checkmarx CxSAST and CxSCA scans with feedback to issue tracking systems via webhooks triggered by SCM events. 

CxFlow can also run as a CLI tool embedded in CI/CD pipelines. 

## <a name="quickstart">Quick Start</a>
For a Quick Start Tutorial, please refer to [Quick Start](https://github.com/checkmarx-ltd/cx-flow/wiki/Tutorials#quickstart)

## <a name="configuration">Configuration Details</a>
For Configuration details, please refer to [Configuration Definitions](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration#configuration-definitions)

## <a name="monitoring">Monitoring</a>
CxFlow has Spring Actuator built in and enabled for monitoring purposes:
https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html