package com.checkmarx.flow.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RepoComment {
    long id;
    String comment;
    String commentUrl;
    Date createdAt;
    Date updateTime;
}
