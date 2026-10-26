package com.myblog.repository;

import com.myblog.entity.ArticleCreationRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;

public interface ArticleCreationRequestRepository extends JpaRepository<ArticleCreationRequest, Long> {
    // A locking read also observes the latest committed receipt under MySQL REPEATABLE READ.
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<ArticleCreationRequest> findByAuthorIdAndRequestKey(Long authorId, String requestKey);
}
