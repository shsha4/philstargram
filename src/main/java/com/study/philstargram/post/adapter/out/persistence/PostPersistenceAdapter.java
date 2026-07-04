package com.study.philstargram.post.adapter.out.persistence;

import com.study.philstargram.post.domain.Post;
import com.study.philstargram.post.domain.PostId;
import com.study.philstargram.post.domain.PostRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class PostPersistenceAdapter implements PostRepository {

    private final PostJpaRepository postJpaRepository;

    PostPersistenceAdapter(PostJpaRepository postJpaRepository) {
        this.postJpaRepository = postJpaRepository;
    }

    @Override
    public Post save(Post post) {
        PostJpaEntity saved = postJpaRepository.save(toEntity(post));
        // DB 가 생성한 식별자를 애그리거트에 되돌려 부여한다. 그래야 애그리거트가 누적한
        // 도메인 이벤트를 postId 까지 채워 발행할 수 있다(pullDomainEvents 는 UseCase 가 호출).
        if (post.getId() == null) {
            post.assignId(PostId.of(saved.getId()));
        }
        return post;
    }

    @Override
    public Optional<Post> findById(PostId id) {
        return postJpaRepository.findById(id.value()).map(PostPersistenceAdapter::toDomain);
    }

    @Override
    public List<Post> findRecentByAuthorIds(List<Long> authorIds, int limit) {
        if (authorIds.isEmpty()) {
            return List.of();
        }
        return postJpaRepository.findByAuthorIdInOrderByCreatedAtDesc(authorIds, PageRequest.of(0, limit)).stream()
                .map(PostPersistenceAdapter::toDomain)
                .toList();
    }

    private static PostJpaEntity toEntity(Post post) {
        Long id = post.getId() == null ? null : post.getId().value();
        return new PostJpaEntity(id, post.getAuthorId(), post.getAuthorNickname(), post.getContent(), post.getCreatedAt());
    }

    private static Post toDomain(PostJpaEntity entity) {
        return Post.reconstitute(entity.getId(), entity.getAuthorId(), entity.getAuthorNickname(), entity.getContent(), entity.getCreatedAt());
    }
}
