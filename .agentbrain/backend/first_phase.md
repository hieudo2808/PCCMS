# SYSTEM ARCHITECTURE & CODING GUIDELINES FOR PETCARE SYSTEM (PCCMS)

## 1. ARCHITECTURAL PATTERN

- **Design Principles:** SOLID, CQRS-inspired separation for administrative tasks.
- **Data Integrity:** Strict implementation of Soft-Delete for core entities. Code must never use hard `DELETE` SQL commands for master records.

---

## 2. SUBSYSTEMS & CLASS SPECIFICATIONS

### PHASE 1: IDENTITY & ACCESS SUBSYSTEM (Authentication & Profiles)

#### Entities & Data Models

1. **Account**
   - `accountId`: String (UUID, Primary Key)
   - `mail`: String (Unique, Email format)
   - `number`: String (Unique, Phone format)
   - `passwordHash`: String (Bcrypt encrypted)
   - `status`: Integer/Enum (1: ACTIVE, 0: INACTIVE, -1: LOCKED)
   - `createdAt`: DateTime
   - `updatedAt`: DateTime

2. **UserProfile**
   - `profileId`: String (UUID, Primary Key)
   - `accountId`: String (Foreign Key -> Account)
   - `fullName`: String (Required)
   - `address`: String
   - `avatarUrl`: String (S3 or Cloudinary URL)

3. **JWTToken**
   - `tokenId`: String (UUID, Primary Key)
   - `hashToken`: String (JWT String)
   - `expiresAt`: DateTime
   - `isRevoked`: Boolean (True if logged out)

4. **OTPToken**
   - `otpId`: String (UUID, Primary Key)
   - `otpCode`: String (6 digits)
   - `expiresAt`: DateTime (TTL: 3-5 minutes)
   - `isUsed`: Boolean

#### Services & Controllers

- **ProfileController:** Exposes endpoints for authentication and profile management (`/api/v1/auth`, `/api/v1/profiles`). Injects `AuthService`.
- **AuthService:** Orchestrates the authentication lifecycle. Injects `TokenService`, `OTPService`, `PasswordService`, and `AccountRepository`.
- **TokenService:** Handles generation, validation, and revocation of `JWTToken`.
- **OTPService:** Handles generation, verification of `OTPToken`, and triggers notifications.
- **PasswordService:** Handles password hashing and verification using secure algorithms.

---

### PHASE 2: ACCOUNT ADMINISTRATION SUBSYSTEM (Management)

#### Services & Dependencies

- **AdminAccountService:** Specialized service executing high-privilege administrative tasks.
  - _Dependencies:_ `IAccountRepository` (for global account access), `IPasswordService` (for employee initialization), `IAuditLogger` (for tracking changes).
  - _Methods:_ `lockAccount(id)`, `unlockAccount(id)`, `createNewEmployeeAccount(data)`.

---

### PHASE 3: MEDICAL CARE SUBSYSTEM (Clinical & Treatment)

#### Entities & Data Models

1. **MedicalRecord**
   - `recordId`: String (UUID, Primary Key)
   - `appointmentId`: String (Foreign Key)
   - `petId`: String (Foreign Key)
   - `ownerId`: String (Foreign Key)
   - `vitalSigns`: JSON/String (Must store: Temperature, Heart Rate, Respiratory Rate, Blood Pressure, SpO2, Mucous Membrane Color, CRT)
   - `diagnosisData`: String (Text description of diagnosis)
   - `isEditable`: Boolean (Defaults to True; set to False once bill is settled)

2. **Prescription**
   - `prescriptionId`: String (UUID, Primary Key)
   - `recordId`: String (Foreign Key -> MedicalRecord)
   - `prescriptionData`: JSON/String (List of structures: medicineId, dosage, frequency, durationDays, totalQuantity)
   - `createdAt`: DateTime

3. **Medicine**
   - `medicineId`: String (UUID, Primary Key)
   - `name`: String (Unique)
   - `unit`: String (e.g., Tablet, Vial)
   - `price`: Decimal/Float
   - `stockQuantity`: Integer
   - `status`: Boolean (True: Active, False: Discontinued)

4. **TestResult**
   - `resultId`: String (UUID, Primary Key)
   - `recordId`: String (Foreign Key -> MedicalRecord)
   - `fileData`: String (URL path to uploaded X-Ray/Ultrasound images)
   - `uploadedAt`: DateTime

#### Services & Business Logic

- **MedicalRecordService:** Manages clinical entries. Injects `IMedicalRecordRepository`, `ITestResultRepository`.
- **PrescriptionService:** Manages medication workflows. Injects `IPrescriptionRepository`, `IMedicineRepository`.

---

## 3. CORE BUSINESS RULES & CONSTRAINT LOGIC (GUARDRAILS)

When writing logic or creating service methods, Agent must enforce the following rules:

1. **Rule: Security Verification**
   - Any access to `UserProfile` or `MedicalRecord` by a Customer role must enforce `Owner_ID == Current_User_ID`.

2. **Rule: Data Locking (Data Integrity)**
   - Inside `MedicalRecordService.update()`, check the `isEditable` flag. If `isEditable == False` (meaning invoice paid), reject any updates to `vitalSigns` or `diagnosisData` with a `403 Forbidden` error.

3. **Rule: Inventory Stock Check**
   - Inside `PrescriptionService.create()`, calculate `totalQuantity = (dosage_per_intake * frequency_per_day) * duration_days`.
   - Cross-check with `Medicine.stockQuantity`. If `totalQuantity > stockQuantity`, abort transaction and return `400 Bad Request: Insufficient Stock`.

4. **Rule: Soft-Delete Rule**
   - `Medicine` entries must never be deleted via a `DELETE` query. Implement a `status = False` switch. Filter queries to only retrieve active listings for active prescriptions.

5. **Rule: Automated Notifications**
   - When any state machine changes (e.g., an OTP is requested or a service moves to completion), trigger the notification interfaces asynchronously without blocking the primary HTTP thread.
