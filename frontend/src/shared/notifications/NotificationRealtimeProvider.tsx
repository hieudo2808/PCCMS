import { Client } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";
import { type ReactNode, useEffect } from "react";
import toast from "react-hot-toast";
import { useAuth } from "~/features/auth/context/AuthContext";
import { notificationKeys, type NotificationResponse } from "~/shared/api/notificationApi";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const websocketUrl = () => API_BASE_URL.replace(/^http/, "ws") + "/ws/notifications";

export function NotificationRealtimeProvider({ children }: { children: ReactNode }) {
    const queryClient = useQueryClient();
    const { isAuthenticated, user } = useAuth();
    const userId = user?.id;

    useEffect(() => {
        if (!isAuthenticated || !userId) return;

        const client = new Client({
            brokerURL: websocketUrl(),
            reconnectDelay: 5_000,
            heartbeatIncoming: 10_000,
            heartbeatOutgoing: 10_000,
            beforeConnect: async () => {
                const token = localStorage.getItem("token");
                client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
            },
            onConnect: () => {
                client.subscribe("/user/queue/notifications", (message) => {
                    try {
                        const notification = JSON.parse(message.body) as NotificationResponse;
                        if (!notification.id || !notification.title) return;
                        toast(notification.title);
                        void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
                    } catch {
                        // Ignore malformed frames and keep the realtime connection alive.
                    }
                });
            },
        });

        client.activate();
        return () => {
            void client.deactivate();
        };
    }, [isAuthenticated, queryClient, userId]);

    return <>{children}</>;
}
