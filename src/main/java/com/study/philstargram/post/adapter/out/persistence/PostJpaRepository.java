package com.study.philstargram.post.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {

    List<PostJpaEntity> findByAuthorIdInOrderByCreatedAtDesc(List<Long> authorIds, Pageable pageable);
}
