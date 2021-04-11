# CxConfigProvider
CxConfigProvider is a Configuration library used by CxFlow.  It reads configurations in a JSON or YAML formats from .checkmarx folder in the root of a git repository.

## Overview  
* Implemented in plain Java
* Supports files in 2 formats: JSON and a YAML.
* Can load files from .checkmarx folder in the root of a git repository.
* Support for nesting configurations
* Can resolve parameters inside the configuration

## Special notes on YAML
```yaml
github:
  token: ${GITHUB_TOKEN}
  configAsCode: cx.configuration
  
ast:
  apiUrl: "http://this.is.just.an.example"
  token: ${AST_TOKEN}
  preset: true
  incremental: false
```
Unlike normal yaml files, the configuration can have variables.  The variables are resolved as follow:

* ${XXX} - will be resolved to the system environment variable with the name XXX. ex. ${AST_TOKEN} 
* ${path.in.config} - will be resolved to the value inside the configuration. ex. ${ast.preset} is true
* \\"TEST\\" - TEST is a value that should not be resolved. ex. \\"this is a normal text with special characters like $ and { \\"

## Special notes on JSON
```JSON
{
    "github": {
        "token": ${GITHUB_TOKEN},
        "configAsCode": "cx.configuration"
    },
    "ast": {
        "apiUrl": "http://this.is.just.an.example",
        "token": ${AST_TOKEN},
        "preset": true,
        "incremental": false
    }
}
```
Unlike normal json files, the configuration is [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) and can have variables.  The variables are resolved, if the value is not quoted as follow:
* ${XXX} - will be resolved to the system environment variable with the name XXX. ex. ${AST_TOKEN} 
* ${path.in.config} - will be resolved to the value inside the configuration. ex. ${ast.preset} is true  

_note:_  if the value is quoted, the variables will not be resolved. ex. "this is a normal text with special characters like $ and { "

For more information visit [Cx-ConfigProvider](https://github.com/checkmarx-ltd/cx-config-provider)