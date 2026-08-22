# Local development image, built by the root docker-compose.yml. The deployed
# frontend is deploy/web.Dockerfile, which serves a production build from nginx
# and is the one that matters for anything public.
#
# node:18 reached end of life in April 2025. Left alone here on purpose: moving
# it is a build change with real risk to the local stack, and it belongs in its
# own commit rather than inside a security-gates change.
FROM node:18-alpine

WORKDIR /app

# /app is created by WORKDIR as root, so hand it over before dropping
# privileges. CRA's dev server writes to node_modules/.cache at runtime, which
# is why the whole directory has to be writable and not just the source.
RUN chown node:node /app

USER node

COPY --chown=node:node package.json .

RUN npm install

COPY --chown=node:node . .

RUN npm run build

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:3000/ || exit 1

CMD ["npm", "start"]
