package com.checkmarx.flow.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "self",
        "meta",
        "history"
})
public class Links_ {

    @JsonProperty("self")
    private Self self;
    @JsonProperty("meta")
    private Meta meta;
    @JsonProperty("history")
    private History history;

    @JsonProperty("self")
    public Self getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(Self self) {
        this.self = self;
    }

    @JsonProperty("meta")
    public Meta getMeta() {
        return meta;
    }

    @JsonProperty("meta")
    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    @JsonProperty("history")
    public History getHistory() {
        return history;
    }

    @JsonProperty("history")
    public void setHistory(History history) {
        this.history = history;
    }

}