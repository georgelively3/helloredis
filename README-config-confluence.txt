h1. HelloRedis

A reference Spring Boot application demonstrating the minimum steps required to integrate Redis using Spring Data Redis.

The business domain (a *Dialog* entity) is intentionally simple. The goal is to demonstrate Redis integration patterns rather than domain-specific functionality.

This application supports both:

* Local Redis instances (development and testing)
* AWS ElastiCache Redis clusters using IAM authentication

---

h2. What This Demonstrates

* Spring Data Redis integration
* Redis repository implementation using *CrudRepository*
* Redis entity persistence using *@RedisHash*
* Environment-variable-based configuration
* Local Redis development support
* Testcontainers-based integration testing
* AWS ElastiCache cluster connectivity
* IAM authentication using AWS SigV4 tokens
* Conditional Spring configuration for multiple deployment models
* Service and controller layer patterns

---

h2. Project Structure

{code}
src/
main/
java/.../
config/
RedisConfig.java
ElastiCacheIAMTokenProvider.java

```
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
```

test/
java/.../
controller/
DialogControllerTest.java

```
  service/
    DialogServiceTest.java
```

karateTest/
java/.../karate/
DevKarateRunner.java

```
resources/karate/
  dialog.feature
```

{code}

|| Component || Purpose ||
| RedisConfig | Redis configuration and ElastiCache integration |
| ElastiCacheIAMTokenProvider | Generates AWS IAM authentication tokens |
| Dialog | Redis-persisted entity |
| DialogRepository | Spring Data repository |
| DialogService | Business logic layer |
| DialogController | REST API layer |
| application.yml | Redis configuration |
| DevKarateRunner | End-to-end integration testing |

---

h2. Step 1 – Dependencies

Add Spring Data Redis support.

