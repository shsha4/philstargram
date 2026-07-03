package com.study.philstargram.post.application;

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
        // 작성자 존재 검증과 닉네임 조회를 한 번에 한다(없으면 NotFoundException). event-carried
        // state: 여기서 얻은 닉네임을 이벤트에 실어, feed/notification 이 member 를 재조회하지 않게 한다.
        String authorNickname = memberQueryService.getSummary(command.authorId()).nickname();
        Post post = postRepository.save(Post.write(command.authorId(), command.content()));
        // 애그리거트가 발생시킨 도메인 이벤트를 드레인해 모듈 간 계약으로 번역·발행한다.
        post.pullDomainEvents().forEach(event -> eventPublisher.publishEvent(PostCreatedEvent.from(event, authorNickname)));
        return PostResult.from(post);
    }
}
