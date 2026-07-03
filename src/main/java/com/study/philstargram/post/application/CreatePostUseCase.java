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
        Post post = postRepository.save(Post.write(command.authorId(), command.content()));
        // 애그리거트가 발생시킨 도메인 이벤트를 드레인해 모듈 간 계약으로 번역·발행한다.
        post.pullDomainEvents().forEach(event -> eventPublisher.publishEvent(PostCreatedEvent.from(event)));
        return PostResult.from(post);
    }
}
