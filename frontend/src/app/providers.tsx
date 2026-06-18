import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "react-hot-toast";
import type { ReactNode } from "react";
import { AuthProvider } from "../features/auth/context/AuthContext";
import { NotificationRealtimeProvider } from "../shared/notifications/NotificationRealtimeProvider";

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
});

interface ProvidersProps {
    children: ReactNode;
}

export function Providers({ children }: ProvidersProps) {
    return (
        <QueryClientProvider client={queryClient}>
            <AuthProvider>
                <NotificationRealtimeProvider>{children}</NotificationRealtimeProvider>
            </AuthProvider>
            <Toaster position="top-right" />
        </QueryClientProvider>
    );
}
