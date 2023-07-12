package com.mfi.articlesapi.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "articles-api")
public class ArticleProperty {

    private int numOfThreads;
    private int articlesThreadLimit;
    private int articlesTotalLimit;
    private Set<String> blackList;
    private int numOfRecordsInNewsSite;
    private int periodOfStartingThreads;

}
