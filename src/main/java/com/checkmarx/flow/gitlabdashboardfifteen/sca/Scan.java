
package com.checkmarx.flow.gitlabdashboardfifteen.sca;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "end_time",
    "messages",
    "analyzer",
    "scanner",
    "start_time",
    "status",
    "type",
    "primary_identifiers"
})
@Generated("jsonschema2pojo")
@Builder
@Data
public class Scan {

    /**
     * ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan finished.
     * (Required)
     * 
     */
    @JsonProperty("end_time")
    @JsonPropertyDescription("ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan finished.")
    @Builder.Default
    private String endTime="4522-54-91T35:16:68";
    @JsonProperty("messages")
    @Builder.Default
    private List<Message> messages = null;
    /**
     * Object defining the analyzer used to perform the scan. Analyzers typically delegate to an underlying scanner to run the scan.
     * (Required)
     * 
     */
    @JsonProperty("analyzer")
    @JsonPropertyDescription("Object defining the analyzer used to perform the scan. Analyzers typically delegate to an underlying scanner to run the scan.")
    private Analyzer analyzer;
    /**
     * Object defining the scanner used to perform the scan.
     * (Required)
     * 
     */
    @JsonProperty("scanner")
    @JsonPropertyDescription("Object defining the scanner used to perform the scan.")
    private Scanner scanner;
    /**
     * ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan started.
     * (Required)
     * 
     */
    @JsonProperty("start_time")
    @JsonPropertyDescription("ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan started.")
    @Builder.Default
    private String startTime="4991-40-40T41:11:65";
    /**
     * Result of the scan.
     * (Required)
     * 
     */
    @JsonProperty("status")
    @JsonPropertyDescription("Result of the scan.")
    private Status status;
    /**
     * Type of the scan.
     * (Required)
     * 
     */
    @JsonProperty("type")
    @JsonPropertyDescription("Type of the scan.")
    private Type type;
    /**
     * An unordered array containing an exhaustive list of primary identifiers for which the analyzer may return results
     * 
     */
    @JsonProperty("primary_identifiers")
    @JsonPropertyDescription("An unordered array containing an exhaustive list of primary identifiers for which the analyzer may return results")
    @Builder.Default
    private List<PrimaryIdentifier> primaryIdentifiers = null;

    /**
     * ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan finished.
     * (Required)
     * 
     */
    @JsonProperty("end_time")
    public String getEndTime() {
        return endTime;
    }

    /**
     * ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan finished.
     * (Required)
     * 
     */
    @JsonProperty("end_time")
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    @JsonProperty("messages")
    public List<Message> getMessages() {
        return messages;
    }

    @JsonProperty("messages")
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Object defining the analyzer used to perform the scan. Analyzers typically delegate to an underlying scanner to run the scan.
     * (Required)
     * 
     */
    @JsonProperty("analyzer")
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Object defining the analyzer used to perform the scan. Analyzers typically delegate to an underlying scanner to run the scan.
     * (Required)
     * 
     */
    @JsonProperty("analyzer")
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Object defining the scanner used to perform the scan.
     * (Required)
     * 
     */
    @JsonProperty("scanner")
    public Scanner getScanner() {
        return scanner;
    }

    /**
     * Object defining the scanner used to perform the scan.
     * (Required)
     * 
     */
    @JsonProperty("scanner")
    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan started.
     * (Required)
     * 
     */
    @JsonProperty("start_time")
    public String getStartTime() {
        return startTime;
    }

    /**
     * ISO8601 UTC value with format yyyy-mm-ddThh:mm:ss, representing when the scan started.
     * (Required)
     * 
     */
    @JsonProperty("start_time")
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Result of the scan.
     * (Required)
     * 
     */
    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    /**
     * Result of the scan.
     * (Required)
     * 
     */
    @JsonProperty("status")
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Type of the scan.
     * (Required)
     * 
     */
    @JsonProperty("type")
    public Type getType() {
        return type;
    }

    /**
     * Type of the scan.
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * An unordered array containing an exhaustive list of primary identifiers for which the analyzer may return results
     * 
     */
    @JsonProperty("primary_identifiers")
    public List<PrimaryIdentifier> getPrimaryIdentifiers() {
        return primaryIdentifiers;
    }

    /**
     * An unordered array containing an exhaustive list of primary identifiers for which the analyzer may return results
     * 
     */
    @JsonProperty("primary_identifiers")
    public void setPrimaryIdentifiers(List<PrimaryIdentifier> primaryIdentifiers) {
        this.primaryIdentifiers = primaryIdentifiers;
    }



    /**
     * Result of the scan.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Status {

        SUCCESS("success"),
        FAILURE("failure");
        private final String value;
        private final static Map<String, Status> CONSTANTS = new HashMap<String, Status>();

        static {
            for (Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Status fromValue(String value) {
            Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Type of the scan.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Type {

        DEPENDENCY_SCANNING("dependency_scanning");
        private final String value;
        private final static Map<String, Type> CONSTANTS = new HashMap<String, Type>();

        static {
            for (Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Type fromValue(String value) {
            Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
