package com.lithespeed.helloredis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import com.lithespeed.helloredis.logger.MyLogger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    /**
     * ElastiCache cluster connection with IAM authentication and TLS.
     * Activated by setting ELASTICACHE_ENABLED=true.
     *
     * When disabled (default), Spring Boot auto-configuration uses
     * spring.data.redis.host/port for a standalone connection (local dev /
     * Testcontainers).
     *
     * NOTE: IAM tokens expire after 15 minutes. For long-running services,
     * implement a
     * scheduled refresh (e.g. @Scheduled every 10 minutes) that recreates the
     * factory
     * or look into Lettuce's RedisCredentialsProvider for seamless rotation.
     */
    @Bean
    @ConditionalOnProperty(name = "aws.elasticache.enabled", havingValue = "true")
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${aws.elasticache.cluster-endpoint}") String clusterEndpoint,
            @Value("${aws.elasticache.port}") int port,
            @Value("${aws.elasticache.iam-username}") String iamUsername,
            ElastiCacheIAMTokenProvider tokenProvider) {

        MyLogger.debugLog(
                "ElastiCache enabled — connecting to " + clusterEndpoint + ":" + port + " as user=" + iamUsername);

        String token;
        try {
            token = tokenProvider.generateToken();
            MyLogger.debugLog("IAM token generated successfully (first 20 chars: "
                    + token.substring(0, Math.min(20, token.length())) + ")");
        } catch (Exception e) {
            MyLogger.debugLog("Failed to generate IAM token: " + e.getClass().getName() + ": " + e.getMessage());
            throw e;
        }

        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
        clusterConfig.clusterNode(clusterEndpoint, port);
        clusterConfig.setUsername(iamUsername);
        clusterConfig.setPassword(RedisPassword.of(token));

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .disablePeerVerification() // mirrors ssl_cert_reqs=None; remove if CA cert is valid
                .and()
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    @Bean
    public String redisDebugProbe(@Value("${aws.elasticache.enabled:NOT_SET}") String enabled) {
        MyLogger.debugLog("RedisConfig loaded by Spring. aws.elasticache.enabled=" + enabled);
        return "redisDebugProbe";
    }
}
