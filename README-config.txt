# HelloRedis

A reference Spring Boot application demonstrating the minimum steps required to integrate Redis using Spring Data Redis.

The business domain (a `Dialog` entity) is intentionally simple. The goal is to demonstrate Redis integration patterns rather than domain-specific functionality.

This application supports both:

* Local Redis instances (development and testing)
* AWS ElastiCache Redis clusters using IAM authentication

---

## What This Demonstrates

* Spring Data Redis integration
* Redis repository implementation using `CrudRepository`
* Redis entity persistence using `@RedisHash`
* Environment-variable-based configuration
* Local Redis development support
* Testcontainers-based integration testing
* AWS ElastiCache cluster connectivity
* IAM authentication using AWS SigV4 tokens
* Conditional Spring configuration for multiple deployment models
* Service and controller layer patterns

---

## Project Structure

```text
src/
  main/
    java/.../
      config/
        RedisConfig.java
        ElastiCacheIAMTokenProvider.java

      model/
        Dialog.java

      repository/
        DialogRepository.java

      service/
        DialogService.java

      controller/
        DialogController.java

    resources/
      application.yml

  test/
    java/.../
      controller/
        DialogControllerTest.java

      service/
        DialogServiceTest.java

  karateTest/
    java/.../karate/
      DevKarateRunner.java

    resources/karate/
      dialog.feature
```

| Component                   | Purpose                                         |
| --------------------------- | ----------------------------------------------- |
| RedisConfig                 | Redis configuration and ElastiCache integration |
| ElastiCacheIAMTokenProvider | Generates AWS IAM authentication tokens         |
| Dialog                      | Redis-persisted entity                          |
| DialogRepository            | Spring Data repository                          |
| DialogService               | Business logic layer                            |
| DialogController            | REST API layer                                  |
| application.yml             | Redis configuration                             |
| DevKarateRunner             | End-to-end integration testing                  |

---

## Step 1 – Dependencies

Add Spring Data Redis support.

```groovy
dependencies {

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

}
```

Spring Boot automatically provides:

* Spring Data Redis
* Lettuce Redis client
* Redis auto-configuration
* Repository support

No additional Redis client libraries are required for standard Redis implementations.

---

## Step 2 – Redis Connectivity

The application supports two Redis deployment models.

| Deployment Model | Use Case                | Authentication          |
| ---------------- | ----------------------- | ----------------------- |
| Local Redis      | Development and testing | None or static password |
| AWS ElastiCache  | Cloud deployments       | IAM authentication      |

### Local Redis Configuration

For local development and Testcontainers integration tests, configure Redis using standard Spring Boot properties.

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

| Environment Variable | Default   | Purpose        |
| -------------------- | --------- | -------------- |
| REDIS_HOST           | localhost | Redis hostname |
| REDIS_PORT           | 6379      | Redis port     |
| REDIS_PASSWORD       | (empty)   | Redis password |

When running locally, Spring Boot's default Redis auto-configuration is used.

### AWS ElastiCache Configuration

To connect to AWS ElastiCache using IAM authentication, configure the following properties.

```yaml
aws:
  elasticache:
    enabled: true
    cluster-endpoint: your-cluster-endpoint.cache.amazonaws.com
    cluster-name: your-cluster-name
    region: us-east-1
    port: 6379
    iam-username: appuser
```

| Property                         | Description                  |
| -------------------------------- | ---------------------------- |
| aws.elasticache.enabled          | Enables ElastiCache IAM mode |
| aws.elasticache.cluster-endpoint | Redis cluster endpoint       |
| aws.elasticache.cluster-name     | ElastiCache cluster name     |
| aws.elasticache.region           | AWS region                   |
| aws.elasticache.port             | TLS port                     |
| aws.elasticache.iam-username     | IAM-enabled Redis user       |

These values are typically supplied through Helm values, environment variables, or platform configuration.

### RedisConfig

The application contains a custom Redis configuration class.

The configuration is activated only when:

```text
aws.elasticache.enabled=true
```

When enabled, `RedisConfig`:

* Creates a custom `LettuceConnectionFactory`
* Connects to a Redis cluster endpoint
* Enables TLS
* Generates IAM authentication tokens
* Authenticates using an IAM-enabled Redis user

When disabled, Spring Boot automatically falls back to standard Redis configuration.

This allows the same application binary to run:

* Locally
* In Testcontainers
* In cloud environments using ElastiCache

without code changes.
