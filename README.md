# Spring Boot Chat Backend

## Local Configurations and Steps for Running This App

This project is a Spring Boot backend for a chat application. The instructions below cover how to build and run the application on Windows (PowerShell), configure required credentials, and common troubleshooting steps.

Prerequisites
- Java 17 (JDK 17)
- Maven or the included wrapper (`mvnw.cmd`)
- PowerShell (Windows)

Environment & configuration
- The app loads configuration from `src/main/resources/application.yml`.
- Important properties you may need to override in production or on your machine:
  - OAuth2 client credentials (Google/GitHub) under `spring.security.oauth2.client.registration.*`.
  - Firebase service account JSON: by default `classpath:chat-app-service-acc-key.json`. You can replace the file or update `firebase.credentials.path`.
  - JWT secret and expiration in `spring.app.jwtSecret` and `spring.app.jwtExpirationMs`.
  - AWS S3 credentials under `aws.s3.*` if file upload features are used.

You can set environment variables instead of changing `application.yml`. Example (PowerShell):

```powershell
$env:SPRING_APPLICATION_JSON = '{"spring": {"app": {"jwtSecret":"your-secret"}}}'
```

Build
- From repository root (uses included wrapper):

```powershell
# build without running tests
.\mvnw.cmd -DskipTests package
```

- Or with installed Maven:

```powershell
mvn -DskipTests package
```

Run (local)
- Run with Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

- Run the packaged jar (after `package`):

```powershell
java -jar target\chat-app-0.0.1-SNAPSHOT.jar
```

- Run from IDE: launch the `tbh.chat_app.ChatAppApplication` main class.

Setting OAuth2 client credentials
- It's recommended to provide real client IDs/secrets using environment variables or a secure secrets manager. Example environment variables you could export in PowerShell before running the app:

```powershell
$env:OAUTH_GOOGLE_CLIENT_ID = 'your-google-client-id'
$env:OAUTH_GOOGLE_CLIENT_SECRET = 'your-google-client-secret'
$env:OAUTH_GITHUB_CLIENT_ID = 'your-github-client-id'
$env:OAUTH_GITHUB_CLIENT_SECRET = 'your-github-client-secret'
```

Then map or read them in `application.yml` (or via `SPRING_APPLICATION_JSON`) as needed.

Testing API endpoints
- The API runs by default on `http://localhost:8080`.
- Example: call a protected endpoint with JWT:

```powershell
curl -H "Authorization: Bearer <JWT>" http://localhost:8080/api/users
```

Expired JWT handling
- When a request contains an expired JWT the server responds with `401 Unauthorized` and a JSON body like:

```json
{"error": "JWT token expired"}
```

Troubleshooting
- Port already in use: change `server.port` in `application.yml` or stop the process occupying the port.
- Firebase credentials not found: make sure `chat-app-service-acc-key.json` is in `src/main/resources` or update `firebase.credentials.path` to point to your file or a classpath resource.
- OAuth login problems: verify redirect URIs in provider console (Google/GitHub) match `http://localhost:8080/login/oauth2/code/{registrationId}` and client id/secret are correct.
- JWT secret missing or invalid: set `spring.app.jwtSecret` to a secure value.
- Build or runtime Java version errors: ensure `java -version` shows Java 17.

---