# Build the React frontend and serve it from nginx. Build context is the repository root.
FROM node:20-bookworm AS build
WORKDIR /build

COPY react_frontend/package.json react_frontend/package-lock.json ./
RUN npm ci

COPY react_frontend/ ./

# Empty means same-origin: nginx proxies the API paths, so no absolute backend
# URL is baked into the bundle.
ENV REACT_APP_API_URL=""
ENV GENERATE_SOURCEMAP=false
RUN npm run build

# 1.27-alpine sat on Alpine 3.21.3 and carried 35 findings at HIGH or CRITICAL,
# almost all of them from the base layer rather than from anything here.
FROM nginx:1.31-alpine
COPY --from=build /build/build /usr/share/nginx/html
COPY deploy/nginx.conf           /etc/nginx/conf.d/default.conf
COPY deploy/carsale-headers.conf /etc/nginx/carsale-headers.conf
COPY deploy/carsale-proxy.conf   /etc/nginx/carsale-proxy.conf

# Drop root. This is only possible because nginx.conf listens on 8080 rather
# than 80: a non-root process cannot bind a privileged port, so a stock nginx
# image cannot simply have USER added to it. The cache directory and the pid
# file are the two paths nginx writes to, and both have to be handed over first.
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx \
 && touch /var/run/nginx.pid \
 && chown nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 8080

# Matches the compose health check. Declared here too so the image is
# self-describing wherever it runs. wget is busybox's, present in alpine.
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
