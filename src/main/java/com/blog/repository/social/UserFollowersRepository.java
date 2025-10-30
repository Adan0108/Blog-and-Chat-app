package com.blog.repository.user;

import com.blog.entity.social.UserFollower;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFollowersRepository extends CrudRepository<UserFollower, Long> {

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM user_followers uf
        WHERE uf.follower_id = :viewerId
          AND uf.following_id = :authorId
          AND uf.relation_type IN ('FRIEND','BEST_FRIEND')
        """, nativeQuery = true)
    boolean isFriend(Long viewerId, Long authorId);

    @Query(value = """
        SELECT uf.follower_id
        FROM user_followers uf
        WHERE uf.following_id = :authorId
        """, nativeQuery = true)
    List<Long> followerIdsOf(Long authorId);


}


