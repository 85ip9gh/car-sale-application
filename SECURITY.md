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

### 2. The MySQL data directory

`.db/` was tracked, 179 files of a live MySQL 8 data directory. It contained
`user_details.ibd` (application accounts and their BCrypt password hashes) and
MySQL's own generated server TLS keys.

**Status: not reused.** The hosted deployment starts from an empty database
rather than mounting that snapshot, so no account or hash from it exists in the
running system. `.db/` is now gitignored.

Anyone who reused a password from that old database elsewhere should change it
there. The hashes are BCrypt, so they are not trivially reversible, but they
are public and offline-crackable.

## Reporting

Open an issue, or contact the repository owner directly.
