package com.mfi.articlesapi.service.impl;

import com.mfi.articlesapi.dto.DownloadArticle;
import com.mfi.articlesapi.entity.Article;
import com.mfi.articlesapi.exception.NotFoundException;
import com.mfi.articlesapi.property.ArticleProperty;
import com.mfi.articlesapi.repository.ArticlesRepository;
import com.mfi.articlesapi.service.ArticleClient;
import com.mfi.articlesapi.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
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
    private final ExecutorService executor;
    private AtomicInteger offsetCounter;
    private final ConcurrentHashMap<String, List<DownloadArticle>> articlesMap;
    private final ArticlesRepository articlesRepository;
    private final RestTemplate restTemplate;


    public ArticleServiceImpl(ArticleClient articleClient, ArticleProperty articleProperty, ArticlesRepository articlesRepository, RestTemplate restTemplate){

        this.articleClient = articleClient;
        this.articleProperty = articleProperty;
        this.articlesRepository = articlesRepository;
        this.restTemplate = restTemplate;
        executor = Executors.newFixedThreadPool(articleProperty.getNumOfThreads());
        offsetCounter = new AtomicInteger(0);
        articlesMap = new ConcurrentHashMap<>();

    }

    @Override
    public Page<Article> getAllArticles(Pageable pageable) {
        if(articlesRepository.findAll(pageable).isEmpty()){
            throw new NotFoundException("There's not any articles yet");
        }
        return articlesRepository.findAll(pageable);
    }

    @Override
    public Article getById(Long id) {
        return articlesRepository.findById(id).orElseThrow(()->
                new NotFoundException("Article not found with id="+id));
    }

    @Override
    public Page<Article> getAllArticlesFilteredByNewsSite(String newsSite, Pageable pageable) {
        if(articlesRepository.getAllByNewsSite(newsSite, pageable).isEmpty()){
            throw new NotFoundException("There's not any articles yet");
        }
        return articlesRepository.getAllByNewsSite(newsSite, pageable);
    }

    /*
    Данный метод запускает требуемое количество потоков для загрузки информации о новостных статьях
    Потоки запускаются с периодичностью в 1 час
    */
    //    @Scheduled(cron = "0 0 * * * *")
    @Scheduled(fixedRate = 3600000)
    @Override
    public void addArticle() {

        int numOfTasks = articleProperty.getArticlesTotalLimit()/articleProperty.getArticlesThreadLimit();
        int lastOfArticles = articleProperty.getArticlesTotalLimit()%articleProperty.getArticlesThreadLimit();

        if (numOfTasks < 1 && lastOfArticles < 1) {
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

    /*
    Данный метод формирует клиентский запрос для загрузки информации о новостных статьях, фильтрует согласно черного списка
    и группирует полученную информацию, сохраняет информацию в буфер
    */
    private void downloadArticles(int limit, int offset){

        log.info("Asked client : limit={}, offset={}", limit, offset);
        List<DownloadArticle> articles = articleClient.getArticles(limit, offset);
        log.info("Articles client return= {} articles", articles.size());
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

        checkPoolSizeAndAddArtToDataBase();
        log.info("Finish download articles");
    }


    /*
    Данный метод проверяет достижение лимита количества статей, сгруппированных по новостному сайту.
    При достижении лимита формируется клиентский запрос по ссылке статьи, скачивается ее содержание,
    статья добавляется в базу и удаляется из буфера
    */
    private void checkPoolSizeAndAddArtToDataBase() {
        HashMap<String, List<DownloadArticle>> copyMap = new HashMap<>(articlesMap);

        for(Map.Entry<String, List<DownloadArticle>> entrySet : copyMap.entrySet()){

            if (entrySet.getValue().size() >= articleProperty.getNumOfRecordsInNewsSite()) {
                log.info("Buffer for news site={} is overflow", entrySet.getKey());
                for (DownloadArticle downloadArticle : entrySet.getValue()) {

                    String contentOfArticle = restTemplate.getForObject(downloadArticle.getUrl(), String.class);

                    Article article = Article.builder()
                            .title(downloadArticle.getTitle())
                            .newsSite(downloadArticle.getNewsSite())
                            .publishedDate(downloadArticle.getPublishedAt())
                            .content(contentOfArticle)
                            .build();
                    saveArticleToDB(article);

                }
                articlesMap.remove(entrySet.getKey());
            }
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    protected void saveArticleToDB(Article article) {
        if (!articlesRepository.existsByTitleAndNewsSite(article.getTitle(), article.getNewsSite())) {
            articlesRepository.save(article);
            log.info("The article with title={} from news site={} is successfully saved", article.getTitle(), article.getNewsSite());
        } else {
            log.warn("The article with title={} from news site={} is already exists", article.getTitle(), article.getNewsSite());
        }
    }

}
