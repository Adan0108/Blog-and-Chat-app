package com.blog.service.impl.post;

import com.blog.dto.response.post.ReactionDto;
import com.blog.entity.blog.Post;
import com.blog.entity.blog.Visibility;
import com.blog.entity.reaction.PostReaction;
import com.blog.entity.reaction.PostReactionId;
import com.blog.entity.reaction.ReactionType;
import com.blog.repository.blog.PostRepository;
import com.blog.repository.reaction.PostReactionRepository;
import com.blog.repository.reaction.ReactionTypeRepository;
import com.blog.repository.social.UserFollowersRepository;
import com.blog.repository.user.UserRepository;
import com.blog.service.post.PostReactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class PostReactionServiceImpl implements PostReactionService {

    private final PostRepository posts;
    private final UserRepository users;
    private final ReactionTypeRepository reactionTypes;
    private final PostReactionRepository reactions;
    private final UserFollowersRepository followers;

    public PostReactionServiceImpl(PostRepository posts,
                                   UserRepository users,
                                   ReactionTypeRepository reactionTypes,
                                   PostReactionRepository reactions,
                                   UserFollowersRepository followers) {
        this.posts = posts;
        this.users = users;
        this.reactionTypes = reactionTypes;
        this.reactions = reactions;
        this.followers = followers;
    }

    // ---------- reactions ----------
    @Override
    public ReactionDto react(Long userId, Long postId, short reactionTypeId) {
        if (userId == null) throw new IllegalStateException("Unauthenticated");

        var post = posts.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureCanReact(userId, post);

        ReactionType type = reactionTypes.findById(reactionTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Reaction type not found"));

        var existing = reactions.findByPost_IdAndUser_Id(postId, userId);
        if (existing.isPresent()) {
            var pr = existing.get();
            if (!pr.getType().getId().equals(type.getId())) {
                pr.setType(type);
            }
        } else {
            var pr = new PostReaction();
            pr.setId(new PostReactionId(postId, userId));
            pr.setPost(post);
            pr.setUser(user);
            pr.setType(type);
            reactions.save(pr);
        }

        long total = reactions.countByPost_Id(postId);
        return new ReactionDto(
                postId,
                userId,
                type.getId(),
                type.getName(),
                type.getIconUrl(),
                total
        );
    }

    @Override
    public void unreact(Long userId, Long postId) {
        if (userId == null) throw new IllegalStateException("Unauthenticated");
        var post = posts.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        ensureCanReact(userId, post); // keep parity with react()
        reactions.deleteByPost_IdAndUser_Id(postId, userId);
    }

    // --- visibility rules for reactions ---
    private void ensureCanReact(Long viewerId, Post post) {
        // Owner can always react (easy to change if you want owner blocked on PRIVATE—tell me)
        if (post.getAuthor() != null && post.getAuthor().getId().equals(viewerId)) return;

        Visibility v = post.getVisibility();
        switch (v) {
            case PUBLIC -> { /* anyone can react */ }
            case FRIENDS -> {
                Long ownerId = post.getAuthor().getId();
                if (!followers.isFriend(viewerId, ownerId)) {
                    throw new IllegalStateException("Not allowed to react (friends only)");
                }
            }
            case BEST_FRIEND -> {
                Long ownerId = post.getAuthor().getId();
                // If you intend BEST_FRIEND to be a stricter “friends” set:
                if (!followers.isBestFriend(viewerId, ownerId)) {
                    throw new IllegalStateException("Not allowed to react (best friends only)");
                }
            }
            case PRIVATE -> throw new IllegalStateException("Not allowed to react (private)");
        }
    }
}
