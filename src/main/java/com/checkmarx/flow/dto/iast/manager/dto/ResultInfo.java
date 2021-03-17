package com.checkmarx.flow.dto.iast.manager.dto;

import com.checkmarx.flow.dto.iast.common.model.enums.ManagementResultState;
import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "date")
public class ResultInfo implements Comparable<ResultInfo> {

    private Long resultId;

    private String name;

    private String url;

    private Instant date;

    private Severity severity;

    private HttpMethod httpMethod;

    private boolean newResult;

    private ResolutionStatus resolved;

    private boolean isCorrelated;

    private ManagementResultState resultState;

    private ManagementResultState suggestedResultState;

    private boolean assignedToUser;

    @JsonIgnore
    private Long projectId;

    private List<ConnectionResultsDetails> connectionResultsDetails;

    private Integer cwe;

    @Override
    public int compareTo(ResultInfo o) {
        int resultSeverity = o.severity.compareTo(severity);
        final int resultName = name.compareTo(o.name);
        return (resultSeverity != 0) ? resultSeverity : ((resultName != 0) ? resultName : o.getDate().compareTo(date));
    }

}
