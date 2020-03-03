package com.checkmarx.flow.cucumber.common.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Anything {

    public static final Anything INSTANCE = new Anything();

    @JsonCreator
    public static Anything anyObject() {
        return INSTANCE;
    }

    private Anything() {
    }

    public String getFakeField() {
        return null;
    }

    public void setFakeField(String fakeField) {
    }

    @Override
    public boolean equals(Object o) {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}