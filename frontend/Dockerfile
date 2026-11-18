# ---- Giai đoạn 1: build bundle React ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Gọi API qua chính origin của trang web; nginx sẽ chuyển tiếp /api và /ws tới backend.
ENV VITE_API_BASE_URL=/api
RUN npm run build

# ---- Giai đoạn 2: phục vụ tệp tĩnh bằng nginx ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
