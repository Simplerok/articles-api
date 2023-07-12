package com.mfi.articlesapi.repository;

import com.mfi.articlesapi.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticlesRepository extends JpaRepository<Article, Integer> {

    boolean existsByTitleAndNewsSite(String title, String newsSite);

    Optional<Article> findById(Long integer);
    Page<Article> getAllByNewsSite(String newsSite, Pageable pageable);
}
