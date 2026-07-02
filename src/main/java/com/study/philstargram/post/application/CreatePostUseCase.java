package com.study.philstargram.post.application;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.post.domain.Post;
import com.study.philstargram.post.domain.PostRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePostUseCase {

    private final PostRepository postRepository;
    private final MemberQueryService memberQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public CreatePostUseCase(PostRepository postRepository, MemberQueryService memberQueryService, ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.memberQueryService = memberQueryService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PostResult execute(CreatePostCommand command) {
        if (!memberQueryService.existsById(command.authorId())) {
            throw new NotFoundException("존재하지 않는 회원입니다: " + command.authorId());
        }
        Post post = Post.write(command.authorId(), command.content());
        PostResult result = PostResult.from(postRepository.save(post));
        eventPublisher.publishEvent(PostCreatedEvent.from(result));
        return result;
    }
}
