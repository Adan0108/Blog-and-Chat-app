package com.blog.dto.response.post;

public record ReactionDto(
        Long postId,
        Long userId,
        Short typeId,
        String typeName,
        String iconUrl,
        long totalCount
) { }
