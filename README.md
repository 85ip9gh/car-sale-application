# Car Sale Application

Car Sale Application is an authenticated peer-to-peer car marketplace built with React, Spring Boot, and MySQL. Users can create accounts, list and manage vehicles, purchase listings, and access role-specific administration workflows through a JWT-secured REST API.

> **Project status:** Live at [carsale.pesanth.com](https://carsale.pesanth.com), self-hosted behind an outbound-only Cloudflare Tunnel and verified end to end on 2026-08-09. Sign in as `demo` / `demo1234` to browse the market, list a vehicle, and buy from another seller. The earlier GCP and Azure deployments are offline. See [SECURITY.md](./SECURITY.md) for credential material that was committed to this repository in earlier history.

![Car Sale Application marketplace](./images/Website_V4_home.jpg)

[Live site](https://carsale.pesanth.com) · [Technical overview](https://pesanth.com/work/car-sale-application) · [Portfolio](https://pesanth.com)

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
docker-compose.yml      Local three-service development stack
deploy/                 Production stack for carsale.pesanth.com
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

- The live deployment was exercised end to end on 2026-08-09: account creation, JWT login, listing a vehicle, purchasing from another seller, security headers, and rate limiting all behaved as expected over HTTPS.
- **Buying a car does not move money.** `CarService.changeCarUser` reassigns ownership only. It never debits the buyer, credits the seller, or clears the `selling` flag, so a purchase is free and the vehicle stays on the market.
- The RSA signing keypair and a MySQL data directory were committed to this public repository in earlier history. Both were replaced and are trusted by nothing that runs, but they remain in Git history permanently. See [SECURITY.md](./SECURITY.md).
- There is no automated test coverage. Verification was done by exercising the running system.

## License

No open-source license has been added. The code is public for review, but reuse permission is not granted by default.
