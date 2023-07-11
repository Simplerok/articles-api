package com.mfi.articlesapi.service;

import com.mfi.articlesapi.dto.DownloadArticle;
import feign.Param;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "articles", url = "${articles-api.external-url}")
public interface ArticleClient {

    @GetMapping("/v3/articles")
    List<DownloadArticle> getArticles(@RequestParam("_limit") int limit, @RequestParam("_start") int start);

}
