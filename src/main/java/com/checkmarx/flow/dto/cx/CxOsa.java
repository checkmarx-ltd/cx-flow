package com.checkmarx.flow.dto.cx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "cveName",
        "score",
        "severity",
        "publishDate",
        "url",
        "description",
        "recommendations",
        "sourceFileName",
        "libraryId",
        "state",
        "commentsAmount"
})
public class CxOsa {

    @JsonProperty("id")
    public String id;
    @JsonProperty("cveName")
    public String cveName;
    @JsonProperty("score")
    public Double score;
    @JsonProperty("severity")
    public Severity severity;
    @JsonProperty("publishDate")
    public String publishDate;
    @JsonProperty("url")
    public String url;
    @JsonProperty("description")
    public String description;
    @JsonProperty("recommendations")
    public String recommendations;
    @JsonProperty("sourceFileName")
    public String sourceFileName;
    @JsonProperty("libraryId")
    public String libraryId;
    @JsonProperty("state")
    public State state;
    @JsonProperty("commentsAmount")
    public String commentsAmount;

    public String getId() {
        return this.id;
    }

    public String getCveName() {
        return this.cveName;
    }

    public Double getScore() {
        return this.score;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public String getPublishDate() {
        return this.publishDate;
    }

    public String getUrl() {
        return this.url;
    }

    public String getDescription() {
        return this.description;
    }

    public String getRecommendations() {
        return this.recommendations;
    }

    public String getSourceFileName() {
        return this.sourceFileName;
    }

    public String getLibraryId() {
        return this.libraryId;
    }

    public State getState() {
        return this.state;
    }

    public String getCommentsAmount() {
        return this.commentsAmount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCveName(String cveName) {
        this.cveName = cveName;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setCommentsAmount(String commentsAmount) {
        this.commentsAmount = commentsAmount;
    }

    public static class State{
        @JsonProperty("id")
        public Integer id;
        @JsonProperty("name")
        public String name;
        @JsonProperty("actionType")
        public String actionType;

        public Integer getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String getActionType() {
            return this.actionType;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }
    }

    public static class Severity{
        @JsonProperty("name")
        public String name;
        @JsonProperty("id")
        public Integer id;

        public String getName() {
            return this.name;
        }

        public Integer getId() {
            return this.id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }
}
