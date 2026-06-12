import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

// https://vite.dev/config/
export default defineConfig({
    plugins: [react(), tailwindcss()],
    resolve: {
        alias: {
            "~": path.resolve(__dirname, "./src"),
            "@tests": path.resolve(__dirname, "./tests"),
        },
    },
    test: {
        globals: true,
        environment: "jsdom",
        setupFiles: "./tests/setupTests.ts",
    },
});
