export const queryKeys = {
    users: {
        all: ["users"] as const,
        detail: (id: string) => ["users", id] as const,
        me: ["users", "me"] as const,
    },
    pets: {
        all: ["pets"] as const,
        detail: (id: string) => ["pets", id] as const,
    },
    medicalRecords: {
        detail: (id: string) => ["medicalRecords", id] as const,
    },
    medicines: {
        all: ["medicines"] as const,
        detail: (id: string) => ["medicines", id] as const,
    },
};
