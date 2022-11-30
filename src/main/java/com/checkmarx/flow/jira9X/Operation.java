
package com.checkmarx.flow.jira9X;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "id",
        "styleClass",
        "iconClass",
        "label",
        "title",
        "href",
        "weight"
})
@Generated("jsonschema2pojo")
public class Operation {

    @JsonProperty("id")
    private String id;
    @JsonProperty("styleClass")
    private String styleClass;
    @JsonProperty("iconClass")
    private String iconClass;
    @JsonProperty("label")
    private String label;
    @JsonProperty("title")
    private String title;
    @JsonProperty("href")
    private String href;
    @JsonProperty("weight")
    private Integer weight;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("styleClass")
    public String getStyleClass() {
        return styleClass;
    }

    @JsonProperty("styleClass")
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    @JsonProperty("iconClass")
    public String getIconClass() {
        return iconClass;
    }

    @JsonProperty("iconClass")
    public void setIconClass(String iconClass) {
        this.iconClass = iconClass;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("href")
    public String getHref() {
        return href;
    }

    @JsonProperty("href")
    public void setHref(String href) {
        this.href = href;
    }

    @JsonProperty("weight")
    public Integer getWeight() {
        return weight;
    }

    @JsonProperty("weight")
    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Operation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("styleClass");
        sb.append('=');
        sb.append(((this.styleClass == null)?"<null>":this.styleClass));
        sb.append(',');
        sb.append("iconClass");
        sb.append('=');
        sb.append(((this.iconClass == null)?"<null>":this.iconClass));
        sb.append(',');
        sb.append("label");
        sb.append('=');
        sb.append(((this.label == null)?"<null>":this.label));
        sb.append(',');
        sb.append("title");
        sb.append('=');
        sb.append(((this.title == null)?"<null>":this.title));
        sb.append(',');
        sb.append("href");
        sb.append('=');
        sb.append(((this.href == null)?"<null>":this.href));
        sb.append(',');
        sb.append("weight");
        sb.append('=');
        sb.append(((this.weight == null)?"<null>":this.weight));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.weight == null)? 0 :this.weight.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.label == null)? 0 :this.label.hashCode()));
        result = ((result* 31)+((this.href == null)? 0 :this.href.hashCode()));
        result = ((result* 31)+((this.styleClass == null)? 0 :this.styleClass.hashCode()));
        result = ((result* 31)+((this.title == null)? 0 :this.title.hashCode()));
        result = ((result* 31)+((this.iconClass == null)? 0 :this.iconClass.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Operation) == false) {
            return false;
        }
        Operation rhs = ((Operation) other);
        return ((((((((this.weight == rhs.weight)||((this.weight!= null)&&this.weight.equals(rhs.weight)))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.label == rhs.label)||((this.label!= null)&&this.label.equals(rhs.label))))&&((this.href == rhs.href)||((this.href!= null)&&this.href.equals(rhs.href))))&&((this.styleClass == rhs.styleClass)||((this.styleClass!= null)&&this.styleClass.equals(rhs.styleClass))))&&((this.title == rhs.title)||((this.title!= null)&&this.title.equals(rhs.title))))&&((this.iconClass == rhs.iconClass)||((this.iconClass!= null)&&this.iconClass.equals(rhs.iconClass))));
    }

}
