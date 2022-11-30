
package com.checkmarx.flow.jira9X;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;



@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "size",
        "max-results",
        "items"
})
@Generated("jsonschema2pojo")
public class SimpleListWrapper {

    @JsonProperty("size")
    private Integer size;
    @JsonProperty("max-results")
    private Integer maxResults;
    @JsonProperty("items")
    private List<Item> items = null;

    @JsonProperty("size")
    public Integer getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(Integer size) {
        this.size = size;
    }

    @JsonProperty("max-results")
    public Integer getMaxResults() {
        return maxResults;
    }

    @JsonProperty("max-results")
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    @JsonProperty("items")
    public List<Item> getItems() {
        return items;
    }

    @JsonProperty("items")
    public void setItems(List<Item> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SimpleListWrapper.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("size");
        sb.append('=');
        sb.append(((this.size == null)?"<null>":this.size));
        sb.append(',');
        sb.append("maxResults");
        sb.append('=');
        sb.append(((this.maxResults == null)?"<null>":this.maxResults));
        sb.append(',');
        sb.append("items");
        sb.append('=');
        sb.append(((this.items == null)?"<null>":this.items));
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
        result = ((result* 31)+((this.size == null)? 0 :this.size.hashCode()));
        result = ((result* 31)+((this.items == null)? 0 :this.items.hashCode()));
        result = ((result* 31)+((this.maxResults == null)? 0 :this.maxResults.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SimpleListWrapper) == false) {
            return false;
        }
        SimpleListWrapper rhs = ((SimpleListWrapper) other);
        return ((((this.size == rhs.size)||((this.size!= null)&&this.size.equals(rhs.size)))&&((this.items == rhs.items)||((this.items!= null)&&this.items.equals(rhs.items))))&&((this.maxResults == rhs.maxResults)||((this.maxResults!= null)&&this.maxResults.equals(rhs.maxResults))));
    }

}
