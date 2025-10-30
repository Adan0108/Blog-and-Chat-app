package com.blog.dto.request.post;

import com.blog.entity.blog.Visibility;
import java.util.List;

public class UpdatePostRequest {
    public String content;
    public Visibility visibility;
    public List<String> images; // full replacement if provided
}
