# Build the React frontend and serve it from nginx. Build context is the repository root.
FROM node:20-bookworm AS build
WORKDIR /build

COPY react_frontend/package.json react_frontend/package-lock.json ./
RUN npm ci

COPY react_frontend/ ./

# Same-origin API: nginx proxies the API paths, so no absolute backend URL is baked in.
ENV REACT_APP_API_BASE_URL=""
ENV GENERATE_SOURCEMAP=false
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /build/build /usr/share/nginx/html
COPY deploy/nginx.conf           /etc/nginx/conf.d/default.conf
COPY deploy/carsale-headers.conf /etc/nginx/carsale-headers.conf
COPY deploy/carsale-proxy.conf   /etc/nginx/carsale-proxy.conf

EXPOSE 8080
