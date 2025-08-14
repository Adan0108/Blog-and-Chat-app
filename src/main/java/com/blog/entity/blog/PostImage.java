package com.blog.entity.blog;

import jakarta.persistence.*;

@Entity @Table(name = "post_images", uniqueConstraints = {
        @UniqueConstraint(name = "uq_post_display_order", columnNames = {"post_id","display_order"})
}, indexes = { @Index(name = "ix_post_images_post", columnList = "post_id") })
public class PostImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0; // enforce 0..14 at service layer or DB CHECK

    // getters/setters
}

