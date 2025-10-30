package com.blog.dto.request.post;

import com.blog.entity.blog.Visibility;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public class CreatePostRequest {
    @NotBlank @Size(max = 3000)
    public String content;
    public Visibility visibility;
    @Size(max = 15)
    public List<@NotBlank String> images;
}

