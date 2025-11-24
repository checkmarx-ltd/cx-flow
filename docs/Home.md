<img src="https://raw.githubusercontent.com/Checkmarx/ci-cd-integrations/main/.images/PluginBanner.jpg">
<br />
<div align="center">

![CircleCI](https://img.shields.io/circleci/build/github/checkmarx-ltd/cx-flow)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/checkmarx-ltd/cx-flow)
![Docker Pulls](https://img.shields.io/docker/pulls/checkmarx/cx-flow)

<br />
<p align="center">
  <a href="https://github.com/checkmarx-ltd/cx-flow">
    <img src="https://raw.githubusercontent.com/Checkmarx/ci-cd-integrations/main/.images/cx_logo.svg" alt="Logo" width="80" height="80" />
  </a>

<h3 align="center">CxFlow</h3>

</div>

## <a name="overview">Overview</a>
CxFlow is a Spring Boot application that can run anywhere Java is installed. CxFlow glues together Checkmarx CxSAST and CxSCA scans with feedback to issue tracking systems via webhooks triggered by SCM events. 

CxFlow can also run as a CLI tool embedded in CI/CD pipelines. 

## <a name="quickstart">Quick Start</a>
For a Quick Start Tutorial, please refer to [Quick Start](https://github.com/checkmarx-ltd/cx-flow/wiki/Tutorials#quickstart)

## <a name="configuration">Configuration Details</a>
For Configuration details, please refer to [Configuration Definitions](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration#configuration-definitions)

## <a name="monitoring">Monitoring</a>
CxFlow has Spring Actuator built in and enabled for monitoring purposes:
https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html