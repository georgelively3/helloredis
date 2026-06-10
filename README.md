# helloredis

A reference Spring Boot application showing the minimum steps required to integrate Redis using Spring Data Redis. The business domain (a `Dialog` entity) is intentionally simple — the goal is to demonstrate the Redis wiring pattern, not the domain logic.

---

## What this demonstrates

- Adding the Spring Data Redis dependency
- Configuring the Redis connection via environment variables
- Annotating a model class for Redis storage with `@RedisHash`
- Implementing a repository with zero custom code using `CrudRepository`
- Wiring the repository through a service and REST controller
- Unit testing with mocked repositories (no Redis required)
- Integration testing with an embedded Redis via Testcontainers

---

## Project structure

```
src/
  main/
    java/.../
      config/       RedisConfig.java      — enables Redis repositories
      model/        YourEntity.java       — @RedisHash entity
      repository/   YourRepository.java  — CrudRepository interface
      service/      YourService.java
      controller/   YourController.java
    resources/
      application.yml                    — Redis connection config
  test/
    java/.../
      controller/   YourControllerTest.java  — MockMvc unit tests
      service/      YourServiceTest.java     — Mockito unit tests
  karateTest/
    java/.../karate/  DevKarateRunner.java   — integration tests (Testcontainers)
    resources/karate/ your.feature
```

---

## Step 1 — Gradle dependency

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

`spring-boot-starter-data-redis` pulls in Lettuce (the default Redis client) and all Spring Data Redis support. No additional client configuration is required for standard use.

---

## Step 2 — Connection configuration (`application.yml`)

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

Read connection values from environment variables, with sensible local defaults. Never hardcode these.

| Environment variable | Default     | Purpose             |
|----------------------|-------------|---------------------|
| `REDIS_HOST`         | `localhost` | Redis server host   |
| `REDIS_PORT`         | `6379`      | Redis server port   |
| `REDIS_PASSWORD`     | *(empty)*   | Redis auth password |

---

## Step 3 — Enable Redis repositories

```java
@Configuration
@EnableRedisRepositories
public class RedisConfig {
}
```

`@EnableRedisRepositories` activates Spring Data Redis repository support — equivalent to `@EnableJpaRepositories` for JPA. This is the only configuration class required when using `@RedisHash`.

---

## Step 4 — Annotate your entity

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("your_entities")        // namespace for keys in Redis
public class YourEntity implements Serializable {

    @Id                            // primary key; drives key generation
    private int id;

    private String fieldOne;
    private String fieldTwo;
}
```

Key points:

- **`@RedisHash("your_entities")`** — each instance is stored as a Redis hash at the key `your_entities:<id>`. Spring Data also maintains an index set at `your_entities` containing all known ids.
- **`@Id`** — required. Supported types: `String`, `int`/`Integer`, `long`/`Long`.
- **`implements Serializable`** — recommended.
- All fields are stored regardless of any Jackson annotations (`@JsonView`, `@JsonIgnoreProperties` etc.), which only affect HTTP JSON serialization, not Redis storage.

### Optional: TTL (time-to-live)

```java
@RedisHash(value = "your_entities", timeToLive = 86400)  // seconds — 24 hours
public class YourEntity implements Serializable { ... }
```

Each entity key expires independently. Redis removes it automatically when the TTL elapses.

---

## Step 5 — Define the repository

```java
@Repository
public interface YourRepository extends CrudRepository<YourEntity, Integer> {
}
```

`CrudRepository` provides `save`, `findById`, `findAll`, `existsById`, `deleteById`, and `count` with no implementation code. Spring Data Redis generates the implementation at startup.

---

## Step 6 — Service layer

```java
@Service
@RequiredArgsConstructor
public class YourService {

    private final YourRepository yourRepository;

    public List<YourEntity> getAll() {
        // CrudRepository.findAll() returns Iterable — convert with StreamSupport
        return StreamSupport.stream(yourRepository.findAll().spliterator(), false).toList();
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
}
```

Notes:
- `CrudRepository.findAll()` returns `Iterable<T>`, not `List<T>`. Use `StreamSupport.stream(...)` to convert.
- Keep business logic in the service. The repository should remain a plain `CrudRepository` interface unless you need custom query methods.
- Consider a dedicated response DTO to decouple the Redis entity shape from the API contract (see `DialogResponseDTO` in this project for an example).

---

## Running the application

### Prerequisites

- Java 21
- A running Redis instance **or** Docker (for Testcontainers-based integration tests)

### With a local Redis

```bash
# Start Redis (Docker)
docker run -d -p 6379:6379 redis:7-alpine

# Run the app
gradle bootRun
```

### Environment variable overrides

```bash
REDIS_HOST=my-redis-host REDIS_PORT=6380 gradle bootRun
```

---

## Testing

### Unit tests (no Redis required)

```bash
gradle test
```

Mock `YourRepository` with Mockito. Redis is never contacted. See `DialogServiceTest` and `DialogControllerTest` for examples.

### Integration tests (embedded Redis via Testcontainers — requires Docker)

```bash
gradle devTest
```

`DevKarateRunner` spins up a real Redis container, starts the Spring Boot app on a random port, and runs Karate feature scenarios end-to-end. Docker must be running.

### Integration tests against a deployed environment

```bash
gradle intTest -PbaseUrl=http://your-int-host
gradle preprodTest -PbaseUrl=http://your-preprod-host
```

These target a live environment with a real Redis instance.

---

## Key design decisions

| Decision | Rationale |
|---|---|
| `@RedisHash` + `CrudRepository` | Minimal code; per-entity TTL support; idiomatic Spring Data |
| Env-var-based connection config | No hardcoded credentials; works across local / int / preprod |
| Response DTO | Decouples the Redis entity shape from the API response contract |
| Testcontainers for integration tests | No external Redis dependency in CI; reproducible across machines |
| Mockito unit tests for service/controller | Fast feedback; no infrastructure needed |
