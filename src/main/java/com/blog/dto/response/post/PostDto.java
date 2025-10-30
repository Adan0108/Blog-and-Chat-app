package com.blog.dto.response.post;

import com.blog.entity.blog.Visibility;
import java.time.Instant;
import java.util.List;

public class PostDto {
    public Long id;
    public Long authorId;
    public String authorUsername;
    public String content;
    public Visibility visibility;
    public List<String> images;
    public Instant createdAt;
    public long reactions;
    public long comments;
}
