package com.checkmarx.flow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

@JsonInclude(Include.NON_NULL)
public class EventResponse {

    @JsonProperty("success")
    @NotNull
    private Boolean success;

    @JsonProperty("id")
    private String id;

    @JsonProperty("message")
    private String message;

    @JsonProperty("step")
    private String step;

    @ConstructorProperties({"success", "id", "message", "step"})
    EventResponse(@NotNull Boolean success, String id, String message, String step) {
        this.success = success;
        this.id = id;
        this.message = message;
        this.step = step;
    }

    public static EventResponseBuilder builder() {
        return new EventResponseBuilder();
    }

    public @NotNull Boolean getSuccess() {
        return this.success;
    }

    public String getId() {
        return this.id;
    }

    public String getMessage() {
        return this.message;
    }

    public String getStep() {
        return this.step;
    }

    public void setSuccess(@NotNull Boolean success) {
        this.success = success;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String toString() {
        return "EventResponse(success=" + this.getSuccess() + ", id=" + this.getId() + ", message=" + this.getMessage() + ", step=" + this.getStep() + ")";
    }

    public static class EventResponseBuilder {
        private @NotNull Boolean success;
        private String id;
        private String message;
        private String step;

        EventResponseBuilder() {
            this.success = false;
        }

        public EventResponse.EventResponseBuilder success(@NotNull Boolean success) {
            this.success = success;
            return this;
        }

        public EventResponse.EventResponseBuilder id(String id) {
            this.id = id;
            return this;
        }

        public EventResponse.EventResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public EventResponse.EventResponseBuilder step(String step) {
            this.step = step;
            return this;
        }

        public EventResponse build() {
            return new EventResponse(success, id, message, step);
        }

        public String toString() {
            return "EventResponse.EventResponseBuilder(success=" + this.success + ", id=" + this.id + ", message=" + this.message + ", step=" + this.step + ")";
        }
    }
}