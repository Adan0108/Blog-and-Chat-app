// src/main/java/com/blog/repository/blog/PostRepository.java
package com.blog.repository.blog;

import com.blog.entity.blog.Post;
import com.blog.entity.blog.Visibility;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(value = """
        SELECT id FROM (
          SELECT
            p.id,
            p.user_id,
            p.created_at,
            ROW_NUMBER() OVER (PARTITION BY p.user_id ORDER BY p.created_at DESC) AS rn,
        
            /* viewer is author */
            (p.user_id = :viewerId) AS self_weight,
        
            /* single join to inspect relationship from viewer -> author */
            COALESCE(
              CASE uf.relation_type
                WHEN 'BEST_FRIEND' THEN 1000   /* biggest boost */
                WHEN 'FRIEND'      THEN  400   /* mutual */
                WHEN 'FOLLOWER'    THEN  120   /* one-way follow */
                ELSE 0
              END,
            0) AS rel_score,
        
            /* freshness + engagement */
            TIMESTAMPDIFF(SECOND, p.created_at, NOW()) AS age_sec,
            (SELECT COUNT(*) FROM post_reactions r WHERE r.post_id = p.id) AS react_cnt,
            (SELECT COUNT(*) FROM user_comments  c WHERE c.post_id = p.id) AS cmt_cnt
        
          FROM posts p
          LEFT JOIN user_followers uf
            ON uf.follower_id  = :viewerId
           AND uf.following_id = p.user_id
        
          WHERE
            p.created_at < :cursor
            AND (
              /* own posts always visible */
              p.user_id = :viewerId
        
              /* public visible to all */
              OR p.visibility = 'PUBLIC'
        
              /* FRIENDS/BEST_FRIEND: single check since your DB promotes to FRIEND on mutual */
              OR (
                p.visibility = 'FRIENDS'
                AND uf.relation_type IN ('FRIEND','BEST_FRIEND')
              )
        
              /* if you also have BEST_FRIEND-only visibility, gate it here similarly:
                 OR (p.visibility = 'BEST_FRIEND' AND uf.relation_type = 'BEST_FRIEND')
              */
            )
        ) x
        WHERE x.rn <= :perAuthorCap
        ORDER BY
            (CASE WHEN x.self_weight THEN 1200 ELSE 0 END)
          + x.rel_score
          + (x.react_cnt * 3 + x.cmt_cnt * 5)
          - (x.age_sec / 300)
          DESC,
          x.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> getFeedIds(@Param("viewerId") Long viewerId,
                          @Param("cursor") Instant cursor,
                          @Param("perAuthorCap") int perAuthorCap,
                          @Param("limit") int limit);


    @Query("""
      select p from Post p
      where p.author.id = :authorId and p.createdAt < :cursor
      order by p.createdAt desc
    """)
    List<Post> findByAuthorIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("authorId") Long authorId,
            @Param("cursor") Instant cursor,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
      select p from Post p
      where p.author.id = :authorId
        and p.visibility in :vis
        and p.createdAt < :cursor
      order by p.createdAt desc
    """)
    List<Post> findByAuthorIdAndVisibilityInAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("authorId") Long authorId,
            @Param("vis") Collection<com.blog.entity.blog.Visibility> vis,
            @Param("cursor") Instant cursor,
            org.springframework.data.domain.Pageable pageable
    );
}
