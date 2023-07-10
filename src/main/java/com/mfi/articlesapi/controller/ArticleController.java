package com.mfi.articlesapi.controller;

import com.mfi.articlesapi.entity.Article;
import com.mfi.articlesapi.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/download-articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;

    @GetMapping
    public Page<Article> getAllArticles(@PageableDefault(size = 5, sort = {"id"})Pageable pageable){
        return articleService.getAllArticles(pageable);
    }
}
