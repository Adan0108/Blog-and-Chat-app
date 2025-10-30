package com.blog.entity.reaction;

import jakarta.persistence.*;

@Entity
@Table(name = "reaction_types", indexes = {
        @Index(name = "ix_reaction_types_name", columnList = "name", unique = true)
})
public class ReactionType {
    @Id
    private Short id; // TINYINT in MySQL (0..255) → fits in short

    @Column(nullable = false, length = 20, unique = true)
    private String name; // e.g., LIKE, LOVE, CLAP

    @Column(length = 255)
    private String iconUrl;

    // getters/setters
    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}
