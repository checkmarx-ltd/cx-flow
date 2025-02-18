package com.checkmarx.flow.dto.github.issue;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubIssuesSummary {

   @JsonProperty("total")
   int total;

   @JsonProperty("completed")
   int completed;

   @JsonProperty("percent_completed")
   int percentCompleted;


    public void setTotal(int total) {
        this.total = total;
    }
    public int getTotal() {
        return total;
    }
    
    public void setCompleted(int completed) {
        this.completed = completed;
    }
    public int getCompleted() {
        return completed;
    }
    
    public void setPercentCompleted(int percentCompleted) {
        this.percentCompleted = percentCompleted;
    }
    public int getPercentCompleted() {
        return percentCompleted;
    }
    
}