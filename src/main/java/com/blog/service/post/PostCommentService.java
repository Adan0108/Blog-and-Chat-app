package com.blog.service.post;

import com.blog.dto.response.comment.CommentDto;
import com.blog.dto.response.comment.CommentPage;

import java.time.Instant;

public interface PostCommentService {
    CommentDto addComment(Long userId, Long postId, String text, Long parentId);
    CommentPage listComments(Long userId, Long postId, Instant cursor, int limit);
    CommentDto editComment(Long userId, Long commentId, String newText);
    void deleteComment(Long userId, Long commentId);
}
