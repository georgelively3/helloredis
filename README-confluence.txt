h1. HelloRedis

A reference Spring Boot application demonstrating the minimum steps required to integrate Redis using Spring Data Redis.

The business domain (a *Dialog* entity) is intentionally simple. The goal is to demonstrate the Redis integration pattern rather than domain-specific functionality.

---

h2. What This Demonstrates

* Adding the Spring Data Redis dependency
* Configuring Redis connectivity through environment variables
* Annotating a model class for Redis storage using *@RedisHash*
* Implementing a repository with zero custom code using *CrudRepository*
* Wiring the repository through a service and REST controller
* Unit testing with mocked repositories (no Redis instance required)
* Integration testing using Testcontainers and Redis

---

h2. Project Structure

{code}
src/
main/
java/.../
config/       RedisConfig.java
model/        YourEntity.java
repository/   YourRepository.java
service/      YourService.java
controller/   YourController.java

```
resources/
  application.yml
```

test/
java/.../
controller/   YourControllerTest.java
service/      YourServiceTest.java

karateTest/
java/.../karate/
DevKarateRunner.java

```
resources/karate/
  your.feature
```

{code}

|| Component || Purpose ||
| RedisConfig.java | Enables Redis repository support |
| YourEntity.java | Redis-persisted entity |
| YourRepository.java | Spring Data repository interface |
| YourService.java | Business logic layer |
| YourController.java | REST API layer |
| application.yml | Redis connection configuration |
| DevKarateRunner.java | End-to-end integration testing |

---

h2. Step 1 – Add Spring Data Redis Dependency

{code:language=groovy}
dependencies {
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.boot:spring-boot-starter-web'
}
{code}

The *spring-boot-starter-data-redis* dependency pulls in:

* Spring Data Redis
* Lettuce (default Redis client)
* Spring Boot auto-configuration support

No additional Redis client libraries are required for standard implementations.

---

h2. Step 2 – Configure Redis Connectivity

Configure Redis using environment variables in *application.yml*.

{code:language=yaml}
spring:
data:
redis:
host: ${REDIS_HOST:localhost}
port: ${REDIS_PORT:6379}
password: ${REDIS_PASSWORD:}
{code}

|| Environment Variable || Default Value || Purpose ||
| REDIS_HOST | localhost | Redis server hostname |
| REDIS_PORT | 6379 | Redis server port |
| REDIS_PASSWORD | (empty) | Redis authentication password |

*Best Practice:* Never hardcode Redis connection information or credentials.

---

h2. Step 3 – Enable Redis Repositories

{code:language=java}
@Configuration
@EnableRedisRepositories
public class RedisConfig {
}
{code}

The *@EnableRedisRepositories* annotation activates Spring Data Redis repository support, similar to *@EnableJpaRepositories* for JPA applications.

For applications using *@RedisHash*, this is typically the only Redis-specific configuration class required.

---

h2. Step 4 – Create a Redis Entity

{code:language=java}
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("your_entities")
public class YourEntity implements Serializable {

```
@Id
private int id;

private String fieldOne;
private String fieldTwo;
```

}
{code}

h3. Key Concepts

* *@RedisHash("your_entities")*
  ** Stores each entity as a Redis hash
  ** Keys are stored using the format:
  {code}
  your_entities:<id>
  {code}

* *@Id*
  ** Required field
  ** Supported types include:
  *** String
  *** Integer/int
  *** Long/long

* *Serializable*
  ** Recommended for Redis-persisted entities

* Jackson annotations such as *@JsonView* and *@JsonIgnoreProperties* affect only REST serialization and do not affect Redis persistence.

h3. Optional TTL Support

Redis entities can be configured with a time-to-live (TTL).

{code:language=java}
@RedisHash(value = "your_entities", timeToLive = 86400)
public class YourEntity implements Serializable {
}
{code}

In this example, records automatically expire after 24 hours.

---

h2. Step 5 – Define the Repository

{code:language=java}
@Repository
public interface YourRepository
extends CrudRepository<YourEntity, Integer> {
}
{code}

Spring Data automatically generates the implementation.

Available methods include:

* save()
* findById()
* findAll()
* existsById()
* deleteById()
* count()

No implementation code is required.

---

h2. Step 6 – Implement the Service Layer

{code:language=java}
@Service
@RequiredArgsConstructor
public class YourService {

```
private final YourRepository yourRepository;

public List<YourEntity> getAll() {
    return StreamSupport
            .stream(yourRepository.findAll().spliterator(), false)
            .toList();
}

public Optional<YourEntity> getById(int id) {
    return yourRepository.findById(id);
}

public YourEntity create(YourEntity entity) {
    return yourRepository.save(entity);
}

public Optional<YourEntity> update(int id, YourEntity updated) {

    if (!yourRepository.existsById(id)) {
        return Optional.empty();
    }

    updated.setId(id);
    return Optional.of(yourRepository.save(updated));
}

public boolean delete(int id) {

    if (!yourRepository.existsById(id)) {
        return false;
    }

    yourRepository.deleteById(id);
    return true;
}
```

}
{code}

h3. Notes

* *CrudRepository.findAll()* returns an *Iterable<T>* rather than a *List<T>*
* Use *StreamSupport* to convert the result to a list
* Keep business logic in the service layer
* Keep repositories focused on persistence concerns
* Consider using response DTOs to decouple API contracts from Redis entities

---

h2. Running the Application

h3. Prerequisites

* Java 21
* Redis instance (local or remote)

OR

* Docker (for integration testing with Testcontainers)

h3. Run with Local Redis

Start Redis:

{code:bash}
docker run -d -p 6379:6379 redis:7-alpine
{code}

Run the application:

{code:bash}
gradle bootRun
{code}

h3. Override Connection Settings

{code:bash}
REDIS_HOST=my-redis-host 
REDIS_PORT=6380 
gradle bootRun
{code}

---

h2. Testing

h3. Unit Tests

Run:

{code:bash}
gradle test
{code}

Characteristics:

* No Redis instance required
* Repository mocked using Mockito
* Fast execution
* Suitable for CI pipelines

Examples:

* DialogServiceTest
* DialogControllerTest

h3. Integration Tests Using Testcontainers

Run:

{code:bash}
gradle devTest
{code}

The integration suite:

* Starts Redis in a Docker container
* Launches the Spring Boot application
* Executes Karate end-to-end tests
* Requires Docker to be running

h3. Integration Tests Against Deployed Environments

INT:

{code:bash}
gradle intTest -PbaseUrl=http://your-int-host
{code}

PreProd:

{code:bash}
gradle preprodTest -PbaseUrl=http://your-preprod-host
{code}

These tests execute against deployed environments backed by real Redis infrastructure.

---

h2. Key Design Decisions

|| Decision || Rationale ||
| @RedisHash + CrudRepository | Minimal code, idiomatic Spring Data Redis, supports TTL |
| Environment-variable configuration | Eliminates hardcoded connection information and credentials |
| Response DTO layer | Decouples API contracts from persistence models |
| Testcontainers integration testing | Reproducible testing without external Redis dependencies |
| Mockito-based unit testing | Fast feedback and infrastructure-free testing |

---

h2. Summary

This reference implementation demonstrates the simplest production-oriented approach for integrating Redis into a Spring Boot application:

* Spring Data Redis for persistence
* Environment-variable-based configuration
* Repository pattern via CrudRepository
* Service and controller layering
* Unit testing with Mockito
* Integration testing with Testcontainers

The result is a lightweight, maintainable Redis implementation that follows standard Spring Boot conventions and can be adapted easily for platform onboarding demonstrations.
