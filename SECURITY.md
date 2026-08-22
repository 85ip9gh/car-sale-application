# Security

## Credential material previously committed to this repository

Two things were tracked in this public repository and are therefore permanent
in its Git history. Both were removed from the working tree in the deployment
change, but **removal from the tip does not remove them from history**, and
anyone who cloned the repository before that point still holds them.

### 1. The JWT signing keypair

`java_backend_CRUD_1/src/main/resources/certification/private.pem` and
`keypair.pem` held the RSA private key used to sign authentication tokens.
Anyone with that key could mint a token asserting any username and any role,
including `ROLE_ADMIN`, without a password.

**Status: replaced.** A new keypair was generated, the old one was discarded and
is used nowhere, and the hosted deployment loads its keys from a host path
mounted at runtime rather than from the classpath. The old key is now worthless
because no running service trusts it.

### 2. Two MySQL data directories

`.db/` was tracked, 179 files of a live MySQL 8 data directory. It contained
`user_details.ibd` (application accounts and their BCrypt password hashes) and
MySQL's own generated server TLS keys.

A **second** copy was tracked at `vm_docker_compose/.db/`, 216 more files
including its own `pesanth/user_details.ibd`. It was missed when the first was
removed and was caught afterwards by the CI check added for exactly this, which
is the argument for the check existing rather than relying on someone
remembering.

**Status: not reused.** The hosted deployment starts from an empty database
rather than mounting that snapshot, so no account or hash from it exists in the
running system. Both are now gitignored.

Anyone who reused a password from that old database elsewhere should change it
there. The hashes are BCrypt, so they are not trivially reversible, but they
are public and offline-crackable.

### 3. Three mkcert development TLS keys

Added 2026-08-22. **This section exists because the list above was incomplete.**
It was written by hand and it missed three private keys. A history-wide secret
scan added to CI on the same day found them, which is the argument for the scan
rather than for a careful author.

`react_frontend/ssl/localhost-key.pem`, `react_frontend/ssl-gcp/34.148.248.82-key.pem`
and `react_frontend/install-key.pem` were committed on 2023-12-18 during a
"HTTPS for localhost" experiment and removed on 2024-01-07. They are permanent
in history like everything else here.

**Status: expired and unusable.** Two independent reasons:

1. All three certificates **expired on 2026-03-18**.
2. The mkcert **certificate authority private key was never committed**. These
   are leaf keys, verified with `openssl x509 -noout -ext basicConstraints`,
   which reports no extensions and therefore no `CA:TRUE`. A leaf key signs
   nothing on behalf of anyone else, so nobody can use these to issue a
   certificate that a machine would trust.

One covered `localhost`, which authenticates nothing remote. One covered the GCP
host at `34.148.248.82`, which is retired and is on the banned-strings list in
the CI bundle check. The third was the leaf pair from the same experiment.

## How this is enforced now

Two separate checks, answering two different questions.

- `ci.yml` asserts that no key or database file is **tracked**. That is a
  tip-of-tree question, and it is the check that caught the second MySQL data
  directory when a human had missed it.
- `security.yml` runs gitleaks over the **full history**, along with dependency,
  container and infrastructure scanning. Everything above is allowlisted in
  `.gitleaksignore` with a dated reason for each entry, so the record is a
  documented judgement rather than silence.

The history is not rewritten, deliberately. It was rewritten on the sibling
`cube-store-application` repository for an exposed Stripe key, and measured
afterwards: GitHub pins every pull request's head commit under `refs/pull/*`,
which no force-push reaches, so the material stayed fetchable and only GitHub
Support can purge it. **Neutralising the credential is the remediation. A
rewrite is hygiene.** Everything listed here is already neutralised.

## Reporting

Open an issue, or contact the repository owner directly.
