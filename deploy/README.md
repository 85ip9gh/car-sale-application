# Deployment

Production stack for `carsale.pesanth.com`, running on the `host-b` host.

## Shape

Three containers on one internal bridge network:

| Container | Role | Published |
|---|---|---|
| `web` | nginx serving the compiled React bundle, reverse-proxying API paths | `${BIND_ADDRESS}:${BIND_PORT}` only |
| `api` | Spring Boot REST API | no |
| `db` | MySQL 8 | no |

Public ingress is an outbound-only Cloudflare Tunnel. No router port is
forwarded. `web` binds to the host's Tailscale address, so even on the LAN the
only route in is the tunnel.

The frontend and API share one origin, so the browser never makes a
cross-origin call and no backend hostname is compiled into the bundle.

## First deploy

```sh
cd ~/car-sale/deploy
cp app.env.example app.env    # fill in real values
chmod 600 app.env

mkdir -p keys && chmod 700 keys
openssl genrsa -out keys/keypair.pem 2048
openssl rsa -in keys/keypair.pem -pubout -out keys/public.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in keys/keypair.pem -out keys/private.pem
chmod 600 keys/*.pem

docker compose --env-file app.env up -d --build
```

`app.env` and `keys/` are gitignored and must stay on the host only.

## Passwords

Use alphanumeric values in `app.env`. Compose interpolates that file, so a
literal `$` is read as a variable reference and silently corrupts the value.

## Schema and seed

Hibernate creates the schema on first boot (`ddl-auto=update`). Spring's
`data.sql` seeding is off (`spring.sql.init.mode=never`); seed data is applied
directly to MySQL after the schema exists, so the seed matches the real
generated columns rather than a guess at them.

## Granting yourself admin

No admin account is seeded, because the admin role can delete other users and
this is a public demo. Create a normal account through the site, then promote
it:

```sh
docker compose --env-file app.env exec db \
  mysql -u root -p"$MYSQL_ROOT_PASSWORD" pesanth \
  -e "UPDATE user_details SET roles='ROLE_ADMIN' WHERE name='<your-username>';"
```

## Security notes

- The JWT signing keypair is generated on the host and mounted read-only at
  `/run/keys`. It is never baked into an image or committed. See `SECURITY.md`
  in the repository root for why the previous keypair had to be replaced.
- CORS is an exact origin allow list from `APP_CORS_ALLOWED_ORIGINS`, not `*`.
- nginx rate limits authentication endpoints (`/jwt-token`, `/basic-auth`,
  `/addUser`) hard, caps request bodies at 16k, and sets the security headers.
- `api` and `web` run read-only with all capabilities dropped and
  `no-new-privileges`. `api` runs as an unprivileged user.
- Nothing under `/actuator` is proxied from the edge; it backs the container
  health check only.
