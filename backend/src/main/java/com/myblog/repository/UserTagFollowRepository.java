package com.myblog.repository;

import com.myblog.entity.UserTagFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagFollowRepository extends JpaRepository<UserTagFollow, Long> {

    boolean existsByUserIdAndTagId(Long userId, Long tagId);

    void deleteByUserIdAndTagId(Long userId, Long tagId);

    List<UserTagFollow> findByUserId(Long userId);

    List<UserTagFollow> findByTagId(Long tagId);
}
