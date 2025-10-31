package com.blog.dto.response.comment;

import java.time.Instant;

public class CommentDto {
    public Long id;
    public Long postId;
    public Long authorId;
    public String authorUsername;
    public String text;
    public Instant createdAt;
    public Long parentId;
}

