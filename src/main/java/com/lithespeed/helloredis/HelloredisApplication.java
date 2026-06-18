package com.lithespeed.helloredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.lithespeed.helloredis.logger.MyLogger;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@SpringBootApplication
public class HelloredisApplication implements ApplicationRunner {

	@Value("${aws.elasticache.enabled:NOT_SET}")
	private String elasticacheEnabled;

	@Value("${aws.elasticache.cluster-endpoint:NOT_SET}")
	private String clusterEndpoint;

	@Value("${aws.elasticache.cluster-name:NOT_SET}")
	private String clusterName;

	@Value("${aws.elasticache.region:NOT_SET}")
	private String region;

	@Value("${aws.elasticache.iam-username:NOT_SET}")
	private String iamUsername;

	@Value("${aws.elasticache.port:NOT_SET}")
	private String port;

	public static void main(String[] args) {
		SpringApplication.run(HelloredisApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) {
		MyLogger.debugLog("=== Resolved application.yml properties ===");
		MyLogger.debugLog("aws.elasticache.enabled=" + elasticacheEnabled);
		MyLogger.debugLog("aws.elasticache.cluster-endpoint=" + clusterEndpoint);
		MyLogger.debugLog("aws.elasticache.cluster-name=" + clusterName);
		MyLogger.debugLog("aws.elasticache.region=" + region);
		MyLogger.debugLog("aws.elasticache.iam-username=" + iamUsername);
		MyLogger.debugLog("aws.elasticache.port=" + port);
	}
}
