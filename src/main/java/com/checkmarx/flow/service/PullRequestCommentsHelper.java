package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.RepoComment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PullRequestCommentsHelper {

    private static final String COMMENT_TYPE_SCAN_STARTED = "Scan submitted to Checkmarx";
    private static final String COMMENT_TYPE_FINDINGS_1 = "Checkmarx SAST Scan Summary";
    private static final String COMMENT_TYPE_FINDINGS_2 = "Full Scan Details";

    public static boolean isCheckMarxComment(RepoComment comment) {
        return comment.getComment().contains(COMMENT_TYPE_FINDINGS_2) && comment.getComment().contains(COMMENT_TYPE_FINDINGS_1) ||
                comment.getComment().contains(COMMENT_TYPE_SCAN_STARTED);
    }

    public static RepoComment getCommentToUpdate(List<RepoComment> exisitingComments, String newComment) {
        CommentType commnetType = getCommnetType(newComment);
        List<RepoComment> relevantComments= getCheckmarxCommentsForType(exisitingComments, commnetType);
        if (relevantComments.size() == 1) {
            return relevantComments.get(0);
        }
        return null;
    }

    private static CommentType getCommnetType(String newComment) {
        if (newComment.contains(COMMENT_TYPE_SCAN_STARTED)) {
            return CommentType.SCAN_STARTED;
        }
        if (newComment.contains(COMMENT_TYPE_FINDINGS_2) &&
                newComment.contains(COMMENT_TYPE_FINDINGS_1)) {
            return CommentType.FINDINGS;
        }
        return CommentType.UNKNOWN;
    }

    private static List<RepoComment> getCheckmarxCommentsForType(List<RepoComment> allComments, CommentType commentType) {
        if (commentType == CommentType.SCAN_STARTED) {
            return allComments.stream().filter(rc -> rc.getComment().contains(COMMENT_TYPE_SCAN_STARTED)).collect(Collectors.toList());
        }
        else if (commentType == CommentType.FINDINGS) {
            return allComments.stream().filter(rc ->
                    (rc.getComment().contains(COMMENT_TYPE_FINDINGS_1) && rc.getComment().contains(COMMENT_TYPE_FINDINGS_2))).collect(Collectors.toList());
        }
        // We are not supposed to go in here at all.
        return new ArrayList<>();
    }

    public static boolean shouldUpdateComment(String newComment, String oldComment) {
        if (newComment == null) {
            if (oldComment == null) {
                return false;
            }
            return true;
        }
        return !newComment.equals(oldComment);
    }

    enum CommentType {
        SCAN_STARTED,
        FINDINGS,
        UNKNOWN;
    }
}
