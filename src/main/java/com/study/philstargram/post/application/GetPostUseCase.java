package com.study.philstargram.post.application;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.post.domain.PostId;
import com.study.philstargram.post.domain.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPostUseCase {

    private final PostRepository postRepository;

    public GetPostUseCase(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public PostResult execute(Long postId) {
        return postRepository.findById(PostId.of(postId))
                .map(PostResult::from)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글입니다: " + postId));
    }
}
