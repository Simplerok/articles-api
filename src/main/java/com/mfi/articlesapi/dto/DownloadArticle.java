package com.mfi.articlesapi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Builder
@Data
public class DownloadArticle {

    private Long id;
    private String title;
    private String url;
    private String imageUrl;
    private String newsSite;
    private String summary;
    private ZonedDateTime publishedAt;
    private ZonedDateTime updatedAt;
    private boolean featured;

}
