# Security gates: baseline

What the three gates found on their first run, before anything was fixed, and
what was done about each finding.

Captured 2026-08-22 from [run 32548494884](https://github.com/85ip9gh/car-sale-application/actions/runs/32548494884)
on branch `security/supply-chain-gates`. Reports are attached to that run as
artifacts. This file is the permanent record, because artifacts expire.

All three gates failed. That was the expected outcome and the reason the
baseline was taken before any fix.

## The before state

| Gate | Findings |
|---|---|
| secrets, gitleaks over full history | 5 private keys |
| deps, trivy `fs` | 80: **78** in `react_frontend/package-lock.json` (5 CRITICAL), 2 in `pom.xml` |
| deps, trivy `image`, API | 43 (6 CRITICAL) |
| deps, trivy `image`, web | 35 (2 CRITICAL) |
| iac, checkov | 5 failed of 171; workflows clean at 136 of 136 |

## secrets: the list in SECURITY.md was incomplete

Five private keys. `SECURITY.md` documented two.

**The three it missed** are mkcert development TLS keys, committed 2023-12-18
during a "HTTPS for localhost" experiment and removed 2024-01-07:
`react_frontend/ssl/localhost-key.pem`,
`react_frontend/ssl-gcp/34.148.248.82-key.pem`, and
`react_frontend/install-key.pem`.

**This is the argument for the gate rather than for a careful author.** That
list was written by hand, it was wrong, and nobody noticed until something read
the whole history.

They are worthless for two independent reasons, both checked and not assumed:

1. All three certificates **expired on 2026-03-18**.
2. The mkcert **CA private key was never committed**. These are leaf keys, and a
   leaf key issues nothing. `openssl x509 -noout -ext basicConstraints` reports
   no extensions on `install.pem`, so there is no `CA:TRUE`.

One covered `localhost`, which authenticates nothing remote. One covered the
retired GCP host at `34.148.248.82`, an address already on the banned-strings
list in `ci.yml`'s bundle check.

All five are allowlisted in `.gitleaksignore` with a dated reason each, and
`SECURITY.md` now carries the section it was missing.

**The history is deliberately not rewritten.** It was rewritten on the sibling
`cube-store-application` repository and measured afterwards: GitHub pins every
pull request's head commit under `refs/pull/*`, which no force-push reaches, so
102 matches stayed fetchable and only GitHub Support can purge them.
Neutralising the credential is the remediation. A rewrite is hygiene, and here
the material is already neutralised.

## deps, filesystem: 80 down to zero, mostly by fixing a lie

**62 of the 78 npm findings were not production dependencies at all.**
`react-scripts` and the three `@testing-library` packages sat in `dependencies`
with `devDependencies` empty. That is what `create-react-app` generates and it
is simply wrong: they are build and test tooling, and none of them reaches a
browser. Trivy counted the entire `react-scripts` transitive tree as production
because the manifest told it to.

Moving them is a **correctness fix, not a way to quiet the scanner**. The
packages are still installed and the build still uses them. `web-vitals` stayed
in `dependencies`, because `reportWebVitals.js` really does import it at runtime.

The remaining 16 were two packages that genuinely ship:

- `axios` 1.5.1 to 1.19.0, which also carries `form-data` past its own advisory
- `react-router-dom` 6.16.0 to 6.30.6, carrying `@remix-run/router` past 1.23.2

Both stay inside their major version and both were already covered by the
existing caret ranges.

## deps, API image: 43 down to zero, from one line

Almost every finding traced to `spring-boot-starter-parent` being **3.1.3**,
which pins Tomcat 10.1.12, Spring Framework 6.0.11, Spring Security 6.1.3,
snakeyaml 1.33, jackson 2.15.2, logback 1.4.11 and mysql-connector-j 8.0.33.
`tomcat-embed-core` alone accounted for 17 of the 43.

Raised to **3.5.16**, the newest 3.x. Deliberately not 4.x: staying inside the
current major keeps this a dependency bump rather than a migration.

## deps, web image: 35 down to zero, from the base tag

`nginx:1.27-alpine` sat on Alpine 3.21.3 and carried all 35, almost none of them
from anything in this repository. `nginx:1.31-alpine` clears them.

## iac: 5 fixed

- `deploy/api.Dockerfile` declares its `HEALTHCHECK`. It always had one, but it
  lived in `compose.yaml` where a scanner reading the image cannot see it.
- `deploy/web.Dockerfile` drops to `USER nginx`. **That is only possible because
  `nginx.conf` listens on 8080 rather than 80**: a non-root process cannot bind
  a privileged port, so a stock nginx image cannot simply have `USER` added.
  The cache directory and the pid file are handed over first.
- `react_frontend/Dockerfile` drops to `USER node` and gains a healthcheck.

## The part worth keeping: two smoke tests

A dependency scan says the artifact contains no known-vulnerable library. **It
says nothing about whether the thing still runs**, and both of the changes above
are exactly the kind that fail at start rather than at build. So the pipeline now
proves it:

**The web image** is started and asked for `/healthz`. This caught a real
failure on its first run: nginx exited with `host not found in upstream
"api:8080"`, because it resolves every upstream at config-parse time and no
`api` container exists when the image runs alone. The test now supplies the name
with `--add-host`. **A build would have passed that image happily.**

**The API image** is started against a real `mysql:8.0` on a throwaway network,
with a throwaway keypair, and polled until `/actuator/health` reports `UP`. The
unit suite cannot answer this, which is why `ci.yml` skips it: it needs a live
database. Without this step the Spring Boot jump from 3.1.3 to 3.5.16 would rest
on nothing but the fact that it compiled.

Both report what they proved rather than just passing: `API reports UP on Spring
Boot 3.5.16`, and `web image serves /healthz as nginx`.

## Two things carried from cube-store, so they were not rediscovered

**Checkov has no `docker_compose` framework**, verified against 3.3.13's own
valid-framework list. The three compose files are not covered and stay a manual
review. `github_actions` took that slot and passes 136 of 136.

**`CKV_IGNORE_HIDDEN_DIRECTORIES=false` is set.** Checkov skips dot-directories
by default. On cube-store a Dockerfile with no `USER` and no `HEALTHCHECK`
passed cleanly inside one and failed the moment it moved to a visible path, so
the gate read green while not looking.

## Still open

- `node:18-alpine` in `react_frontend/Dockerfile` has been end of life since
  April 2025. Left alone deliberately: moving it is a build change with real
  risk to the local stack and belongs in its own commit.
- The three compose files are outside the IaC gate and reviewed by hand.
- The Spring Boot 4.x line exists. Nothing here needs it, and the jump is a
  migration rather than a bump.
