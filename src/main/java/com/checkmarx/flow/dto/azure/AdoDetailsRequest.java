package com.checkmarx.flow.dto.azure;

import lombok.*;

/**
 * Represents ADO-specific parameters that are passed to {@link com.checkmarx.flow.controller.ADOController}.
 * All the parameters are optional.
 *
 * Field names are quite awkward but have to be kept for backward compatibility, unless we find a better solution.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdoDetailsRequest {
    // Issue type
    private String adoIssue;

    // Issue body
    private String adoBody;

    // Opened state
    private String adoOpened;

    // Closed state
    private String adoClosed;
}
