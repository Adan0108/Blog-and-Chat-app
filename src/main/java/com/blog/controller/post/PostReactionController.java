package com.blog.controller.post;

import com.blog.dto.request.post.ReactionRequest;
import com.blog.dto.response.post.ReactionDto;
import com.blog.service.post.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostReactionController {

    private final PostService posts;
    public PostReactionController(PostService posts) { this.posts = posts; }

    private Long currentUserId() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? null : Long.valueOf(String.valueOf(a.getPrincipal()));
    }

    @PostMapping("/{id}/reactions")
    public ResponseEntity<ReactionDto> react(@PathVariable Long id,
                                             @RequestBody(required = false) ReactionRequest req) {
        short typeId = (req == null || req.type() == null) ? 1 : req.type(); // default LIKE=1
        var dto = posts.react(currentUserId(), id, typeId);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}/reactions")
    public ResponseEntity<Void> unreact(@PathVariable Long id) {
        posts.unreact(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
