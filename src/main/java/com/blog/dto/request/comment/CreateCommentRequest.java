package com.blog.dto.request.comment;

public record CreateCommentRequest(
        String text,
        Long parentId // nullable
) {}
