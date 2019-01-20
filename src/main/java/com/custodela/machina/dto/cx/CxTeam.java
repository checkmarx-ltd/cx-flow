package com.custodela.machina.dto.cx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "fullName"
})
public class CxTeam {

    @JsonProperty("id")
    public String id;
    @JsonProperty("fullName")
    public String fullName;

    public CxTeam() {
    }

    public String getId() {
        return this.id;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String toString() {
        return "CxTeam(id=" + this.getId() + ", fullName=" + this.getFullName() + ")";
    }
}