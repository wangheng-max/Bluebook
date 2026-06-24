# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Compile and run tests
mvn clean test

# Compile only (skip tests)
mvn clean compile -DskipTests

# Run a single test class
mvn -Dtest=RedisTest test

# Run a single test method
mvn -Dtest=RedisTest#testMethodName test

# Package as JAR
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
# or after packaging:
java -jar target/big-event-1.0-SNAPSHOT.jar
```

The application starts on **port 8080** and connects to a local MySQL database `big_event` and Redis on `localhost:6379`.

## Architecture Overview

This is a Spring Boot 3.5.9 REST API for an article/blog management system ("Big Event" / 大事件). It follows a standard layered architecture: **Controller → Service → Mapper (MyBatis)**.

### Authentication Flow

1. **Login**: `POST /user/login` validates credentials (username/password hashed with MD5), generates a JWT (auth0 `java-jwt`), stores a copy in Redis with a 1-hour TTL, and returns the token.
2. **Every other request**: `LoginInterceptor` extracts the `Authorization` header, verifies the token exists in Redis, parses the JWT claims, and stores them in `ThreadLocalUtil` for the duration of the request. Expired/missing tokens get a 401 response.
3. **Excluded endpoints**: `/user/login` and `/user/register` bypass the interceptor (configured in `WebConfig`).
4. **Password changes** invalidate the token by deleting it from Redis.

### Key Design Patterns

- **ThreadLocal user context**: Services never receive the current user as a parameter. Instead, they call `ThreadLocalUtil.get()` to retrieve the JWT claims map (containing `id` and `username`), which was set by `LoginInterceptor`. The interceptor's `afterCompletion` clears it to prevent memory leaks.
- **Unified response wrapper**: Every endpoint returns `Result<T>` (fields: `code` — 0=success, 1=failure; `message`; `data`). Static factory methods `Result.success(data)` and `Result.error(message)` are used everywhere.
- **Global exception handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) catches all unhandled exceptions and wraps them in `Result.error()`.
- **Custom validation**: `@State` annotation + `StateValidation` validator constrains the `Article.state` field to `"已发布"` or `"草稿"`. `Category` uses validation groups (`Add` extends `Default`, `Update` extends `Default`) so that `@NotNull` on `id` only applies during updates.
- **Pagination**: PageHelper (`com.github.pagehelper`) is used via `PageHelper.startPage(pageNum, pageSize)` before the mapper call. The returned `List` is cast to `Page<Article>` to extract `getTotal()` and `getResult()`.

### Package Layout

| Package | Purpose |
|---|---|
| `com.itheima.controller` | REST controllers (`UserController`, `ArticleController`, `CategoryController`, `FileUploadController`) |
| `com.itheima.service` / `service.impl` | Business logic interfaces and implementations |
| `com.itheima.mapper` | MyBatis mapper interfaces (mix of annotation-based SQL and XML for dynamic queries) |
| `com.itheima.pojo` | Entity classes (`User`, `Article`, `Category`) and response types (`Result<T>`, `PageBean<T>`) |
| `com.itheima.config` | Spring configuration (`WebConfig` — interceptor registration) |
| `com.itheima.interceptors` | `LoginInterceptor` — JWT + Redis token validation |
| `com.itheima.utils` | `JwtUtil`, `Md5Util`, `ThreadLocalUtil`, `AliOssUtil` |
| `com.itheima.anno` | Custom annotations (`@State`) |
| `com.itheima.validation` | Custom validators (`StateValidation`) |
| `com.itheima.exception` | `GlobalExceptionHandler` |

### Database & MyBatis

- **MySQL** database `big_event` at `localhost:3306`, user `root`.
- MyBatis uses `map-underscore-to-camel-case: true` for automatic column-to-field mapping.
- Simple CRUD uses **annotation-based SQL** (e.g., `@Select`, `@Insert`, `@Update`, `@Delete`).
- The only **XML mapper** is `ArticleMapper.xml` — the `list` method uses dynamic SQL (`<where>` + `<if>`) to optionally filter by `categoryId` and `state`, while always filtering by `create_user`.

### File Upload

`POST /upload` accepts `MultipartFile`, generates a UUID-based filename, and uploads to **Alibaba Cloud OSS** (bucket: `biggetst-event`, region: Beijing). Note: the `AliOssUtil` class contains **hardcoded AccessKey credentials** — these should be externalized to environment variables or a secrets manager.

### External Dependencies

- **MySQL** (local, port 3306, database `big_event`)
- **Redis** (local, port 6379) — used solely for token session storage
- **Alibaba Cloud OSS** — file upload storage
