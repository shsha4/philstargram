package com.study.philstargram.post.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {
}
