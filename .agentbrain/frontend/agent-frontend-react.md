---
trigger: always_on
---

# FRONTEND ARCHITECTURE & GUIDELINES (REACT + VITE)

## 1. Tech Stack

- **Core:** React, Vite.
- **Data Fetching:** Axios. (Use custom Axios instance with Interceptors for JWT token attachment).
- **Routing:** React Router DOM.
- **Styling:** [Điền thư viện UI team bạn dùng, VD: Tailwind CSS / Ant Design / MUI].

## 2. Folder Structure (Feature-Sliced Design)

Agent must organize code by features, NOT by file types.

- `src/features/account-admin`: Contains Pet list, Pet creation forms.
- `src/features/medical-care`: Contains Medical Record forms, Vital Signs inputs, Prescription dynamic tables.
- `src/shared/components`: Reusable UI (Buttons, DataTables, Modals).

## 3. Strict UI Rules for Hiếu's Domains

1. **Medical Record Form (UI Lock):** - If `record.statusCode === 'FINALIZED'`, Agent MUST render all input fields as `disabled={true}` and hide all "Save/Submit" buttons.
2. **Vital Signs Client-Side Validation:** - Add real-time validation for `spo2_percent` (must be 0-100). Show red helper text immediately if invalid.
3. **Prescription Dynamic Form:** - Agent must build a form allowing users to dynamically "Add/Remove Medicine Rows".
   - Auto-calculate `totalQuantity` on the frontend (`dosage` _ `frequency` _ `durationDays`) before submitting to the backend.
