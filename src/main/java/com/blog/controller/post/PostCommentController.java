package com.blog.controller.post;

import com.blog.dto.request.comment.CreateCommentRequest;
import com.blog.dto.response.comment.CommentDto;
import com.blog.dto.response.comment.CommentPage;
import com.blog.service.post.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/posts")
public class PostCommentController {

    private final PostService posts;
    public PostCommentController(PostService posts) { this.posts = posts; }

    private Long currentUserId() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? null : Long.valueOf(String.valueOf(a.getPrincipal()));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDto> add(@PathVariable Long id, @RequestBody CreateCommentRequest req) {
        var dto = posts.addComment(currentUserId(), id, req.text());
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<CommentPage> list(@PathVariable Long id,
                                            @RequestParam(required = false) Instant cursor,
                                            @RequestParam(defaultValue = "20") int limit) {
        int safe = Math.max(1, Math.min(limit, 50));
        return ResponseEntity.ok(posts.listComments(currentUserId(), id, cursor, safe));
    }
}
