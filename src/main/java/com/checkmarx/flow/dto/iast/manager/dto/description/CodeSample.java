package com.checkmarx.flow.dto.iast.manager.dto.description;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yevgeny Kuznetsov
 * @since 3.6, 20 May 2020
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSample {

    private String title;

    private String sourceCodeExample;

    private String programmingLanguage;

    private Boolean vulnerable;
}
