package com.checkmarx.flow.dto.iast.manager.dto;

import lombok.Data;

import java.util.List;

@Data
public class Page<T> {
    public static final String PAGE_NUMBER_PARAM_DESCRIPTION = "The page number inside the pagination - starts from 0";
    public static final String PAGE_SIZE_PARAM_DESCRIPTION =
            "The size of the page - the maximum number of items in the requested page";

    private int pageNumber;
    private int pageSize;
    private int totalPages;

    private List<T> content;
}
