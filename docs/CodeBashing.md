## Integration with CodeBashing

CxFlow adds CodeBashing link when creating a Bug-Tracker ticket.
The link to CodeBasing is added under '**Training**' in the ticket recommendation:

[[/Images/Training_link.png|Training_Link]]

By default cxflow puts codebashing-url value in the link url
```
cx-flow:
  codebash-url: https:...
```

Now each customer (CodeBashing tenant), can use cxflow to add training link to his tickets - with direct link to relevant course per language and CWE!
The link will direct to the specific lesson under the customer CodeBashing account.
for example:  (https://cxflow-account.codebashing.com/courses/backend_java/lessons/sql_injection)


To get direct CodeBashing integration and connect ot to  your own tenant, add the following section to cxflow configuration:

 ```
codebashing:
  codebashingApiUrl: https://api.codebashing.com/lessons
  tenantBaseUrl: https://xxxxx.codebashing.com
  apiSecret: SECRET
 ```


 