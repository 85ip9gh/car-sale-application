# ShiftLane

ShiftLane is an authenticated peer-to-peer car marketplace built with React, Spring Boot, and MySQL. Users can create accounts, list and manage vehicles, purchase listings, and access role-specific administration workflows through a JWT-secured REST API.

> **Project status:** This is a historical portfolio project. Its former GCP and Azure deployments are offline, and the current runtime has not been reverified. The repository is presented as source and architecture evidence, not as a live production service.

![ShiftLane marketplace](./images/Website_V4_home.jpg)

[Technical overview](https://pesanth.com/work/shiftlane) · [Portfolio](https://pesanth.com)

## What this project demonstrates

- Account creation and BCrypt password hashing.
- JWT authentication through Spring Security and OAuth2 resource-server support.
- Role-based user, vehicle, inventory, and administration workflows.
- A Spring Boot REST API using JPA and MySQL persistence.
- A React interface for listings, profiles, purchases, and inventory management.
- A three-service frontend, API, and database deployment model.

## Architecture

```text
Browser
  |
  v
React client (port 3000)
  |
  | REST / JWT
  v
Spring Boot API (port 8080)
  |
  | JPA / JDBC
  v
MySQL (port 3306)
```

The React client calls a configurable API base URL. Spring Security authenticates requests and issues JWTs, the service layer applies marketplace rules, and JPA persists users, vehicles, listings, and transactions in MySQL.

## Repository layout

```text
react_frontend/          React application
java_backend_CRUD_1/    Spring Boot API
docker-compose.yml      Historical three-service deployment model
vm_docker_compose/      Historical cloud deployment material
images/                 Interface screenshots
```

## Run locally

Prerequisites: Java 20 or newer, Node.js, npm, MySQL, Maven, and OpenSSL.

1. Create a MySQL database and set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in your shell.
2. Generate an RSA key pair in `java_backend_CRUD_1/src/main/resources/certification` for JWT signing:

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

3. Start the API from `java_backend_CRUD_1`:

```bash
./mvnw spring-boot:run
```

4. Set `REACT_APP_API_URL=http://localhost:8080`, then start the client from `react_frontend`:

```bash
npm install
npm start
```

The client opens on `http://localhost:3000` and the API listens on `http://localhost:8080`.

## Verification and limitations

- The current technical review verified the architecture and source structure, not a live deployment.
- The Docker Compose files document the historical deployment approach and require a backend image or build artifact before use.
- The codebase requires a focused security review, test expansion, and dependency refresh before any public redeployment.

## License

No open-source license has been added. The code is public for review, but reuse permission is not granted by default.
