package com.blog.entity.reaction;

import jakarta.persistence.*;

@Entity @Table(name = "reaction_types", indexes = {
        @Index(name = "ix_reaction_types_name", columnList = "name", unique = true)
})
public class ReactionType {
    @Id
    private Short id; // TINYINT in MySQL

    @Column(nullable = false, length = 20, unique = true)
    private String name;

    @Column(length = 255)
    private String iconUrl;

    // getters/setters
}
