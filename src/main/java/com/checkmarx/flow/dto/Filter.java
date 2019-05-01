package com.checkmarx.flow.dto;

import java.beans.ConstructorProperties;
import java.util.Objects;


public class Filter {
    private Type type;
    private String value;

    @ConstructorProperties({"type", "value"})
    public Filter(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public static FilterBuilder builder() {
        return new FilterBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filter)) return false;
        Filter filter = (Filter) o;
        return getType().equals(filter.getType())  &&
                getValue().equalsIgnoreCase(filter.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getValue());
    }

    public Type getType() {
        return this.type;
    }

    public String getValue() {
        return this.value;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        return "Filter(type=" + this.getType() + ", value=" + this.getValue() + ")";
    }

    public enum Type {
        SEVERITY("SEVERITY"),
        CWE("CWE"),
        OWASP("OWASP"),
        TYPE("TYPE"),
        STATUS("STATUS");

        private String type;

        Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public enum Severity {
        CRITICAL("Critical"),
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low"),
        INFO("Informational");

        private String severity;

        Severity(String severity) {
            this.severity = severity;
        }

        public String getSeverity() {
            return severity;
        }
    }

    public static class FilterBuilder {
        private Type type;
        private String value;

        FilterBuilder() {
        }

        public Filter.FilterBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public Filter.FilterBuilder value(String value) {
            this.value = value;
            return this;
        }

        public Filter build() {
            return new Filter(type, value);
        }

        public String toString() {
            return "Filter.FilterBuilder(type=" + this.type + ", value=" + this.value + ")";
        }
    }
}
