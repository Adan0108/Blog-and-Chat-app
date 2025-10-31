package com.blog.service.impl.post;

import com.blog.dto.response.comment.CommentDto;
import com.blog.dto.response.comment.CommentPage;
import com.blog.entity.blog.Post;
import com.blog.entity.blog.Visibility;
import com.blog.entity.comment.Comment;
import com.blog.repository.blog.PostRepository;
import com.blog.repository.comment.CommentRepository;
import com.blog.repository.user.UserProfileRepository;
import com.blog.repository.social.UserFollowersRepository;
import com.blog.service.post.PostCommentService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class PostCommentServiceImpl implements PostCommentService {

    private final PostRepository posts;
    private final CommentRepository comments;
    private final UserProfileRepository profiles;
    private final UserFollowersRepository followers;

    public PostCommentServiceImpl(PostRepository posts,
                                  CommentRepository comments,
                                  UserProfileRepository profiles,
                                  UserFollowersRepository followers) {
        this.posts = posts;
        this.comments = comments;
        this.profiles = profiles;
        this.followers = followers;
    }

    private boolean canSee(Long requesterId, Post p) {
        if (p.getVisibility() == Visibility.PUBLIC) return true;

        Long authorId = p.getAuthor().getId();
        if (Objects.equals(authorId, requesterId)) return true;

        return switch (p.getVisibility()) {
            case FRIENDS -> followers.isFriend(requesterId, authorId) || followers.isBestFriend(requesterId, authorId);
            case BEST_FRIEND -> followers.isBestFriend(requesterId, authorId);
            default -> false; // PRIVATE, or anything else
        };
    }



    private boolean canModify(Long userId, Comment c) {
        if (userId == null) return false;
        Long authorId = c.getAuthor() == null ? null : c.getAuthor().getId();
        Long postOwnerId = c.getPost().getAuthor() == null ? null : c.getPost().getAuthor().getId();
        return userId.equals(authorId) || userId.equals(postOwnerId);
    }

    private CommentDto toDto(Comment c) {
        CommentDto d = new CommentDto();
        d.id = c.getId();
        d.postId = c.getPost().getId();
        var au = c.getAuthor();
        d.authorId = (au != null) ? au.getId() : null;
        d.authorUsername = (d.authorId == null) ? null
                : profiles.findByUserId(d.authorId).map(p -> p.getUsername()).orElse(null);
        d.text = c.getContent();
        d.createdAt = c.getCreatedAt();
        d.parentId = (c.getParent() != null) ? c.getParent().getId() : null;
        return d;
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long postId, String text, Long parentId) {
        if (userId == null) throw new IllegalStateException("Unauthenticated");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text required");

        Post post = posts.findById(postId).orElseThrow();
        if (!canSee(userId, post)) throw new IllegalStateException("Not allowed");

        if (parentId != null) {
            Comment parent = comments.findById(parentId).orElseThrow();
            if (!parent.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("Parent comment not in this post");
            }
        }

        comments.insertRaw(postId, userId, parentId, text.trim());
        Long id = comments.lastInsertId();
        Comment saved = comments.findById(id).orElseThrow();
        return toDto(saved);
    }

    @Override
    @Transactional
    public CommentPage listComments(Long userId, Long postId, Instant cursor, int limit) {
        Post post = posts.findById(postId).orElseThrow();
        if (!canSee(userId, post)) throw new IllegalStateException("Not allowed");

        if (cursor == null) cursor = Instant.now();
        limit = Math.max(1, Math.min(limit, 50));

        var list = comments.pageByPost(postId, cursor, PageRequest.of(0, limit));
        var dtos = list.stream().map(this::toDto).toList();

        var page = new CommentPage();
        page.items = dtos;
        page.nextCursor = dtos.isEmpty() ? null : dtos.get(dtos.size() - 1).createdAt;
        return page;
    }

    @Override
    @Transactional
    public CommentDto editComment(Long userId, Long commentId, String newText) {
        if (userId == null) throw new IllegalStateException("Unauthenticated");
        if (newText == null || newText.isBlank()) throw new IllegalArgumentException("text required");

        Comment c = comments.findById(commentId).orElseThrow();
        if (!canSee(userId, c.getPost())) throw new IllegalStateException("Not allowed");
        if (!canModify(userId, c)) throw new IllegalStateException("Forbidden");

        c.setContent(newText.trim());
        c.setUpdatedAt(Instant.now());
        Comment saved = comments.save(c);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        if (userId == null) throw new IllegalStateException("Unauthenticated");

        Comment c = comments.findById(commentId).orElseThrow();
        if (!canSee(userId, c.getPost())) throw new IllegalStateException("Not allowed");
        if (!canModify(userId, c)) throw new IllegalStateException("Forbidden");

        comments.delete(c);
    }
}
