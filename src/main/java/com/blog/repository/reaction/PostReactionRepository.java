package com.blog.repository.reaction;

import com.blog.entity.reaction.PostReaction;
import com.blog.entity.reaction.PostReactionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, PostReactionId> {

    long countByPost_Id(Long postId);

    Optional<PostReaction> findByPost_IdAndUser_Id(Long postId, Long userId);

    void deleteByPost_IdAndUser_Id(Long postId, Long userId);
}
