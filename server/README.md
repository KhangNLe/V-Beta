# Backend (`server`)

Spring Boot REST API for the capstone project. It pairs with the Next.js app in `../v-beta`.

## Prerequisites

- **JDK 17 or newer** (JDK 21 is fine).
- **MySQL 8** reachable when you run the application (not required for automated tests; those use an in-memory H2 database under the `test` profile).

You do **not** need a global Maven install if you use the wrapper scripts `mvnw` / `mvnw.cmd`.

## Configuration

- **`src/main/resources/application.properties`** — datasource and JPA (MySQL by default).
- **`src/main/resources/application.yml`** — application name, HTTP port (default **8080**), and CORS-related settings.

Database defaults assume:

- Host: `localhost`, port: `3306`, database: `V_Beta`
- User: `root`, password: set via `MYSQL_PASSWORD` (example: `devpassword` in `application.properties`)

Override with environment variables if needed: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`.

## Run the application

From this directory (`server/`):

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

**Windows**

```bash
mvnw.cmd spring-boot:run
```

The API listens on **http://localhost:8080** unless you change `server.port` in `application.yml`.

Quick checks:

- `GET http://localhost:8080/api/health` — liveness JSON (`status: ok`)
- `GET http://localhost:8080/api/v1/meta` — application metadata JSON

Main class: `edu.ics499.teamsatisfaction.VBetaApplication`.

## Run tests

Tests use the **`test`** Spring profile and **H2** (see `src/test/resources/application-test.yml`), so they do not need MySQL.

```bash
./mvnw test
```

On Windows:

```bash
mvnw.cmd test
```

To run tests and skip the long integration phase locally, you can still use `./mvnw test`; adjust Surefire or profiles later if the suite grows.

## Build a runnable JAR

```bash
./mvnw -DskipTests package
java -jar target/team-satisfaction-server-0.0.1-SNAPSHOT.jar
```

## Optional: MySQL with Docker

If you want a local database without a full MySQL install:

```bash
docker run -d --name team-satisfaction-mysql \
  -e MYSQL_ROOT_PASSWORD=devpassword \
  -e MYSQL_DATABASE=V_Beta \
  -p 3306:3306 \
  mysql:8
```

Then run the app with `MYSQL_PASSWORD=devpassword` (and `MYSQL_USER=root` if you use the defaults).
