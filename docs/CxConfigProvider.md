# CxConfigProvider
CxConfigProvider is a Configuration library used by CX-Flow,  
It reads configurations in a JSON or YAML formats from .checkmarx folder in a git repository

## Overview  
* implemented in plain Java
* supports files in 2 formats: JSON and a YAML.
* can load files from .checkmarx folder in the root of a git repository.
* support for nesting configurations
* can resolve parameters inside the configuration



## Special notes on YAML
```yaml
github:
  token: ${GITHUB_TOKEN}
  configAsCode: cx.configuration
  
ast:
  apiUrl: \"http://this.is.just.an.example\"
  token: ${AST_TOKEN}
  preset: true
  incremental: false
```
unlike normal yaml files, the configuration can have variables,  
The variables are resoved as follow:
* ${XXX} - will be resolved to the system environment variable with the name XXX. ex. ${AST_TOKEN} 
* ${path.in.config} - will be resolved to the value inside the configuration. ex. ${ast.preset} is true
* \\"TEST\\" - TEST is a value that sould not be resoved. ex. \\"this is a normal text with special characters like $ and { \\"

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
unlike normal json files, the configuration is [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) and can have variables,  
The variables are resoved, if the value is not quated as follow:
* ${XXX} - will be resolved to the system environment variable with the name XXX. ex. ${AST_TOKEN} 
* ${path.in.config} - will be resolved to the value inside the configuration. ex. ${ast.preset} is true  

_note:_  if the value is quated, the variables will not be resolved. ex. "this is a normal text with special characters like $ and { "

For more information visit [Cx-ConfigProvider](https://github.com/checkmarx-ltd/cx-config-provider)