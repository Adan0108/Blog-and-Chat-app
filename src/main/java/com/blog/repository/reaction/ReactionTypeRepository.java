package com.blog.repository.reaction;

import com.blog.entity.reaction.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionTypeRepository extends JpaRepository<ReactionType, Short> { }