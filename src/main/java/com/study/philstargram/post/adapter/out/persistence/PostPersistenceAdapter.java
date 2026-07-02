package com.study.philstargram.post.adapter.out.persistence;

import com.study.philstargram.post.domain.Post;
import com.study.philstargram.post.domain.PostRepository;
import java.util.Optional;
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
        return toDomain(saved);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id).map(PostPersistenceAdapter::toDomain);
    }

    private static PostJpaEntity toEntity(Post post) {
        return new PostJpaEntity(post.getId(), post.getAuthorId(), post.getContent(), post.getCreatedAt());
    }

    private static Post toDomain(PostJpaEntity entity) {
        return Post.reconstitute(entity.getId(), entity.getAuthorId(), entity.getContent(), entity.getCreatedAt());
    }
}
