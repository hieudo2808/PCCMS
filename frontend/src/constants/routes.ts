export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',
  OWNER: {
    DASHBOARD: '/owner',
    PETS: '/owner/pets',
    BOOKING: '/owner/book',
    BOARDING_TRACKING: '/owner/boarding/tracking',
    PAYMENTS: '/owner/payments',
    PROFILE: '/owner/profile',
  },
  RECEPTION: {
    DASHBOARD: '/staff',
    APPOINTMENTS: '/staff/appointments',
    GROOMING_BOARD: '/staff/grooming-board',
    BOARDING_LOG: '/staff/boarding-log',
  },
  DOCTOR: {
    DASHBOARD: '/veterinarian',
    QUEUE: '/veterinarian/queue',
    MEDICAL_RECORD: '/veterinarian/medical-record',
  },
  ADMIN: {
    DASHBOARD: '/admin',
    ACCOUNTS: '/admin/accounts',
    CATALOG: '/admin/catalog',
    ROOMS: '/admin/rooms',
    SCHEDULE: '/admin/schedule',
    REPORTS: '/admin/reports',
  },
} as const;
