package com.stan.stancore.extended.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class CacheService {

    private static final Integer EXPIRE_MINUTES = 10;
    private LoadingCache<String, String> loadingCache;

    private CacheService() {
        loadingCache =
            CacheBuilder
                .newBuilder()
                .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build(
                    new CacheLoader<String, String>() {
                        public String load(String key) {
                            return null;
                        }
                    }
                );
    }

    private static class SingletonHelper {

        private static final CacheService INSTANCE = new CacheService();
    }

    public static CacheService getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public void put(String key, String value) {
        loadingCache.put(key, value);
    }

    public String get(String key) {
        try {
            return loadingCache.get(key);
        } catch (Exception e) {
            return null;
        }
    }
}
