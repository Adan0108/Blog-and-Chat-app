package com.blog.repository.comment;

import com.blog.entity.comment.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByPost_Id(Long postId);

    // your existing cursor-paging helper
    @Query("""
        select c from Comment c
         where c.post.id = :postId and c.createdAt < :cursor
         order by c.createdAt desc
        """)
    List<Comment> pageByPost(Long postId, Instant cursor, Pageable pageable);

    // Minimal INSERT that returns the generated id (MySQL)
    // Adjust column names if your table differs: user_id vs author_id, text/content, etc.
    @Modifying
    @Query(value = """
    INSERT INTO user_comments (post_id, user_id, content, created_at)
    VALUES (?1, ?2, ?3, NOW())
    """, nativeQuery = true)
    void insertRaw(Long postId, Long userId, String text);


    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long lastInsertId();
}
