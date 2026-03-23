import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
    // 환경변수 로드
    const env = loadEnv(mode, process.cwd(), '');

    return {
        plugins: [react()],
        resolve: {
            alias: {
                "@": path.resolve(__dirname, "./src"),
            },
        },
        server: {
            port: 3000,
            proxy: {
                // OCR API 프록시 설정
                "/ocr": {
                    target: env.VITE_PROXY_TARGET || "http://localhost:8080",
                    changeOrigin: true,
                    secure: false,
                },
                // 일반 API 프록시 설정
                "/api": {
                    target: env.VITE_PROXY_TARGET || "http://localhost:8080",
                    changeOrigin: true,
                    secure: false,
                },
            },
        },
    };
});
