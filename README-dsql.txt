# HelloDSQL

## Overview

HelloDSQL is a reference Spring Boot application demonstrating how to connect a Java application to Amazon Aurora DSQL using the Platform Engineering reference architecture.

The application intentionally mirrors the HelloJava (Aurora PostgreSQL) example as closely as possible. The goal is to demonstrate that, for many applications, migrating from Aurora PostgreSQL to Aurora DSQL requires very few code changes.

The application implements a simple CRUD API using the familiar Spring Boot architecture:

```
Controller
    ↓
Service
    ↓
Repository (Spring Data JPA)
    ↓
Aurora DSQL
```

The application contains a single table populated with a small amount of seed data.

---

# Comparison with HelloJava (Aurora PostgreSQL)

| Component | HelloJava | HelloDSQL |
|----------|-----------|-----------|
| Spring Boot | ✓ | ✓ |
| Spring Data JPA | ✓ | ✓ |
| Flyway | ✓ | ✓ |
| H2 Local Development | ✓ | ✓ |
| Controller/Service/Repository | ✓ | ✓ |
| Dockerfile | ✓ | ✓ |
| Kubernetes Deployment | ✓ | ✓ |
| Helm Charts | ✓ | ✓ |
| BOM Configuration | ✓ | ✓ |

Nearly all application code is identical.

The primary differences are infrastructure configuration.

---

# What Changes?

## 1. BOM / Helm Configuration

The workload is configured to provision (or reference) an Aurora DSQL cluster rather than an Aurora PostgreSQL database.

Platform-specific values such as:

- cluster endpoint
- database name
- region
- authentication configuration

are provided through the BOM and Helm charts.

The application should never hardcode these values.

---

## 2. application.yml

The datasource configuration references values injected by the deployment environment.

Example:

```yaml
spring:

  datasource:

    url: ${DB_URL}

    username: ${DB_USERNAME}

    password: ${DB_PASSWORD}

    driver-class-name: org.postgresql.Driver

  jpa:

    hibernate:
      ddl-auto: none

    open-in-view: false

  flyway:

    enabled: true
```

In most environments, these values are injected by the Platform through Helm values and Kubernetes Secrets.

No environment-specific values should be committed to source control.

---

## 3. Local Development

For local development and automated testing, the application uses H2.

Example:

```yaml
spring:

  config:

    activate:

      on-profile: local

  datasource:

    url: jdbc:h2:mem:testdb;MODE=PostgreSQL

    driver-class-name: org.h2.Driver

    username: sa

    password:
```

Using H2 allows developers to:

- run locally
- execute unit tests
- develop without provisioning Aurora DSQL

The deployed environments use Aurora DSQL.

---

## 4. Flyway

Flyway is used to create the application schema and seed the demonstration data.

Example schema:

```sql
CREATE TABLE dialog (

    id BIGINT PRIMARY KEY,

    request VARCHAR(255) NOT NULL,

    response VARCHAR(255) NOT NULL

);
```

Seed data inserts fixed identifiers:

```sql
INSERT INTO dialog VALUES
(1,'Hello','Bonjour'),
(2,'Goodbye','Au revoir');
```

The example intentionally uses application-assigned identifiers rather than generated identities.

---

## 5. Spring Data

The repository layer is unchanged.

Example:

```java
public interface DialogRepository
        extends JpaRepository<Dialog, Long> {
}
```

No DSQL-specific repository implementation is required.

---

# Design Philosophy

HelloDSQL intentionally avoids advanced database features.

The objective is to demonstrate Platform integration rather than database-specific capabilities.

The example avoids:

- generated identities
- sequences
- triggers
- stored procedures
- PostgreSQL extensions
- complex schemas

This keeps the example easy to understand while demonstrating the recommended deployment pattern.

---

# Platform Responsibilities

The Platform supplies:

- Aurora DSQL provisioning
- Kubernetes deployment
- Secrets
- Environment configuration
- Networking
- Authentication
- Helm configuration
- BOM configuration

The application simply consumes the injected configuration.

---

# Summary

For applications with straightforward relational data access, migrating from Aurora PostgreSQL to Aurora DSQL often requires few or no application code changes.

In most cases the primary differences are:

- deployment configuration
- datasource configuration
- infrastructure provisioning

The Spring Boot application architecture remains unchanged.