
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "subscriptionId",
    "notificationId",
    "id",
    "eventType",
    "publisherId",
    "message",
    "detailedMessage",
    "resource",
    "resourceVersion",
    "resourceContainers",
    "createdDate"
})
public class PullEvent {

    @JsonProperty("subscriptionId")
    private String subscriptionId;
    @JsonProperty("notificationId")
    private Integer notificationId;
    @JsonProperty("id")
    private String id;
    @JsonProperty("eventType")
    private String eventType;
    @JsonProperty("publisherId")
    private String publisherId;
    @JsonProperty("message")
    private Message message;
    @JsonProperty("detailedMessage")
    private DetailedMessage detailedMessage;
    @JsonProperty("resource")
    private Resource resource;
    @JsonProperty("resourceVersion")
    private String resourceVersion;
    @JsonProperty("resourceContainers")
    private ResourceContainers resourceContainers;
    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("subscriptionId")
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @JsonProperty("subscriptionId")
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @JsonProperty("notificationId")
    public Integer getNotificationId() {
        return notificationId;
    }

    @JsonProperty("notificationId")
    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("eventType")
    public String getEventType() {
        return eventType;
    }

    @JsonProperty("eventType")
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @JsonProperty("publisherId")
    public String getPublisherId() {
        return publisherId;
    }

    @JsonProperty("publisherId")
    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    @JsonProperty("message")
    public Message getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(Message message) {
        this.message = message;
    }

    @JsonProperty("detailedMessage")
    public DetailedMessage getDetailedMessage() {
        return detailedMessage;
    }

    @JsonProperty("detailedMessage")
    public void setDetailedMessage(DetailedMessage detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    @JsonProperty("resource")
    public Resource getResource() {
        return resource;
    }

    @JsonProperty("resource")
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @JsonProperty("resourceVersion")
    public String getResourceVersion() {
        return resourceVersion;
    }

    @JsonProperty("resourceVersion")
    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    @JsonProperty("resourceContainers")
    public ResourceContainers getResourceContainers() {
        return resourceContainers;
    }

    @JsonProperty("resourceContainers")
    public void setResourceContainers(ResourceContainers resourceContainers) {
        this.resourceContainers = resourceContainers;
    }

    @JsonProperty("createdDate")
    public String getCreatedDate() {
        return createdDate;
    }

    @JsonProperty("createdDate")
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

}
