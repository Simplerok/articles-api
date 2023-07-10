package com.mfi.articlesapi.service;

import com.mfi.articlesapi.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArticleService {

    void addArticle();
    Page<Article> getAllArticles(Pageable pageable);
}
