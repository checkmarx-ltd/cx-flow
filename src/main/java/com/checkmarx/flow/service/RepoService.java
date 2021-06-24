package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.sdk.dto.sast.CxConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class RepoService {
    public abstract Sources getRepoContent(ScanRequest request);

    public CxConfig getCxConfigOverride(ScanRequest request) {
        return null;
    }

    public abstract void deleteComment(String url, ScanRequest scanRequest);

    public abstract void updateComment(String commentUrl, String comment, ScanRequest scanRequest);

    public abstract void addComment(ScanRequest scanRequest, String comment);

    public abstract List<RepoComment> getComments(ScanRequest scanRequest) throws IOException;

    public void sendMergeComment(ScanRequest request, String comment){

        try {
            RepoComment commentToUpdate =
                    PullRequestCommentsHelper.getCommentToUpdate(getComments(request), comment);
            if (commentToUpdate !=  null) {
                log.debug("Got candidate comment to update. comment: {}", commentToUpdate.getComment());
                if (!PullRequestCommentsHelper.shouldUpdateComment(comment, commentToUpdate.getComment())) {
                    log.debug("sendMergeComment: Comment should not be updated");
                    return;
                }
                log.debug("sendMergeComment: Going to update {} pull request comment",
                        request.getRepoType());
                updateComment(commentToUpdate.getCommentUrl(), comment, request);
            } else {
                log.debug("sendMergeComment: Going to create a new {} pull request comment", request.getRepoType());
                addComment(request, comment);
            }
        } catch (Exception e) {
            // We "swallow" the exception so that the flow will not be terminated because of errors in GIT comments
            log.error("Error while adding or updating {} repo pull request comment",
                    request.getRepoType(), e);
        }

    }

    public abstract String createIssue(ScanRequest request,
                                       String title,
                                       String description,
                                       String assignee,
                                       String priority);
}
