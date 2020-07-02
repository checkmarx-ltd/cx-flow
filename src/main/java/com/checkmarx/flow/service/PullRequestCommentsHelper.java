package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.exception.PullRequestCommentUnknownException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PullRequestCommentsHelper {

    private static final String COMMENT_TYPE_SAST_SCAN_STARTED = "Scan submitted to Checkmarx";
    private static final String COMMENT_TYPE_SAST_FINDINGS_1 = "Checkmarx SAST Scan Summary";
    private static final String COMMENT_TYPE_SAST_FINDINGS_2 = "Full Scan Details";
    private static final String COMMENT_TYPE_SCA_FINDINGS = "CxSCA vulnerability result overview";

    public static boolean isCheckMarxComment(RepoComment comment) {
        return comment.getComment().contains(COMMENT_TYPE_SAST_FINDINGS_2) && comment.getComment().contains(COMMENT_TYPE_SAST_FINDINGS_1) ||
                comment.getComment().contains(COMMENT_TYPE_SAST_SCAN_STARTED)
                || comment.getComment().contains(COMMENT_TYPE_SCA_FINDINGS);
    }

    public static RepoComment getCommentToUpdate(List<RepoComment> exisitingComments, String newComment) {
        CommentType commnetType = getCommnetType(newComment);
        List<RepoComment> relevantComments= getCheckmarxCommentsForType(exisitingComments, commnetType);
        if (relevantComments.size() == 1) {
            return relevantComments.get(0);
        }
        return null;
    }

    private static CommentType getCommnetType(String comment) {
        if (isSastAndScaComment(comment)) {
            return CommentType.SCA_AND_SAST;
        }
        if (isScanStartedComment(comment)) {
            return CommentType.SCAN_STARTED;
        }
        if (isSastFindingsComment(comment)) {
            return CommentType.SAST_FINDINGS;
        }
        if (isScaComment(comment)) {
            return CommentType.SCA;
        }
        throw new PullRequestCommentUnknownException("Unknown comment type. content: " + comment);
    }


    private static boolean isCommentType(String comment, CommentType type) {
        for (String text: type.getTexts()) {
            if (!comment.contains(text)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isScaComment(String comment) {
        return isCommentType(comment, CommentType.SCA);
    }

    public static boolean isSastFindingsComment(String comment) {
        return  isCommentType(comment, CommentType.SAST_FINDINGS);
    }

    public static  boolean isSastAndScaComment(String comment) {
        return isCommentType(comment, CommentType.SCA_AND_SAST);
    }

    public static boolean isScanStartedComment(String comment) {
        return isCommentType(comment, CommentType.SCAN_STARTED);
    }

    private static List<RepoComment> getCheckmarxCommentsForType(List<RepoComment> allComments, CommentType commentType) {
        List<RepoComment> result = new ArrayList<>();
        for (RepoComment comment: allComments) {
            if (getCommnetType(comment.getComment()).equals(commentType)) {
                result.add(comment);
            }
        }
        // We are not supposed to go in here at all.
        return result;
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
        SCAN_STARTED(Arrays.asList(COMMENT_TYPE_SAST_SCAN_STARTED)),
        SAST_FINDINGS(Arrays.asList(COMMENT_TYPE_SAST_FINDINGS_1, COMMENT_TYPE_SAST_FINDINGS_2)),
        SCA(Arrays.asList(COMMENT_TYPE_SCA_FINDINGS)),
        SCA_AND_SAST(Arrays.asList(COMMENT_TYPE_SAST_FINDINGS_1, COMMENT_TYPE_SAST_FINDINGS_2, COMMENT_TYPE_SCA_FINDINGS));

        CommentType(List<String> texts) {
            this.texts = texts;
        }

        @Getter
        private List<String> texts;
    }
}
