package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.exception.PullRequestCommentUnknownException;
import com.checkmarx.flow.utils.MarkDownHelper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PullRequestCommentsHelper {

    public static final String COMMENT_TYPE_SAST_FINDINGS_2 = "Violation Summary";
    public static final String COMMENT_TYPE_SCA_FINDINGS = "Cx-SCA vulnerability result overview";

    private static final String COMMENT_TYPE_AST_FINDINGS_DETAILS = MarkDownHelper.AST_DETAILS_HEADER;
    private static final String SAST_SUMMARY_HEADER = MarkDownHelper.SAST_SUMMARY_HEADER;
    private static final String COMMENT_TYPE_SAST_SCAN_STARTED = "Scan submitted to Checkmarx";
    private static final String COMMENT_TYPE_SAST_FINDINGS_1 = MarkDownHelper.SCAN_SUMMARY_DETAILS;
    private static final String COMMENT_TYPE_SAST_SCAN_NOT_SUBMITTED = "Scan not submitted to Checkmarx due to existing Active scan for the same project.";

    public static boolean isCheckMarxComment(RepoComment comment) {
        String currentComment = comment.getComment();
        return currentComment.contains(COMMENT_TYPE_SAST_FINDINGS_2) && currentComment.contains(COMMENT_TYPE_SAST_FINDINGS_1) ||
                currentComment.contains(COMMENT_TYPE_SAST_SCAN_STARTED) ||
                currentComment.contains(COMMENT_TYPE_SAST_SCAN_NOT_SUBMITTED) ||
                currentComment.contains(COMMENT_TYPE_SCA_FINDINGS) ||
                currentComment.contains(COMMENT_TYPE_AST_FINDINGS_DETAILS) ||
                currentComment.contains(SAST_SUMMARY_HEADER);
    }

    public static RepoComment getCommentToUpdate(List<RepoComment> existingComments, String newComment) {
        CommentType commentType = getCommentType(newComment);
        List<RepoComment> relevantComments= getCheckmarxCommentsForType(existingComments, commentType);
        if (relevantComments.size() == 1) {
            return relevantComments.get(0);
        }
        return null;
    }

    private static CommentType getCommentType(String comment) {
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
        if (isScanNotSubmittedComment(comment)) {
            return CommentType.SCAN_NOT_SUBMITTED;
        }
        throw new PullRequestCommentUnknownException("Unknown comment type. content: " + comment);
    }


    private static boolean isCommentType(String comment, CommentType type) {
        return type.getTexts().stream()
                .anyMatch(comment::contains);
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

    public static boolean isScanNotSubmittedComment(String comment) {
        return isCommentType(comment, CommentType.SCAN_NOT_SUBMITTED);
    }

    private static List<RepoComment> getCheckmarxCommentsForType(List<RepoComment> allComments, CommentType commentType) {
        List<RepoComment> result = new ArrayList<>();
        for (RepoComment comment: allComments) {
            if (getCommentType(comment.getComment()).equals(commentType)) {
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
        SCA_AND_SAST(Arrays.asList(COMMENT_TYPE_SAST_FINDINGS_1, COMMENT_TYPE_SAST_FINDINGS_2, COMMENT_TYPE_SCA_FINDINGS)),
        SCAN_NOT_SUBMITTED(Arrays.asList(COMMENT_TYPE_SAST_SCAN_NOT_SUBMITTED));

        CommentType(List<String> texts) {
            this.texts = texts;
        }

        @Getter
        private List<String> texts;
    }
}