package com.blog.controller.post;

import com.blog.dto.request.comment.CreateCommentRequest;
import com.blog.dto.request.comment.UpdateCommentRequest;
import com.blog.dto.response.comment.CommentDto;
import com.blog.dto.response.comment.CommentPage;
import com.blog.service.post.PostCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/posts")
public class PostCommentController {

    private final PostCommentService comments;

    public PostCommentController(PostCommentService comments) {
        this.comments = comments;
    }

    private Long currentUserId() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? null : Long.valueOf(String.valueOf(a.getPrincipal()));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDto> add(@PathVariable Long postId,
                                          @RequestBody CreateCommentRequest req) {
        var dto = comments.addComment(currentUserId(), postId, req.text(), req.parentId());
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<CommentPage> list(@PathVariable Long postId,
                                            @RequestParam(required = false) Instant cursor,
                                            @RequestParam(defaultValue = "20") int limit) {
        int safe = Math.max(1, Math.min(limit, 50));
        return ResponseEntity.ok(comments.listComments(currentUserId(), postId, cursor, safe));
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentDto> edit(@PathVariable Long commentId,
                                           @RequestBody UpdateCommentRequest req) {
        var dto = comments.editComment(currentUserId(), commentId, req.text());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        comments.deleteComment(currentUserId(), commentId);
        return ResponseEntity.noContent().build();
    }
}
