package com.checkmarx.flow.cucumber.common.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnyObject {

    public static final AnyObject INSTANCE = new AnyObject();

    @JsonCreator
    public static AnyObject anyObject(String fakeField) {
        return INSTANCE;
    }

    private AnyObject() {
    }

    public String getFakeField() {
        return null;
    }

    public void setFakeField(String fakeField) {
    }

    @Override
    public boolean equals(Object o) {
        return o != null;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