{code:language=groovy}
dependencies {

```
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

}
{code}

Spring Boot automatically provides:

* Spring Data Redis
* Lettuce Redis client
* Redis auto-configuration
* Repository support

No additional Redis client libraries are required for standard Redis implementations.

---

h2. Step 2 – Redis Connectivity

The application supports two Redis deployment models.

|| Deployment Model || Use Case || Authentication ||
| Local Redis | Development and testing | None or static password |
| AWS ElastiCache | Cloud deployments | IAM authentication |

---

h3. Local Redis Configuration

For local development and Testcontainers integration tests, configure Redis using standard Spring Boot properties.

{code:language=yaml}
spring:
data:
redis:
host: ${REDIS_HOST:localhost}
port: ${REDIS_PORT:6379}
password: ${REDIS_PASSWORD:}
{code}

|| Environment Variable || Default || Purpose ||
| REDIS_HOST | localhost | Redis hostname |
| REDIS_PORT | 6379 | Redis port |
| REDIS_PASSWORD | (empty) | Redis password |

When running locally, Spring Boot's default Redis auto-configuration is used.

---

h3. AWS ElastiCache Configuration

To connect to AWS ElastiCache using IAM authentication, configure the following properties.

{code:language=yaml}
aws:
elasticache:
enabled: true
cluster-endpoint: your-cluster-endpoint.cache.amazonaws.com
cluster-name: your-cluster-name
region: us-east-1
port: 6379
iam-username: appuser
{code}

|| Property || Description ||
| aws.elasticache.enabled | Enables ElastiCache IAM mode |
| aws.elasticache.cluster-endpoint | Redis cluster endpoint |
| aws.elasticache.cluster-name | ElastiCache cluster name |
| aws.elasticache.region | AWS region |
| aws.elasticache.port | TLS port |
| aws.elasticache.iam-username | IAM-enabled Redis user |

These values are typically supplied through Helm values, environment variables, or platform configuration.

---

h3. RedisConfig

The application contains a custom Redis configuration class.

The configuration is activated only when:

{code}
aws.elasticache.enabled=true
{code}

When enabled, RedisConfig:

* Creates a custom LettuceConnectionFactory
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

---

h3. TLS Configuration

ElastiCache connections use TLS.

{code:language=java}
LettuceClientConfiguration.builder()
.useSsl()
.disablePeerVerification()
.build();
{code}

The current implementation disables peer verification to mirror the reference Python implementation.

For production deployments, certificate validation should typically remain enabled and trusted CA certificates should be configured appropriately.

---

h3. ElastiCacheIAMTokenProvider

AWS ElastiCache IAM authentication does not use static passwords.

Instead, the application generates a temporary authentication token using AWS Signature Version 4 (SigV4).

The ElastiCacheIAMTokenProvider:

# Obtains AWS credentials

# Builds a SigV4-signed request

# Generates a pre-signed authentication token

# Supplies the token as the Redis AUTH credential

This behavior is equivalent to Python's:

{code:language=python}
ElastiCacheIAMProvider(...)
{code}

---

h3. AWS Credential Sources

The token provider uses:

{code:language=java}
DefaultCredentialsProvider
{code}

Credentials may come from:

* EKS IAM Roles for Service Accounts (IRSA)
* EC2 instance profiles
* ECS task roles
* Environment variables
* Local AWS profiles

No AWS access keys should be hardcoded in the application.

---

h3. Token Expiration

AWS ElastiCache IAM tokens expire after 15 minutes.

The reference implementation generates a token during startup and establishes the Redis connection.

This approach is sufficient for:

* Demonstrations
* Reference implementations
* Proof-of-concept applications
* Short-lived workloads

Long-running production services should implement token refresh using:

* Scheduled recreation of the connection factory
* Lettuce RedisCredentialsProvider support
* Custom credential rotation logic

Without refresh logic, long-running applications may eventually lose connectivity when tokens expire.

---

h2. Step 3 – Enable Redis Repositories

{code:language=java}
@Configuration
@EnableRedisRepositories
public class RedisConfig {
}
{code}

@EnableRedisRepositories activates Spring Data Redis repository support.

This is conceptually equivalent to:

{code:language=java}
@EnableJpaRepositories
{code}

for JPA applications.

---

h2. Step 4 – Create a Redis Entity

{code:language=java}
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("dialogs")
public class Dialog implements Serializable {

```
@Id
private int id;

private String request;
private String response;
```

}
{code}

h3. Key Concepts

* *@RedisHash("dialogs")* stores each record as a Redis hash
* *@Id* identifies the primary key
* *Serializable* is recommended for persisted entities

Redis keys are stored using:

{code}
dialogs:<id>
{code}

Example:

{code}
dialogs:1
dialogs:2
dialogs:3
{code}

---

h3. Optional TTL Support

Entities can be configured with a time-to-live.

{code:language=java}
@RedisHash(value = "dialogs", timeToLive = 86400)
public class Dialog implements Serializable {
}
{code}

The example above expires records after 24 hours.

---

h2. Step 5 – Create the Repository

{code:language=java}
@Repository
public interface DialogRepository
extends CrudRepository<Dialog, Integer> {
}
{code}

Spring Data automatically generates the implementation.

Available operations include:

* save()
* findById()
* findAll()
* existsById()
* deleteById()
* count()

No implementation code is required.

---

h2. Step 6 – Service Layer

The service layer contains business logic and orchestrates repository access.

Example responsibilities:

* Retrieve all dialogs
* Retrieve a dialog by ID
* Create dialogs
* Update dialogs
* Delete dialogs

Keep business logic in the service layer and keep repositories focused solely on persistence concerns.

Because CrudRepository.findAll() returns an Iterable, convert it using StreamSupport when a List is required.

---

h2. Running the Application

h3. Prerequisites

* Java 21

and either:

* Local Redis

or

* Docker (for Testcontainers integration testing)

---

h3. Run with Local Redis

Start Redis:

{code:bash}
docker run -d -p 6379:6379 redis:7-alpine
{code}

Run the application:

{code:bash}
gradle bootRun
{code}

---

h3. Override Redis Settings

{code:bash}
REDIS_HOST=my-redis-host 
REDIS_PORT=6380 
gradle bootRun
{code}

---

h3. Run with AWS ElastiCache

Provide:

* aws.elasticache.enabled=true
* cluster endpoint
* cluster name
* region
* IAM username
* valid AWS credentials

The application automatically creates IAM authentication tokens and connects to the Redis cluster.

---

h2. Testing

h3. Unit Tests

Run:

{code:bash}
gradle test
{code}

Characteristics:

* No Redis instance required
* Mockito-based repository mocking
* Fast execution
* Suitable for CI pipelines

Examples:

* DialogServiceTest
* DialogControllerTest

---

h3. Integration Tests with Testcontainers

Run:

{code:bash}
gradle devTest
{code}

The integration suite:

* Starts Redis in Docker
* Launches Spring Boot
* Executes Karate feature tests
* Validates end-to-end functionality

Docker must be running.

---

h3. Integration Tests Against Deployed Environments

INT:

{code:bash}
gradle intTest -PbaseUrl=http://your-int-host
{code}

PreProd:

{code:bash}
gradle preprodTest -PbaseUrl=http://your-preprod-host
{code}

These tests execute against deployed infrastructure using a real Redis instance.

---

h2. Platform Integration

In platform environments, Redis configuration is typically supplied through BOM and Helm configuration.

Common platform-provided values include:

* Redis endpoint
* Redis port
* Cluster name
* AWS region
* IAM username

The platform is responsible for:

* Provisioning Redis infrastructure
* Supplying configuration values
* Providing AWS credentials (typically through IRSA)

The application is responsible for:

* Generating IAM authentication tokens
* Establishing Redis connections
* Executing Redis operations through Spring Data Redis

---

h2. Key Design Decisions

|| Decision || Rationale ||
| Spring Data Redis + CrudRepository | Minimal code and idiomatic Spring implementation |
| @RedisHash entities | Simple object persistence with optional TTL support |
| Environment-variable configuration | No hardcoded infrastructure configuration |
| Conditional ElastiCache configuration | Same application runs locally and in cloud environments |
| IAM authentication | Eliminates static Redis passwords |
| Testcontainers integration testing | Reproducible Redis testing without external dependencies |
| Mockito unit testing | Fast feedback and infrastructure-free tests |

---

h2. Summary

HelloRedis demonstrates a production-oriented Redis integration pattern for Spring Boot applications.

Features include:

* Spring Data Redis repositories
* Redis entity persistence
* Local Redis development support
* AWS ElastiCache integration
* IAM authentication
* Environment-based configuration
* Unit testing with Mockito
* Integration testing with Testcontainers

The result is a lightweight reference application that can be used as a starting point for Redis-enabled services across local, test, and cloud deployment environments.
