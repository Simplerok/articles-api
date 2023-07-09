package com.mfi.articlesapi.service.impl;

import com.mfi.articlesapi.dto.DownloadArticle;
import com.mfi.articlesapi.property.ArticleProperty;
import com.mfi.articlesapi.service.ArticleClient;
import com.mfi.articlesapi.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final ArticleClient articleClient;
    private final ArticleProperty articleProperty;
    private ExecutorService executor;
    private AtomicInteger offsetCounter;
    private ConcurrentHashMap<String, List<DownloadArticle>> articlesMap;

    public ArticleServiceImpl(ArticleClient articleClient, ArticleProperty articleProperty){

        this.articleClient = articleClient;
        this.articleProperty = articleProperty;
        executor = Executors.newFixedThreadPool(articleProperty.getNumOfThreads());
        offsetCounter = new AtomicInteger(0);
        articlesMap = new ConcurrentHashMap<>();

    }

    @Scheduled(cron = "0 0 * * * *")
    @Override
    public void addArticle() {

        int numOfTasks = articleProperty.getArticlesTotalLimit()/articleProperty.getArticlesThreadLimit();
        int lastOfArticles = articleProperty.getArticlesTotalLimit()%articleProperty.getArticlesThreadLimit();

        if (numOfTasks < 0) {
            log.warn("The number of articles can't be zero");
            return;
        }

        for (int i = 0; i < numOfTasks; i++) {
            executor.submit(() -> downloadArticles(articleProperty.getArticlesThreadLimit(),
                    offsetCounter.getAndAdd(articleProperty.getArticlesThreadLimit())));
        }
        if(lastOfArticles > 0) {
            executor.submit(() -> downloadArticles(lastOfArticles, offsetCounter.getAndAdd(lastOfArticles)));
        }
    }

    private void downloadArticles(int limit, int offset){

        List<DownloadArticle> articles = articleClient.getArticles(limit, offset);

        articles.stream()
                .filter(article ->
                        articleProperty.getBlackList().stream()
                                .noneMatch(str -> article.getTitle().contains(str)))
                .sorted(Comparator.comparing(DownloadArticle::getPublishedAt))
                .collect(Collectors.groupingBy(DownloadArticle::getNewsSite))
                .forEach((key, value) -> articlesMap.merge(key, value, (oldValue, newValue) ->
                {
                    oldValue.addAll(newValue);
                    return oldValue;
                }));


    }
}
