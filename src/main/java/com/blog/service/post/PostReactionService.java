package com.blog.service.post;

import com.blog.dto.response.post.ReactionDto;

public interface PostReactionService {
    ReactionDto react(Long userId, Long postId, short reactionTypeId);
    void unreact(Long userId, Long postId);
}