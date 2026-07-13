FROM node:22-alpine AS build
WORKDIR /app

RUN corepack enable
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
# El proxy de Nginx publica la API bajo el mismo origen que el frontend.
ARG VITE_MAPTILER_API_KEY
ARG VITE_MAP_STYLE_LIGHT_URL=https://api.maptiler.com/maps/streets-v2/style.json
ARG VITE_MAP_STYLE_DARK_URL=https://api.maptiler.com/maps/streets-v2-dark/style.json
ENV VITE_API_BASE_URL=/api
ENV VITE_MAPTILER_API_KEY=$VITE_MAPTILER_API_KEY
ENV VITE_MAP_STYLE_LIGHT_URL=$VITE_MAP_STYLE_LIGHT_URL
ENV VITE_MAP_STYLE_DARK_URL=$VITE_MAP_STYLE_DARK_URL
RUN pnpm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
