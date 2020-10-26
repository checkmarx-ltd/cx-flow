
Cx-Flow supports multiple SCM’s accounts: GitHub, GitLab & Azure Devops (instances)

[[/Images/github_multi_instance.png|thresholds screenshot]]


<u>**Configuration changes required**</u> – via configuration file

Optional instances are configured under each SCM section (e.g. GitHub)

[[/Images/multi_instance_example.png|thresholds screenshot]]


The supported properties which can be override are:
* webhook-token
* token
* url
* api-url


#### <u>Default properties are getting overridden with optional instance properties</u>
In order to override the default SCM’s properties with an optional properties, a webhook **scm-instance** query parameter should be set on the webhook’s **Payload URL**:
[[/Images/multi_instance_url_payload.png|thresholds screenshot]]

* In this example we are settings the scm-instance parameter key with **‘instance2’** value.
* In case the ‘instance2’ name do exists on the configuration file, the default SCM configuration will be overridden with the ‘instance2’ properties.
* In case a not exists scm-instance is defined an exception will be thrown.
* In case none scm-instance was defined as a webhook query parameter, the default configuration will be used.


