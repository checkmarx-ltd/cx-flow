
package com.checkmarx.flow.gitdashboardnewverfifteen.SCA;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;


/**
 * Communication intended for the initiator of a scan.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "level",
        "value"
})
@Generated("jsonschema2pojo")
public class Message {

    /**
     * Describes the severity of the communication. Use info to communicate normal scan behaviour; warn to communicate a potentially recoverable problem, or a partial error; fatal to communicate an issue that causes the scan to halt.
     * (Required)
     *
     */
    @JsonProperty("level")
    @JsonPropertyDescription("Describes the severity of the communication. Use info to communicate normal scan behaviour; warn to communicate a potentially recoverable problem, or a partial error; fatal to communicate an issue that causes the scan to halt.")
    private Level level;
    /**
     * The message to communicate.
     * (Required)
     *
     */
    @JsonProperty("value")
    @JsonPropertyDescription("The message to communicate.")
    private String value;


    /**
     * Describes the severity of the communication. Use info to communicate normal scan behaviour; warn to communicate a potentially recoverable problem, or a partial error; fatal to communicate an issue that causes the scan to halt.
     * (Required)
     *
     */
    @JsonProperty("level")
    public Level getLevel() {
        return level;
    }

    /**
     * Describes the severity of the communication. Use info to communicate normal scan behaviour; warn to communicate a potentially recoverable problem, or a partial error; fatal to communicate an issue that causes the scan to halt.
     * (Required)
     *
     */
    @JsonProperty("level")
    public void setLevel(Level level) {
        this.level = level;
    }

    /**
     * The message to communicate.
     * (Required)
     *
     */
    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    /**
     * The message to communicate.
     * (Required)
     *
     */
    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }




    /**
     * Describes the severity of the communication. Use info to communicate normal scan behaviour; warn to communicate a potentially recoverable problem, or a partial error; fatal to communicate an issue that causes the scan to halt.
     *
     */
    @Generated("jsonschema2pojo")
    public enum Level {

        INFO("info"),
        WARN("warn"),
        FATAL("fatal");
        private final String value;
        private final static Map<String, Level> CONSTANTS = new HashMap<String, Level>();

        static {
            for (Level c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Level(String value) {
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
        public static Level fromValue(String value) {
            Level constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
