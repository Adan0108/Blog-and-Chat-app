package com.blog.service.impl.post;

import com.blog.dto.request.post.CreatePostRequest;
import com.blog.dto.request.post.UpdatePostRequest;
import com.blog.dto.response.comment.CommentDto;
import com.blog.dto.response.comment.CommentPage;
import com.blog.dto.response.post.FeedPage;
import com.blog.dto.response.post.PostDto;
import com.blog.dto.response.post.ReactionDto;
import com.blog.entity.blog.Post;
import com.blog.entity.blog.PostImage;
import com.blog.entity.blog.Visibility;
import com.blog.entity.comment.Comment;
import com.blog.entity.reaction.PostReaction;
import com.blog.entity.reaction.ReactionType;
import com.blog.entity.user.User;
import com.blog.entity.user.UserProfile;
import com.blog.repository.blog.PostImageRepository;
import com.blog.repository.blog.PostRepository;
import com.blog.repository.comment.CommentRepository;
import com.blog.repository.reaction.PostReactionRepository;
import com.blog.repository.reaction.ReactionTypeRepository;
import com.blog.repository.user.UserProfileRepository;
import com.blog.repository.user.UserRepository;
import com.blog.repository.social.UserFollowersRepository;
import com.blog.service.post.PostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository posts;
    private final PostImageRepository images;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PostReactionRepository reactions;
    private final CommentRepository comments;
    private final StringRedisTemplate redis;
    private final UserFollowersRepository followers;
    private final ReactionTypeRepository reactionTypes;

    public PostServiceImpl(PostRepository posts,
                           PostImageRepository images,
                           UserRepository users,
                           UserProfileRepository profiles,
                           PostReactionRepository reactions,
                           CommentRepository comments,
                           StringRedisTemplate redis,
                           UserFollowersRepository followers,
                           ReactionTypeRepository reactionTypes) {
        this.posts = posts;
        this.images = images;
        this.users = users;
        this.profiles = profiles;
        this.reactions = reactions;
        this.comments = comments;
        this.redis = redis;
        this.followers = followers;
        this.reactionTypes = reactionTypes;
    }

    @Override
    public PostDto create(Long authorId, CreatePostRequest req) {
        User author = users.findById(authorId).orElseThrow();
        Post p = new Post();
        p.setAuthor(author);
        p.setContent(req.content);
        p.setVisibility(req.visibility == null ? Visibility.PUBLIC : req.visibility);
        p.setMaximumImages(15);

        Post saved = posts.save(p);

        if (req.images != null && !req.images.isEmpty()) {
            if (req.images.size() > 15) throw new IllegalArgumentException("max 15 images");
            int order = 0;
            for (String url : req.images) {
                PostImage pi = new PostImage();
                pi.setPost(saved);
                pi.setImageUrl(url);
                pi.setDisplayOrder(order++);
                images.save(pi);
            }
        }
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedPage getFeed(Long viewerId, Instant cursor, int limit, int perAuthorCap) {
        if (cursor == null) cursor = Instant.now();

        // clamp
        limit = Math.max(1, Math.min(limit, 50));
        perAuthorCap = Math.max(1, Math.min(perAuthorCap, 5));

        long bucket = (cursor.toEpochMilli() / (5 * 60_000));
        String redisKey = "FEED:" + viewerId + ":" + bucket + ":" + limit + ":" + perAuthorCap;

        List<String> cachedIds = redis.opsForList().range(redisKey, 0, -1);
        List<Long> ids;
        if (cachedIds != null && !cachedIds.isEmpty()) {
            ids = cachedIds.stream().map(Long::valueOf).toList();
        } else {
            ids = posts.getFeedIds(viewerId, cursor, perAuthorCap, limit);
            if (!ids.isEmpty()) {
                redis.opsForList().rightPushAll(redisKey, ids.stream().map(String::valueOf).toList());
                redis.expire(redisKey, 2, TimeUnit.MINUTES);
            }
        }

        List<PostDto> dtos = ids.stream()
                .map(id -> posts.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();

        Instant nextCursor = dtos.isEmpty() ? null : dtos.get(dtos.size() - 1).createdAt;
        FeedPage page = new FeedPage();
        page.items = dtos;
        page.nextCursor = nextCursor;
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> getUserPosts(Long requesterId, Long profileUserId, Instant cursor, int limit) {
        if (cursor == null) cursor = Instant.now();
        limit = Math.max(1, Math.min(limit, 50));

        boolean isOwner = Objects.equals(requesterId, profileUserId);

        List<Post> result;
        if (isOwner) {
            result = posts.findByAuthorIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    profileUserId,
                    cursor,
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        } else {
            boolean friend = followers.existsFriendOrBestFriend(requesterId, profileUserId);
            Collection<Visibility> allowed = friend
                    ? List.of(Visibility.PUBLIC, Visibility.FRIENDS)
                    : List.of(Visibility.PUBLIC);

            result = posts.findByAuthorIdAndVisibilityInAndCreatedAtBeforeOrderByCreatedAtDesc(
                    profileUserId,
                    allowed,
                    cursor,
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        }

        return result.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getById(Long requesterId, Long postId) {
        Post p = posts.findById(postId).orElseThrow();
        if (!canSee(requesterId, p)) throw new IllegalStateException("Not allowed");
        return toDto(p);
    }

    // ---------- NEW: reactions ----------
    @Override
    @Transactional
    public ReactionDto react(Long userId, Long postId, short reactionTypeId) {
        if (userId == null) throw new IllegalStateException("Unauthenticated");

        var post = posts.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        var type = reactionTypes.findById(reactionTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Reaction type not found"));

        var existing = reactions.findByPost_IdAndUser_Id(postId, userId);

        if (existing.isPresent()) {
            var pr = existing.get();
            if (!pr.getType().getId().equals(type.getId())) {
                pr.setType(type);
            }
        } else {
            var pr = new PostReaction();
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
    @Transactional
    public void unreact(Long userId, Long postId) {
        if (userId == null) throw new IllegalStateException("Unauthenticated");
        reactions.deleteByPost_IdAndUser_Id(postId, userId);
    }


    // ---------- comments (insert + read-back) ----------
    @Override
    public CommentDto addComment(Long userId, Long postId, String text) {
        Post post = posts.findById(postId).orElseThrow();
        if (!canSee(userId, post)) throw new IllegalStateException("Not allowed");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text required");

        comments.insertRaw(postId, userId, text.trim());
        Long id = comments.lastInsertId();
        var saved = comments.findById(id).orElseThrow();

        CommentDto dto = new CommentDto();
        dto.id = saved.getId();
        dto.postId = postId;
        dto.authorId = saved.getAuthor() != null ? saved.getAuthor().getId() : userId;
        dto.authorUsername = profiles.findByUserId(dto.authorId).map(UserProfile::getUsername).orElse(null);
        dto.text = saved.getContent();
        dto.createdAt = saved.getCreatedAt();
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CommentPage listComments(Long userId, Long postId, Instant cursor, int limit) {
        Post post = posts.findById(postId).orElseThrow();
        if (!canSee(userId, post)) throw new IllegalStateException("Not allowed");
        if (cursor == null) cursor = Instant.now();
        limit = Math.max(1, Math.min(limit, 50));

        var list = comments.pageByPost(postId, cursor, PageRequest.of(0, limit));
        var dtos = list.stream().map(c -> {
            CommentDto d = new CommentDto();
            d.id = c.getId();
            d.postId = postId;
            var au = c.getAuthor();
            d.authorId = (au != null) ? au.getId() : null;
            d.authorUsername = (d.authorId == null) ? null
                    : profiles.findByUserId(d.authorId).map(UserProfile::getUsername).orElse(null);
            d.text = c.getContent();
            d.createdAt = c.getCreatedAt();
            return d;
        }).toList();

        Instant next = dtos.isEmpty() ? null : dtos.get(dtos.size() - 1).createdAt;
        CommentPage page = new CommentPage();
        page.items = dtos;
        page.nextCursor = next;
        return page;
    }


    // ---------- NEW: update / delete ----------
    @Override
    public PostDto update(Long userId, Long postId, UpdatePostRequest req) {
        Post p = posts.findById(postId).orElseThrow();
        if (!Objects.equals(p.getAuthor().getId(), userId)) throw new IllegalStateException("Not allowed");

        if (req.content != null) p.setContent(req.content);
        if (req.visibility != null) p.setVisibility(req.visibility);

        if (req.images != null) {
            if (req.images.size() > 15) throw new IllegalArgumentException("max 15 images");
            // remove old
            if (p.getImages() != null) {
                p.getImages().forEach(images::delete);
                p.getImages().clear();
            }
            // add new
            int order = 0;
            for (String url : req.images) {
                PostImage pi = new PostImage();
                pi.setPost(p);
                pi.setImageUrl(url);
                pi.setDisplayOrder(order++);
                images.save(pi);
            }
        }

        return toDto(posts.save(p));
    }

    @Override
    public void delete(Long userId, Long postId) {
        Post p = posts.findById(postId).orElseThrow();
        if (!Objects.equals(p.getAuthor().getId(), userId)) throw new IllegalStateException("Not allowed");

        // if you don't have JPA cascade delete configured for children, do manual cleanup:
        if (p.getImages() != null) p.getImages().forEach(images::delete);
        // (optional) add repository delete-by-post for comments/reactions if needed

        posts.delete(p);
    }

    // ---------- helpers ----------
    private boolean canSee(Long requesterId, Post p) {
        if (p.getVisibility() == Visibility.PUBLIC) return true;
        Long authorId = p.getAuthor().getId();
        if (Objects.equals(authorId, requesterId)) return true;
        if (p.getVisibility() == Visibility.FRIENDS) {
            return followers.isFriend(requesterId, authorId);
        }
        return false; // PRIVATE and not owner
    }

    private PostDto toDto(Post p) {
        PostDto dto = new PostDto();
        dto.id = p.getId();
        dto.authorId = p.getAuthor().getId();
        dto.authorUsername = profiles.findByUserId(p.getAuthor().getId())
                .map(UserProfile::getUsername)
                .orElse(null);
        dto.content = p.getContent();
        dto.visibility = p.getVisibility();
        dto.createdAt = p.getCreatedAt();
        dto.images = (p.getImages() == null)
                ? List.of()
                : p.getImages().stream()
                .sorted(Comparator.comparing(PostImage::getDisplayOrder))
                .map(PostImage::getImageUrl)
                .toList();
        dto.reactions = reactions.countByPost_Id(p.getId());
        dto.comments = comments.countByPost_Id(p.getId());
        return dto;
    }
}
