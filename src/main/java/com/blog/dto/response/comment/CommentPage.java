package com.blog.dto.response.comment;

import java.time.Instant;
import java.util.List;

public class CommentPage {
    public List<CommentDto> items;
    public Instant nextCursor;
}
