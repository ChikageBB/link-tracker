package chikagebb.linktracker.scrapper.config;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

import chikagebb.linktracker.scrapper.properties.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

@Configuration
@EnableCaching
public class ValkeyConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true")
    public RedisCacheManager cacheManager(RedisConnectionFactory factory, CacheProperties props) {
        var baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(fromSerializer(new GenericJackson2JsonRedisSerializer()));

        var linksConfig = baseConfig.entryTtl(props.getLinksTtl());

        return RedisCacheManager.builder(factory)
                .cacheDefaults(baseConfig)
                .withCacheConfiguration("links", linksConfig)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "false")
    public CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }
}
