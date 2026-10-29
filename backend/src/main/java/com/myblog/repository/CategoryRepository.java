package com.myblog.repository;

import com.myblog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c.id AS id, c.name AS name, COUNT(a) AS articleCount FROM Category c "
            + "LEFT JOIN Article a ON a.category = c GROUP BY c.id, c.name")
    List<com.myblog.dto.admin.DashboardReadRows.CategoryCount> countDashboardArticles();

    Optional<Category> findByName(String name);
    boolean existsByName(String name);
}
