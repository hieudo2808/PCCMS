import { useQueryClient } from "@tanstack/react-query";
import { type ReactNode, useEffect } from "react";
import toast from "react-hot-toast";
import type { NotificationResponse } from "~/shared/api/notificationApi";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function websocketUrl() {
    return API_BASE_URL.replace(/^http/, "ws") + "/ws/notifications";
}

function stompFrame(command: string, headers: Record<string, string> = {}, body = "") {
    const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
    return `${command}\n${headerLines.join("\n")}\n\n${body}\0`;
}

function parseStompMessages(data: string) {
    return data
        .split("\0")
        .map((frame) => frame.trim())
        .filter((frame) => frame.startsWith("MESSAGE"))
        .map((frame) => frame.slice(frame.indexOf("\n\n") + 2));
}

export function NotificationRealtimeProvider({ children }: { children: ReactNode }) {
    const queryClient = useQueryClient();

    useEffect(() => {
        const token = localStorage.getItem("token");
        if (!token) return;

        let socket: WebSocket | null = null;
        let reconnectTimer: number | undefined;
        let closedByEffect = false;

        const connect = () => {
            socket = new WebSocket(websocketUrl());

            socket.addEventListener("open", () => {
                socket?.send(stompFrame("CONNECT", {
                    Authorization: `Bearer ${token}`,
                    "accept-version": "1.2",
                    host: window.location.host,
                }));
            });

            socket.addEventListener("message", (event) => {
                const text = String(event.data);
                if (text.startsWith("CONNECTED")) {
                    socket?.send(stompFrame("SUBSCRIBE", {
                        id: "notifications",
                        destination: "/user/queue/notifications",
                        ack: "auto",
                    }));
                    return;
                }

                for (const body of parseStompMessages(text)) {
                    const notification = JSON.parse(body) as NotificationResponse;
                    toast(notification.title);
                    queryClient.invalidateQueries({ queryKey: ["notifications"] });
                    queryClient.invalidateQueries({ queryKey: ["owner-notifications"] });
                }
            });

            socket.addEventListener("close", () => {
                if (!closedByEffect) {
                    reconnectTimer = window.setTimeout(connect, 5000);
                }
            });
        };

        connect();

        return () => {
            closedByEffect = true;
            if (reconnectTimer) window.clearTimeout(reconnectTimer);
            socket?.close();
        };
    }, [queryClient]);

    return <>{children}</>;
}
