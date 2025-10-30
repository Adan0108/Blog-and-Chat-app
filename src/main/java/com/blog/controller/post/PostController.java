package com.blog.controller.post;

import com.blog.dto.request.post.CreatePostRequest;
import com.blog.dto.request.post.UpdatePostRequest;
import com.blog.dto.response.post.FeedPage;
import com.blog.dto.response.post.PostDto;
import com.blog.service.post.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService service;
    public PostController(PostService service) { this.service = service; }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        return Long.valueOf(String.valueOf(auth.getPrincipal()));
    }

    // ---- Create: return 201 + Location header ----
    @PostMapping
    public ResponseEntity<PostDto> create(@RequestBody CreatePostRequest req) {
        Long uid = currentUserId();
        PostDto dto = service.create(uid, req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.id)
                .toUri();

        return ResponseEntity.created(location).body(dto); // 201
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostDto> update(@PathVariable Long id, @RequestBody UpdatePostRequest req) {
        Long uid = currentUserId();
        return ResponseEntity.ok(service.update(uid, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long uid = currentUserId();
        service.delete(uid, id);
        return ResponseEntity.noContent().build();
    }

    // ---- Feed: clamp params to sane bounds ----
    @GetMapping("/feed")
    public ResponseEntity<FeedPage> feed(
            @RequestParam(required = false) Instant cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "2") int perAuthorCap
    ) {
        Long uid = currentUserId();
        int safeLimit = Math.max(1, Math.min(limit, 50));          // 1..50
        int safePerAuthor = Math.max(1, Math.min(perAuthorCap, 5)); // 1..5
        return ResponseEntity.ok(service.getFeed(uid, cursor, safeLimit, safePerAuthor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> get(@PathVariable Long id) {
        Long uid = currentUserId();
        return ResponseEntity.ok(service.getById(uid, id));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<PostDto>> byUser(@PathVariable Long userId,
                                                @RequestParam(required = false) Instant cursor,
                                                @RequestParam(defaultValue = "20") int limit) {
        Long uid = currentUserId();
        int safeLimit = Math.max(1, Math.min(limit, 50)); // 1..50
        return ResponseEntity.ok(service.getUserPosts(uid, userId, cursor, safeLimit));
    }
}
