# helloredis

A reference Spring Boot application demonstrating how to integrate Redis using Spring Data Redis with the `@RedisHash` / `CrudRepository` approach.

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
      config/         RedisConfig.java          — enables Redis repositories
      model/          Dialog.java               — @RedisHash entity
                      DialogResponseDTO.java     — API response shape
      repository/     DialogRepository.java      — CrudRepository interface
      service/        DialogService.java
      controller/     DialogController.java
    resources/
      application.yml                            — Redis connection config
  test/
    java/.../
      controller/     DialogControllerTest.java  — MockMvc unit tests
      service/        DialogServiceTest.java     — Mockito unit tests
  karateTest/
    java/.../karate/  DevKarateRunner.java       — integration tests (Testcontainers)
    resources/karate/ dialog.feature
```

---

## Step 1 — Gradle dependency

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

The `spring-boot-starter-data-redis` dependency pulls in Lettuce (the default Redis client) and all Spring Data Redis support.

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

Connection values are read from environment variables, with sensible local defaults. Never hardcode these values.

| Environment variable | Default     | Purpose              |
|----------------------|-------------|----------------------|
| `REDIS_HOST`         | `localhost` | Redis server host    |
| `REDIS_PORT`         | `6379`      | Redis server port    |
| `REDIS_PASSWORD`     | *(empty)*   | Redis auth password  |

---

## Step 3 — Enable Redis repositories (`RedisConfig.java`)

```java
@Configuration
@EnableRedisRepositories
public class RedisConfig {
}
```

`@EnableRedisRepositories` activates Spring Data Redis repository support, the equivalent of `@EnableJpaRepositories` for JPA. This is all the configuration required when using `@RedisHash`.

---

## Step 4 — Annotate the model (`Dialog.java`)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("dialogs")          // stores under the "dialogs" hash key namespace
public class Dialog implements Serializable {

    @Id                        // marks the primary key; drives key generation
    private int id;
    private String request;
    private String response;
}
```

Key points:

- `@RedisHash("dialogs")` — Spring Data Redis stores each entity as a Redis hash at the key `dialogs:<id>`. It also maintains an index set at `dialogs` containing all known ids.
- `@Id` — required; tells Spring Data which field is the primary key. Supported types: `String`, `int`/`Integer`, `long`/`Long`.
- `implements Serializable` — recommended for Redis entities.
- All fields are stored. Jackson annotations (`@JsonView`, `@JsonIgnoreProperties`) are ignored by Redis serialization; they only apply to HTTP JSON responses.

### Optional: TTL (time-to-live)

To expire all records in the hash after a fixed duration:

```java
@RedisHash(value = "dialogs", timeToLive = 86400)  // seconds — 24 hours
public class Dialog implements Serializable { ... }
```

Each entity key is set to expire independently. When the TTL elapses, Redis removes the hash entry automatically.

---

## Step 5 — Implement the repository (`DialogRepository.java`)

```java
@Repository
public interface DialogRepository extends CrudRepository<Dialog, Integer> {
}
```

`CrudRepository` provides `save`, `findById`, `findAll`, `existsById`, `deleteById`, and `count` — no implementation code required. Spring Data Redis generates it at startup.

---

## Step 6 — Service layer (`DialogService.java`)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DialogService {

    private final DialogRepository dialogRepository;

    public List<Dialog> getAllDialogs() {
        return StreamSupport.stream(dialogRepository.findAll().spliterator(), false).toList();
    }

    public DialogResponseDTO getDialogByIdAndRequest(int id, String request) {
        if (request == null) {
            log.warn("getDialogByIdAndRequest called with null request for id={}", id);
            return null;
        }
        return dialogRepository.findById(id)
                .filter(dialog -> request.equals(dialog.getRequest()))
                .map(dialog -> new DialogResponseDTO(dialog.getId(), dialog.getResponse()))
                .orElse(null);
    }
    ...
}
```

Notes:

- `CrudRepository.findAll()` returns `Iterable<Dialog>`, not `List`. Use `StreamSupport.stream(...)` to convert.
- The service owns the business rule "request must match" — the repository is kept clean.
- `DialogResponseDTO` projects only the fields the API caller needs (`id` + `response`), decoupling the Redis entity shape from the API contract.

---

## REST API

| Method | Path                        | Description                              |
|--------|-----------------------------|------------------------------------------|
| GET    | `/api/dialogs`              | Return all dialogs                       |
| GET    | `/api/dialogs/{id}?request=` | Return response if id + request match   |
| POST   | `/api/dialogs`              | Create a dialog                          |
| PUT    | `/api/dialogs/{id}`         | Update a dialog by id                    |
| DELETE | `/api/dialogs/{id}`         | Delete a dialog by id                    |

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the app is running.

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

The unit tests use Mockito to mock `DialogRepository`. Redis is never contacted.

### Integration tests (embedded Redis via Testcontainers — requires Docker)

```bash
gradle devTest
```

`DevKarateRunner` spins up a real Redis container using Testcontainers, starts the Spring Boot app on a random port, and runs the Karate feature scenarios end-to-end. Docker must be running.

### Integration tests against a deployed environment

```bash
gradle intTest -PbaseUrl=http://your-int-host
gradle preprodTest -PbaseUrl=http://your-preprod-host
```

These require a live Redis instance accessible to the target environment.

---

## Key design decisions

| Decision | Rationale |
|---|---|
| `@RedisHash` + `CrudRepository` | Less code, per-entity TTL support, idiomatic Spring Data |
| Env-var-based connection config | No hardcoded credentials; works across local/int/preprod |
| `DialogResponseDTO` | Decouples the Redis entity from the API response shape |
| Testcontainers for integration tests | No external Redis dependency in CI; reproducible |
| Mockito unit tests for service/controller | Fast feedback; repository behaviour is covered by integration tests |
