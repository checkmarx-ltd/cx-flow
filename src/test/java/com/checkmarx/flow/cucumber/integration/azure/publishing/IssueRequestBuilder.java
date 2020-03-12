package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class IssueRequestBuilder {
    public List<CreateWorkItemAttr> getEntityBodyForCreation(Issue issue) {
        CreateWorkItemAttr title = getTitle(issue);
        CreateWorkItemAttr description = getDescription(issue);
        CreateWorkItemAttr state = getState(issue.getState());
        CreateWorkItemAttr tags = getTags(issue);
        return Arrays.asList(title, description, state, tags);
    }

    public List<CreateWorkItemAttr> getEntityBodyForUpdate(String newState) {
        return Collections.singletonList(getState(newState));
    }

    private CreateWorkItemAttr getTitle(Issue issue) {
        CreateWorkItemAttr title = new CreateWorkItemAttr();
        title.setOp("add");
        title.setPath("/fields/System.Title");
        title.setValue(issue.getTitle());
        return title;
    }

    private CreateWorkItemAttr getDescription(Issue issue) {
        CreateWorkItemAttr description = new CreateWorkItemAttr();
        description.setOp("add");
        description.setPath("/fields/System.Description");
        description.setValue(issue.getBody());
        return description;
    }

    private CreateWorkItemAttr getState(String stateValue) {
        CreateWorkItemAttr state = new CreateWorkItemAttr();
        state.setOp("add");
        state.setPath("/fields/System.State");
        state.setValue(stateValue);
        return state;
    }

    private CreateWorkItemAttr getTags(Issue issue) {
        CreateWorkItemAttr result = new CreateWorkItemAttr();
        result.setOp("add");
        result.setPath("/fields/Tags");

        String projectName = issue.getMetadata().get(AzureDevopsClient.PROJECT_NAME_KEY);
        String tags = String.format("CX,owner:%1$s,repo:%1$s,branch:%2$s", projectName, AzureDevopsClient.DEFAULT_BRANCH);
        result.setValue(tags);
        return result;
    }
}
