package com.blog.dto.response.post;

import java.time.Instant;
import java.util.List;

public class FeedPage {
    public List<PostDto> items;
    public Instant nextCursor; // null if end
}
