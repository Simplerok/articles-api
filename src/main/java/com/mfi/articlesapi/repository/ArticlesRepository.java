package com.mfi.articlesapi.repository;

import com.mfi.articlesapi.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticlesRepository extends JpaRepository<Article, Integer> {

    boolean existsByTitleAndNewsSite(String title, String newsSite);
}
