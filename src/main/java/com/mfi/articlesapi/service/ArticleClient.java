package com.mfi.articlesapi.service;

import com.mfi.articlesapi.dto.DownloadArticle;
import feign.Param;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.List;

@FeignClient(value = "articles", url = "${articles-api.external-url}")
public interface ArticleClient {

    @RequestLine("GET /v3/articles?_limit={limit}&_start={start}")
    List<DownloadArticle> getArticles(@Param int limit, @Param int start);

    @RequestLine("GET {articleUrl}")
    String downloadArticle(@Param int limit, @Param int start);


}
