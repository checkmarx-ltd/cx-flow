package com.custodela.machina.dto.cx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "name"
})
public class CxScanEngine {

    @JsonProperty("id")
    public Integer id;
    @JsonProperty("name")
    public String name;

    public CxScanEngine() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "CxScanEngine(id=" + this.getId() + ", name=" + this.getName() + ")";
    }
}