# Build stage - React 빌드
FROM node:20-alpine AS build

WORKDIR /app

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ .
RUN npm run build

# Production stage - Nginx (설정과 정적파일은 볼륨 마운트)
FROM nginx:alpine

# 빌드 결과물을 임시로 복사 (Jenkins에서 호스트로 복사용)
COPY --from=build /app/dist /tmp/dist

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]