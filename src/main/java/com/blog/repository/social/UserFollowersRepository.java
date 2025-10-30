package com.blog.repository.social;

import com.blog.entity.social.UserFollower;
import com.blog.entity.social.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public interface UserFollowersRepository extends JpaRepository<UserFollower, Long> {

    // Used by feed/visibility checks
    boolean existsByFollower_IdAndFollowing_IdAndRelationTypeIn(
            Long followerId,
            Long followingId,
            Collection<RelationType> relationTypes
    );

    // Useful helper for “who follows this user?”
    @Query("select uf.follower.id from UserFollower uf where uf.following.id = :userId")
    List<Long> followerIdsOf(@Param("userId") Long userId);


    @Query("""
    select count(uf) > 0 from UserFollower uf
    where uf.follower.id = :viewerId
      and uf.following.id = :authorId
      and uf.relationType in (com.blog.entity.social.RelationType.FRIEND,
                              com.blog.entity.social.RelationType.BEST_FRIEND)
  """)
    boolean existsFriendOrBestFriend(@Param("viewerId") Long viewerId,
                                     @Param("authorId") Long authorId);

    @Query("""
    select (count(uf) > 0) from UserFollower uf
    where uf.follower.id = :viewerId
    and uf.following.id = :authorId
    and uf.relationType in (com.blog.entity.social.RelationType.FRIEND,
                            com.blog.entity.social.RelationType.BEST_FRIEND)
    """)
    boolean isFriend(@Param("viewerId") Long viewerId,
                     @Param("authorId") Long authorId);


    @Modifying
    @Query("""
      update UserFollower uf
         set uf.relationType = :relation,
             uf.createdAt = :now
       where uf.follower.id = :followerId and uf.following.id = :followingId
    """)
    int updateRelation(Long followerId, Long followingId, String relation, Instant now);

    @Modifying
    @Query("""
      delete from UserFollower uf
       where uf.follower.id = :followerId and uf.following.id = :followingId
    """)
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Modifying
    @Query(nativeQuery = true, value = """
      insert into user_followers (follower_id, following_id, relation_type, created_at)
      values (:followerId, :followingId, :relation, :epochNow)
      on duplicate key update relation_type = values(relation_type), created_at = values(created_at)
    """)
    void upsertRelation(Long followerId, Long followingId, String relation, Instant epochNow);

    @Query("select uf.following.id from UserFollower uf where uf.follower.id=:userId")
    List<Long> followingIds(Long userId);
}
