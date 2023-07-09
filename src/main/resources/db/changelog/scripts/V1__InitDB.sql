
CREATE TABLE articles(
	id                  bigserial         PRIMARY KEY,
	title               text              NOT NULL,
	news_site           text              NOT NULL,
	published_date      timestamp         NOT NULL ,
	article             text              NOT NULL,
	CONSTRAINT uniq_article UNIQUE (title, news_site)
);





