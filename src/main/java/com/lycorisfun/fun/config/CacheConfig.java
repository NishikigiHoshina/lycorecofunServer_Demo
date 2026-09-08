package com.lycorisfun.fun.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 本地缓存（Caffeine）配置
 *
 * 缓存策略：cache-aside。读方法 @Cacheable 命中缓存；写方法 @CacheEvict 主动失效，
 * TTL 仅作兜底。各 region 名称需与 Service 注解的 cacheNames 一致。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /* 缓存 region 名 */
    public static final String CACHE_NEWS_LATEST     = "newsLatest";     // /newslist
    public static final String CACHE_NEWS_ALL        = "newsAll";        // /newslistAll
    public static final String CACHE_ANNOUNCEMENT    = "announcement";   // /getAnnouncement
    public static final String CACHE_FUNC_STATUS     = "funcStatus";     // /getfuncstatus
    public static final String CACHE_INDEX_IMG       = "indexImg";       // /getIndexIMG
    public static final String CACHE_POST_PAGE       = "postPage";       // /postlistPage
    public static final String CACHE_POST_DETAIL     = "postDetail";     // /getPostByid
    public static final String CACHE_REPLY_LIST      = "replyList";      // /getReply

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);   // 方法返回 null 不缓存
        register(manager, CACHE_NEWS_LATEST,     20,  Duration.ofMinutes(10));
        register(manager, CACHE_NEWS_ALL,       100,  Duration.ofMinutes(10));
        register(manager, CACHE_ANNOUNCEMENT,    10,  Duration.ofMinutes(30));
        register(manager, CACHE_FUNC_STATUS,    100,  Duration.ofMinutes(30));
        register(manager, CACHE_INDEX_IMG,       20,  Duration.ofMinutes(30));
        register(manager, CACHE_POST_PAGE,      500,  Duration.ofMinutes(5));
        register(manager, CACHE_POST_DETAIL,    500,  Duration.ofMinutes(10));
        register(manager, CACHE_REPLY_LIST,     500,  Duration.ofMinutes(3));
        return manager;
    }

    private void register(CaffeineCacheManager manager, String name, int maxSize, Duration ttl) {
        manager.registerCustomCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl)
                .recordStats()          // 便于监控命中率
                .build());
    }
}
