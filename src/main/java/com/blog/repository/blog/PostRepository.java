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
            /* friend weight: 1 if FRIEND/BEST_FRIEND between viewer and author */
            (
              SELECT 1
              FROM user_followers uf
              WHERE uf.follower_id = :viewerId 
                AND uf.following_id = p.user_id
                AND uf.relation_type IN ('FRIEND','BEST_FRIEND')
              LIMIT 1
            ) AS rel_weight,
            /* freshness + engagement */
            TIMESTAMPDIFF(SECOND, p.created_at, NOW()) AS age_sec,
            (SELECT COUNT(*) FROM post_reactions r WHERE r.post_id = p.id) AS react_cnt,
            (SELECT COUNT(*) FROM user_comments c WHERE c.post_id = p.id) AS cmt_cnt
          FROM posts p
          WHERE 
            p.created_at < :cursor
            AND (
              p.user_id = :viewerId
              OR p.visibility = 'PUBLIC'
              OR (
                   p.visibility = 'FRIENDS'
                   AND EXISTS (
                        SELECT 1 FROM user_followers uf2
                        WHERE uf2.follower_id = :viewerId 
                          AND uf2.following_id = p.user_id
                          AND uf2.relation_type IN ('FRIEND','BEST_FRIEND')
                   )
              )
            )
        ) x
        WHERE x.rn <= :perAuthorCap
        ORDER BY 
          (CASE WHEN x.rel_weight = 1 THEN 1000 ELSE 0 END)
          + (x.react_cnt * 3 + x.cmt_cnt * 5)
          - (x.age_sec / 300) DESC,
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
