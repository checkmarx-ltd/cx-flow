
package com.custodela.machina.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "previous",
    "current"
})
public class Labels {

    @JsonProperty("previous")
    @Valid
    private List<Previou> previous = null;
    @JsonProperty("current")
    @Valid
    private List<Current> current = null;

    @JsonProperty("previous")
    public List<Previou> getPrevious() {
        return previous;
    }

    @JsonProperty("previous")
    public void setPrevious(List<Previou> previous) {
        this.previous = previous;
    }

    public Labels withPrevious(List<Previou> previous) {
        this.previous = previous;
        return this;
    }

    @JsonProperty("current")
    public List<Current> getCurrent() {
        return current;
    }

    @JsonProperty("current")
    public void setCurrent(List<Current> current) {
        this.current = current;
    }

    public Labels withCurrent(List<Current> current) {
        this.current = current;
        return this;
    }

}
