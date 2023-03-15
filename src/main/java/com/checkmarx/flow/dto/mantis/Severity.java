package com.checkmarx.flow.dto.mantis;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Severity {
    @JsonProperty("name")
    private String name;

    @Override
    public String toString() {
        return "Severity{" +
                "name ='" + name + '\'' +
                '}';
    }
}
