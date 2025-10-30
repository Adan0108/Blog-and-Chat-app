package com.blog.service.post;

import com.blog.dto.request.post.CreatePostRequest;
import com.blog.dto.request.post.UpdatePostRequest;
import com.blog.dto.response.comment.CommentDto;
import com.blog.dto.response.comment.CommentPage;
import com.blog.dto.response.post.FeedPage;
import com.blog.dto.response.post.PostDto;
import com.blog.dto.response.post.ReactionDto;

import java.time.Instant;
import java.util.List;

/**
 * Post read/write use-cases.
 * Visibility rules are enforced in the implementation (FRIENDS/PRIVATE/Public).
 */
public interface PostService {
    PostDto create(Long authorId, CreatePostRequest req);
    FeedPage getFeed(Long viewerId, Instant cursor, int limit, int perAuthorCap);
    List<PostDto> getUserPosts(Long requesterId, Long profileUserId, Instant cursor, int limit);
    PostDto getById(Long requesterId, Long postId);

    // NEW
    ReactionDto react(Long userId, Long postId, short reactionTypeId);
    void unreact(Long userId, Long postId);

    CommentDto addComment(Long userId, Long postId, String text);
    CommentPage listComments(Long userId, Long postId, Instant cursor, int limit);

    PostDto update(Long userId, Long postId, UpdatePostRequest req);
    void delete(Long userId, Long postId);
}
