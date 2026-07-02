package com.study.philstargram.post.adapter.in.web;

import com.study.philstargram.common.response.ApiResponse;
import com.study.philstargram.post.application.CreatePostCommand;
import com.study.philstargram.post.application.CreatePostUseCase;
import com.study.philstargram.post.application.GetPostUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final CreatePostUseCase createPostUseCase;
    private final GetPostUseCase getPostUseCase;

    public PostController(CreatePostUseCase createPostUseCase, GetPostUseCase getPostUseCase) {
        this.createPostUseCase = createPostUseCase;
        this.getPostUseCase = getPostUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody CreatePostRequest request) {
        var result = createPostUseCase.execute(new CreatePostCommand(request.authorId(), request.content()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(PostResponse.from(result)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long id) {
        var result = getPostUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(PostResponse.from(result)));
    }
}
