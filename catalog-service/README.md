# Catalog Service

This is the core microservice responsible for managing the book catalog. It connects to a PostgreSQL database and fetches its configuration dynamically from the Spring Cloud Config Server.

## 🛠️ Local Development Setup

### 1. Prerequisites

Before starting this service, ensure the following global infrastructure is running:

- **PostgreSQL:** Running via the root Docker Compose file.
- **Config Server:** Running on port `8071`.
-  **Eureka Server:** Running on port `8761`.

### 2. Local Secrets (`application-local.yml`)

Application secrets are excluded from version control. You must manually create the local properties file to connect to your database:

1. Create `src/main/resources/application-local.yml`.
2. Add your local database credentials:

```yaml
spring:
  datasource:
    password: your_local_password_here
```

### 3. Boot Variables & Startup (IDE Setup)

This service requires specific OS-level environment variables to boot correctly, which determine the active profiles and where to fetch configuration from Git.

Configure your Run/Debug Configuration in your IDE (e.g., IntelliJ IDEA) as follows:

1. Open **Edit Configurations** in the top right of your IDE.
2. Select the **CatalogServiceApplication** run configuration.
3. Locate the **Environment variables** field and paste the following:

```text
CONFIG_BRANCH=feature/your-current-branch-name;SPRING_PROFILES_ACTIVE=dev,local
```

> **Note:** Make sure to update the `CONFIG_BRANCH` value to match the Git branch you are actively working on in your configuration repository.

4. Click **Apply** and start the application using the **Debug (🐞)** or **Run (▶️)** button.