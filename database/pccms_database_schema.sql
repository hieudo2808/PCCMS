CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- =========================================================
-- 0. NATIVE ENUMS
-- =========================================================
CREATE TYPE user_status_enum AS ENUM ('UNVERIFIED', 'ACTIVE', 'LOCKED', 'DISABLED');
CREATE TYPE service_category_enum AS ENUM ('MEDICAL', 'GROOMING', 'BOARDING', 'OTHER');
CREATE TYPE service_order_status_enum AS ENUM ('REQUESTED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
CREATE TYPE appointment_status_enum AS ENUM ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
CREATE TYPE grooming_status_enum AS ENUM ('PENDING', 'CONFIRMED', 'IN_SERVICE', 'COMPLETED', 'CANCELLED');
CREATE TYPE boarding_status_enum AS ENUM ('RESERVED', 'CHECKED_IN', 'IN_STAY', 'CHECKED_OUT', 'CANCELLED');
CREATE TYPE room_status_enum AS ENUM ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'INACTIVE');
CREATE TYPE care_period_enum AS ENUM ('MORNING', 'NOON', 'AFTERNOON');
CREATE TYPE schedule_status_enum AS ENUM ('ASSIGNED', 'CANCELLED', 'COMPLETED');
CREATE TYPE shift_request_status_enum AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED');
CREATE TYPE invoice_status_enum AS ENUM ('DRAFT', 'UNPAID', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED', 'REFUNDED');
CREATE TYPE payment_method_enum AS ENUM ('CASH', 'BANK_TRANSFER', 'CARD', 'E_WALLET');
CREATE TYPE payment_status_enum AS ENUM ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'REFUNDED');
CREATE TYPE notification_status_enum AS ENUM ('UNREAD', 'READ', 'ARCHIVED');
CREATE TYPE file_visibility_enum AS ENUM ('PRIVATE', 'OWNER_VISIBLE', 'STAFF_ONLY');

-- =========================================================
-- 1. IDENTITY, ACCESS, ACCOUNT ADMINISTRATION
-- =========================================================
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(60) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles (code, name) VALUES
('OWNER','Chủ nuôi'),
('STAFF','Nhân viên trung tâm'),
('VETERINARIAN','Bác sĩ thú y'),
('ADMIN','Quản trị viên hệ thống')
ON CONFLICT DO NOTHING;

CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email            VARCHAR(255) NOT NULL UNIQUE,
    phone            VARCHAR(30) UNIQUE,
    password_hash    TEXT NOT NULL,
    full_name        VARCHAR(150) NOT NULL,
    role_id          UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    status_code      user_status_enum NOT NULL DEFAULT 'UNVERIFIED',
    email_verified_at TIMESTAMPTZ,
    phone_verified_at TIMESTAMPTZ,
    last_login_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(120) NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- =========================================================
-- SEED DATA: RBAC PERMISSIONS & ROLE MAPPINGS
-- =========================================================

-- 1. Seed Permissions
INSERT INTO permissions (code, name, description) VALUES
-- Pet Module
('PET_CREATE', 'Thêm thú cưng', 'Cho phép thêm hồ sơ thú cưng mới'),
('PET_READ', 'Xem thú cưng', 'Cho phép xem thông tin thú cưng'),
('PET_UPDATE', 'Cập nhật thú cưng', 'Cho phép chỉnh sửa hồ sơ thú cưng'),
('PET_DELETE', 'Xóa thú cưng', 'Cho phép xóa hoặc ẩn hồ sơ thú cưng'),
('OWNER_PROFILE_UPDATE', 'Cập nhật thông tin cá nhân', 'Cho phép chủ nuôi cập nhật thông tin'),
-- Medical Module
('APPOINTMENT_CREATE', 'Đặt lịch khám', 'Cho phép đặt lịch khám bệnh'),
('APPOINTMENT_READ', 'Xem lịch khám', 'Cho phép xem danh sách lịch khám'),
('APPOINTMENT_RECEIVE', 'Tiếp nhận hẹn khám', 'Cho phép nhân viên tiếp nhận lịch hẹn khám'),
('MEDICAL_RECORD_UPDATE', 'Cập nhật HS khám bệnh', 'Cho phép BS cập nhật hồ sơ bệnh án'),
('PRESCRIPTION_CREATE', 'Kê đơn thuốc', 'Cho phép BS kê đơn thuốc'),
-- Service & Boarding Module
('GROOMING_CREATE', 'Đăng ký làm đẹp', 'Cho phép đăng ký dịch vụ làm đẹp'),
('GROOMING_READ', 'Xem dịch vụ làm đẹp', 'Cho phép xem lịch sử làm đẹp'),
('GROOMING_UPDATE', 'Cập nhật làm đẹp', 'Cho phép cập nhật trạng thái làm đẹp'),
('BOARDING_CREATE', 'Đặt phòng lưu trú', 'Cho phép đặt phòng lưu trú'),
('BOARDING_READ', 'Xem lưu trú', 'Cho phép xem lịch sử lưu trú'),
('BOARDING_UPDATE', 'Cập nhật lưu trú', 'Cho phép cập nhật trạng thái lưu trú'),
-- Billing Module
('INVOICE_READ', 'Xem hóa đơn', 'Cho phép xem lịch sử thanh toán'),
('INVOICE_MANAGE', 'Quản lý hóa đơn', 'Cho phép tạo và xử lý hóa đơn'),
-- Admin Module
('ACCOUNT_MANAGE', 'Quản lý tài khoản', 'Cho phép quản lý tài khoản người dùng'),
('MEDICINE_MANAGE', 'Quản lý thuốc', 'Cho phép thêm, sửa, xóa danh mục thuốc'),
('SERVICE_MANAGE', 'Quản lý dịch vụ', 'Cho phép quản lý danh mục dịch vụ'),
('ROOM_MANAGE', 'Quản lý phòng', 'Cho phép quản lý phòng lưu trú'),
('SCHEDULE_MANAGE', 'Quản lý lịch làm việc', 'Cho phép quản lý lịch làm việc nhân sự'),
('REPORT_VIEW', 'Xem báo cáo', 'Cho phép xem báo cáo và thống kê')
ON CONFLICT DO NOTHING;

-- 2. Map Permissions to Roles

-- Map cho OWNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'OWNER' AND p.code IN (
    'PET_CREATE', 'PET_READ', 'PET_UPDATE', 'PET_DELETE', 'OWNER_PROFILE_UPDATE',
    'APPOINTMENT_CREATE', 'APPOINTMENT_READ',
    'GROOMING_CREATE', 'GROOMING_READ',
    'BOARDING_CREATE', 'BOARDING_READ',
    'INVOICE_READ'
)
ON CONFLICT DO NOTHING;

-- Map cho STAFF
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STAFF' AND p.code IN (
    'PET_READ', 'APPOINTMENT_READ', 'APPOINTMENT_RECEIVE',
    'GROOMING_READ', 'GROOMING_UPDATE',
    'BOARDING_READ', 'BOARDING_UPDATE',
    'INVOICE_MANAGE'
)
ON CONFLICT DO NOTHING;

-- Map cho VETERINARIAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VETERINARIAN' AND p.code IN (
    'PET_READ', 'APPOINTMENT_READ', 'MEDICAL_RECORD_UPDATE', 'PRESCRIPTION_CREATE'
)
ON CONFLICT DO NOTHING;

-- Map cho ADMIN (Full quyền)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- 3. Seed Default Admin Account (admin@pccms.vn / admin123)
INSERT INTO users (id, email, password_hash, full_name, role_id, status_code)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'admin@pccms.vn',
    crypt('admin123', gen_salt('bf')),
    'System Admin',
    (SELECT id FROM roles WHERE code = 'ADMIN'),
    'ACTIVE'
) ON CONFLICT DO NOTHING;

CREATE TABLE staff_profiles (
    user_id             UUID PRIMARY KEY REFERENCES users(id) ON DELETE RESTRICT,
    staff_code          VARCHAR(60) UNIQUE,
    professional_title  VARCHAR(120),
    license_number      VARCHAR(120),
    specialization      VARCHAR(200),
    is_service_provider BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE otp_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id) ON DELETE CASCADE,
    contact         VARCHAR(255) NOT NULL,
    purpose         VARCHAR(60) NOT NULL, -- REGISTER, RESET_PASSWORD, CHANGE_EMAIL, CHANGE_PHONE
    token_hash      TEXT NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,
    attempt_count   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL UNIQUE,
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ
);

-- =========================================================
-- 2. FILE AND MEDIA MANAGEMENT
-- =========================================================
CREATE TABLE file_assets (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_name     VARCHAR(255) NOT NULL,
    stored_key        TEXT NOT NULL UNIQUE,
    mime_type         VARCHAR(120) NOT NULL,
    size_bytes        BIGINT NOT NULL CHECK (size_bytes >= 0),
    checksum_sha256   CHAR(64),
    uploaded_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    visibility_code   file_visibility_enum NOT NULL DEFAULT 'PRIVATE',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE file_links (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id       UUID NOT NULL REFERENCES file_assets(id) ON DELETE CASCADE,
    entity_type   VARCHAR(80) NOT NULL, -- USER, PET, MEDICAL_RECORD, LAB_RESULT, CARE_LOG, etc.
    entity_id     UUID NOT NULL,
    purpose       VARCHAR(80) NOT NULL, -- AVATAR, PET_IMAGE, LAB_ATTACHMENT, CARE_LOG_MEDIA, etc.
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (file_id, entity_type, entity_id, purpose),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 3. PET PROFILE MANAGEMENT
-- =========================================================
CREATE TABLE pet_species (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(80) NOT NULL UNIQUE,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pet_breeds (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    species_id  UUID NOT NULL REFERENCES pet_species(id) ON DELETE RESTRICT,
    name        VARCHAR(120) NOT NULL,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (species_id, name),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pets (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id             UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name                 VARCHAR(80) NOT NULL,
    species_id           UUID NOT NULL REFERENCES pet_species(id) ON DELETE RESTRICT,
    breed_id             UUID REFERENCES pet_breeds(id) ON DELETE RESTRICT,
    sex                  VARCHAR(20) NOT NULL CHECK (sex IN ('MALE','FEMALE','UNKNOWN')),
    birth_date           DATE,
    estimated_age_months INT CHECK (estimated_age_months IS NULL OR estimated_age_months >= 0),
    weight_kg            NUMERIC(7,2) CHECK (weight_kg IS NULL OR weight_kg > 0),
    color                VARCHAR(80),
    attributes           JSONB,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMPTZ,
    CHECK (birth_date IS NOT NULL OR estimated_age_months IS NOT NULL)
);

-- =========================================================
-- 4. CATALOG AND RESOURCE MANAGEMENT
-- =========================================================
CREATE TABLE service_catalog (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_code       VARCHAR(60) NOT NULL UNIQUE,
    name               VARCHAR(160) NOT NULL,
    category_code      service_category_enum NOT NULL,
    description        TEXT,
    base_price_vnd BIGINT NOT NULL DEFAULT 0 CHECK (base_price_vnd >= 0),
    duration_minutes   INT CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from     DATE,
    effective_to       DATE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE TABLE medicine_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE medicines (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medicine_code          VARCHAR(60) NOT NULL UNIQUE,
    name                   VARCHAR(160) NOT NULL,
    category_id            UUID REFERENCES medicine_categories(id) ON DELETE RESTRICT,
    unit                   VARCHAR(40) NOT NULL,
    default_instruction    TEXT,
    current_stock          INT NOT NULL DEFAULT 0 CHECK (current_stock >= 0),
    unit_price_vnd BIGINT NOT NULL DEFAULT 0 CHECK (unit_price_vnd >= 0),
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (name, unit)
);

CREATE TABLE medicine_usage_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medicine_id     UUID NOT NULL REFERENCES medicines(id) ON DELETE CASCADE,
    label           VARCHAR(120) NOT NULL,
    dosage          VARCHAR(120),
    frequency       VARCHAR(120),
    duration_days   INT CHECK (duration_days IS NULL OR duration_days >= 0),
    instruction     TEXT NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE exam_rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_code   VARCHAR(20) NOT NULL UNIQUE,
    name        VARCHAR(80) NOT NULL,
    floor       INT NOT NULL DEFAULT 1,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE grooming_stations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    station_code  VARCHAR(20) NOT NULL UNIQUE,
    name          VARCHAR(80) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE room_types (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                  VARCHAR(60) NOT NULL UNIQUE,
    name                  VARCHAR(120) NOT NULL,
    default_capacity      INT NOT NULL DEFAULT 1 CHECK (default_capacity > 0),
    base_daily_price_vnd BIGINT NOT NULL DEFAULT 0 CHECK (base_daily_price_vnd >= 0),
    description           TEXT,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rooms (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_code          VARCHAR(60) NOT NULL UNIQUE,
    name               VARCHAR(120) NOT NULL,
    room_type_id       UUID NOT NULL REFERENCES room_types(id) ON DELETE RESTRICT,
    floor              INT NOT NULL DEFAULT 1,
    capacity           INT NOT NULL DEFAULT 1 CHECK (capacity > 0),
    status_code        room_status_enum NOT NULL DEFAULT 'AVAILABLE',
    description        TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 5. INTEGRATION SPINE: SERVICE ORDERS
-- Every billable/trackable service occurrence should have one service_orders row.
-- Appointment, grooming, boarding and billing connect through service_order_id.
-- =========================================================
CREATE TABLE service_orders (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code          VARCHAR(60) NOT NULL UNIQUE,
    owner_id            UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    pet_id              UUID NOT NULL REFERENCES pets(id) ON DELETE RESTRICT,
    service_id          UUID NOT NULL REFERENCES service_catalog(id) ON DELETE RESTRICT,
    category_code      service_category_enum NOT NULL,
    status_code         service_order_status_enum NOT NULL DEFAULT 'REQUESTED',
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    planned_start_at    TIMESTAMPTZ,
    planned_end_at      TIMESTAMPTZ,
    actual_start_at     TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason TEXT,
    base_amount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (base_amount_vnd >= 0),
    extra_amount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (extra_amount_vnd >= 0),
    final_amount_vnd BIGINT CHECK (final_amount_vnd IS NULL OR final_amount_vnd >= 0),
    created_by          UUID REFERENCES users(id),
    updated_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (planned_end_at IS NULL OR planned_start_at IS NULL OR planned_end_at > planned_start_at)
);

-- =========================================================
-- 6. APPOINTMENT AND RECEPTION
-- Used for medical appointments and grooming appointments.
-- =========================================================
CREATE TABLE appointments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_order_id     UUID NOT NULL UNIQUE REFERENCES service_orders(id) ON DELETE RESTRICT,
    appointment_type      VARCHAR(30) NOT NULL CHECK (appointment_type IN ('MEDICAL','GROOMING','OTHER')),
    scheduled_start_at    TIMESTAMPTZ NOT NULL,
    scheduled_end_at      TIMESTAMPTZ NOT NULL,
    requested_staff_id    UUID REFERENCES users(id) ON DELETE RESTRICT,
    assigned_staff_id     UUID REFERENCES users(id) ON DELETE RESTRICT,
    exam_room_id          UUID REFERENCES exam_rooms(id) ON DELETE SET NULL,
    status_code           appointment_status_enum NOT NULL DEFAULT 'PENDING',
    symptom_text          TEXT,
    owner_note            TEXT,
    internal_note         TEXT,
    created_by            UUID REFERENCES users(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (scheduled_end_at > scheduled_start_at)
);

CREATE TABLE reception_tickets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id   UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE RESTRICT,
    checked_in_by    UUID REFERENCES users(id) ON DELETE RESTRICT,
    checked_in_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    queue_number     INT,
    assigned_vet_id  UUID REFERENCES users(id) ON DELETE RESTRICT,
    note             TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 7. MEDICAL EXAMINATION AND TREATMENT
-- =========================================================
CREATE TABLE medical_records (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_code               VARCHAR(60) NOT NULL UNIQUE,
    appointment_id            UUID UNIQUE REFERENCES appointments(id) ON DELETE RESTRICT,
    pet_id                    UUID NOT NULL REFERENCES pets(id) ON DELETE RESTRICT,
    vet_id                    UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    record_status             VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (record_status IN ('DRAFT','FINALIZED','CANCELLED')),
    temperature_c             NUMERIC(5,2),
    heart_rate_bpm            INT,
    respiratory_rate_bpm      INT,
    weight_kg                 NUMERIC(7,2),
    blood_pressure            VARCHAR(40),
    spo2_percent              INT CHECK (spo2_percent IS NULL OR (spo2_percent BETWEEN 0 AND 100)),
    mucous_membrane_color     VARCHAR(80),
    capillary_refill_seconds  NUMERIC(5,2),
    preliminary_diagnosis     TEXT,
    final_diagnosis           TEXT,
    treatment_note            TEXT,
    follow_up_at              TIMESTAMPTZ,
    locked_at                 TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE lab_results (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medical_record_id  UUID NOT NULL REFERENCES medical_records(id) ON DELETE CASCADE,
    test_name          VARCHAR(160) NOT NULL,
    result_text        TEXT,
    file_id            UUID REFERENCES file_assets(id) ON DELETE SET NULL,
    created_by         UUID REFERENCES users(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE prescriptions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_code  VARCHAR(60) NOT NULL UNIQUE,
    medical_record_id  UUID NOT NULL REFERENCES medical_records(id) ON DELETE RESTRICT,
    vet_id             UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    note               TEXT,
    issued_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE prescription_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id   UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    medicine_id       UUID NOT NULL REFERENCES medicines(id) ON DELETE RESTRICT,
    dosage            VARCHAR(120) NOT NULL,
    quantity          INT NOT NULL CHECK (quantity > 0),
    instruction       TEXT,
    unit_price_vnd BIGINT NOT NULL DEFAULT 0 CHECK (unit_price_vnd >= 0),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vaccination_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_id               UUID NOT NULL REFERENCES pets(id) ON DELETE RESTRICT,
    medical_record_id    UUID REFERENCES medical_records(id) ON DELETE SET NULL,
    vaccine_name         VARCHAR(160) NOT NULL,
    vaccination_date     DATE NOT NULL,
    next_due_date        DATE,
    note                 TEXT,
    created_by           UUID REFERENCES users(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (next_due_date IS NULL OR next_due_date >= vaccination_date),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE health_alerts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_id             UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    medical_record_id  UUID REFERENCES medical_records(id) ON DELETE SET NULL,
    severity           VARCHAR(30) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH')),
    message            TEXT NOT NULL,
    resolved_at        TIMESTAMPTZ,
    created_by         UUID REFERENCES users(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 8. GROOMING SERVICE MANAGEMENT
-- =========================================================
CREATE TABLE grooming_tickets (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id       UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE RESTRICT,
    assigned_staff_id    UUID REFERENCES users(id) ON DELETE RESTRICT,
    station_id           UUID REFERENCES grooming_stations(id) ON DELETE SET NULL,
    status_code          grooming_status_enum NOT NULL DEFAULT 'PENDING',
    started_at           TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    owner_note           TEXT,
    internal_note        TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 9. BOARDING MANAGEMENT
-- =========================================================
CREATE TABLE boarding_bookings (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_code              VARCHAR(60) NOT NULL UNIQUE,
    service_order_id          UUID NOT NULL UNIQUE REFERENCES service_orders(id) ON DELETE RESTRICT,
    owner_id                  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    pet_id                    UUID NOT NULL REFERENCES pets(id) ON DELETE RESTRICT,
    requested_room_type_id    UUID NOT NULL REFERENCES room_types(id) ON DELETE RESTRICT,
    expected_checkin_at       TIMESTAMPTZ NOT NULL,
    expected_checkout_at      TIMESTAMPTZ NOT NULL,
    special_care_request      TEXT,
    estimated_price_vnd BIGINT NOT NULL DEFAULT 0 CHECK (estimated_price_vnd >= 0),
    status_code               boarding_status_enum NOT NULL DEFAULT 'RESERVED',
    created_by                UUID REFERENCES users(id),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (expected_checkout_at > expected_checkin_at)
);

CREATE TABLE room_allocations (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL REFERENCES boarding_bookings(id) ON DELETE RESTRICT,
    room_id             UUID NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    allocated_from      TIMESTAMPTZ NOT NULL,
    allocated_to        TIMESTAMPTZ NOT NULL,
    allocated_by        UUID REFERENCES users(id),
    released_at         TIMESTAMPTZ,
    status_code         VARCHAR(30) NOT NULL DEFAULT 'ALLOCATED' CHECK (status_code IN ('ALLOCATED','RELEASED','CANCELLED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (allocated_to > allocated_from),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE boarding_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id           UUID NOT NULL UNIQUE REFERENCES boarding_bookings(id) ON DELETE RESTRICT,
    room_allocation_id   UUID REFERENCES room_allocations(id) ON DELETE RESTRICT,
    actual_checkin_at    TIMESTAMPTZ,
    actual_checkout_at   TIMESTAMPTZ,
    checked_in_by        UUID REFERENCES users(id),
    checked_out_by       UUID REFERENCES users(id),
    status_code               boarding_status_enum NOT NULL DEFAULT 'RESERVED',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (actual_checkout_at IS NULL OR actual_checkin_at IS NULL OR actual_checkout_at >= actual_checkin_at)
);

CREATE TABLE care_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id         UUID NOT NULL REFERENCES boarding_sessions(id) ON DELETE CASCADE,
    pet_id             UUID NOT NULL REFERENCES pets(id) ON DELETE RESTRICT,
    staff_id           UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    log_date           DATE NOT NULL,
    period_code        care_period_enum NOT NULL,
    feeding_status     VARCHAR(120) NOT NULL,
    hygiene_status     VARCHAR(120) NOT NULL,
    health_note        TEXT,
    staff_note         TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, log_date, period_code)
);

CREATE TABLE care_log_media (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_log_id  UUID NOT NULL REFERENCES care_logs(id) ON DELETE CASCADE,
    file_id      UUID NOT NULL REFERENCES file_assets(id) ON DELETE RESTRICT,
    caption      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (care_log_id, file_id),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 10. STAFF SCHEDULE AND SHIFT MANAGEMENT
-- =========================================================
CREATE TABLE shifts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(60) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (end_time > start_time),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE work_schedules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id         UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    work_date        DATE NOT NULL,
    shift_id         UUID NOT NULL REFERENCES shifts(id) ON DELETE RESTRICT,
    exam_room_id     UUID REFERENCES exam_rooms(id) ON DELETE SET NULL,
    station_id       UUID REFERENCES grooming_stations(id) ON DELETE SET NULL,
    role_id          UUID REFERENCES roles(id) ON DELETE RESTRICT,
    capacity         INT NOT NULL DEFAULT 1 CHECK (capacity > 0),
    status_code      schedule_status_enum NOT NULL DEFAULT 'ASSIGNED',
    note             TEXT,
    created_by       UUID REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (staff_id, work_date, shift_id)
);

CREATE TABLE shift_change_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id     UUID NOT NULL REFERENCES work_schedules(id) ON DELETE CASCADE,
    requested_by    UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    target_staff_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    reason          TEXT NOT NULL,
    status_code     shift_request_status_enum NOT NULL DEFAULT 'PENDING',
    resolved_by     UUID REFERENCES users(id),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 11. BILLING AND PAYMENT
-- =========================================================
CREATE TABLE invoices (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_code        VARCHAR(60) NOT NULL UNIQUE,
    owner_id            UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    pet_id              UUID REFERENCES pets(id) ON DELETE RESTRICT,
    status_code         invoice_status_enum NOT NULL DEFAULT 'UNPAID',
    issued_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    due_at              TIMESTAMPTZ,
    discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (discount_vnd >= 0),
    tax_vnd BIGINT NOT NULL DEFAULT 0 CHECK (tax_vnd >= 0),
    total_amount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (total_amount_vnd >= 0),
    paid_amount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount_vnd >= 0),
    note                TEXT,
    created_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (total_amount_vnd >= discount_vnd)
);

CREATE TABLE invoice_lines (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id         UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    service_order_id   UUID REFERENCES service_orders(id) ON DELETE RESTRICT,
    medicine_id        UUID REFERENCES medicines(id) ON DELETE RESTRICT,
    description        TEXT NOT NULL,
    quantity           NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price_vnd BIGINT NOT NULL DEFAULT 0 CHECK (unit_price_vnd >= 0),
    subtotal_vnd BIGINT GENERATED ALWAYS AS (CAST(quantity * unit_price_vnd AS BIGINT)) STORED,
    line_order         INT NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_code         VARCHAR(60) NOT NULL UNIQUE,
    invoice_id           UUID NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd > 0),
    method_code          payment_method_enum NOT NULL,
    status_code          payment_status_enum NOT NULL DEFAULT 'PENDING',
    paid_at              TIMESTAMPTZ,
    received_by          UUID REFERENCES users(id),
    note                 TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 12. NOTIFICATION AND REMINDER
-- =========================================================
CREATE TABLE notifications (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_type         VARCHAR(80),
    source_id           UUID,
    notification_type   VARCHAR(80) NOT NULL,
    title               VARCHAR(200) NOT NULL,
    body                TEXT NOT NULL,
    status_code         notification_status_enum NOT NULL DEFAULT 'UNREAD',
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 13. SEED DATA FOR CATALOG AND SETTINGS
-- =========================================================
INSERT INTO pet_species (name) VALUES ('Chó'), ('Mèo'), ('Thỏ'), ('Chim') ON CONFLICT (name) DO NOTHING;

INSERT INTO pet_breeds (species_id, name)
SELECT s.id, b.name
FROM pet_species s
JOIN (VALUES
    ('Chó', 'Poodle'),
    ('Chó', 'Husky'),
    ('Chó', 'Corgi'),
    ('Mèo', 'Anh lông ngắn'),
    ('Mèo', 'Ba Tư'),
    ('Mèo', 'Munchkin'),
    ('Thỏ', 'Thỏ mini'),
    ('Chim', 'Vẹt'),
    ('Chim', 'Yến phụng')
) AS b(species_name, name) ON s.name = b.species_name
ON CONFLICT (species_id, name) DO NOTHING;

INSERT INTO shifts (code, name, start_time, end_time) VALUES
('MORNING', 'Ca sáng', '07:00', '12:00'),
('AFTERNOON', 'Ca chiều', '12:00', '17:00'),
('EVENING', 'Ca tối', '17:00', '22:00')
ON CONFLICT DO NOTHING;

INSERT INTO exam_rooms (room_code, name, floor) VALUES
('PKB-01', 'Phòng khám 1', 1),
('PKB-02', 'Phòng khám 2', 1)
ON CONFLICT DO NOTHING;

-- Bác sĩ thú y mặc định (phục vụ đặt lịch / khung giờ khám)
INSERT INTO users (id, email, phone, password_hash, full_name, role_id, status_code)
VALUES
    ('11111111-1111-1111-1111-000000000001', 'vet.an@pccms.vn', '0901000001',
     crypt('vet123', gen_salt('bf')), 'Trần Văn An',
     (SELECT id FROM roles WHERE code = 'VETERINARIAN'), 'ACTIVE'),
    ('11111111-1111-1111-1111-000000000002', 'vet.huong@pccms.vn', '0901000002',
     crypt('vet123', gen_salt('bf')), 'Lê Thị Hương',
     (SELECT id FROM roles WHERE code = 'VETERINARIAN'), 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

INSERT INTO staff_profiles (user_id, staff_code, professional_title, is_service_provider)
SELECT u.id, v.staff_code, 'Bác sĩ thú y', TRUE
FROM (VALUES
    ('11111111-1111-1111-1111-000000000001'::uuid, 'VET-001'),
    ('11111111-1111-1111-1111-000000000002'::uuid, 'VET-002')
) AS v(user_id, staff_code)
JOIN users u ON u.id = v.user_id
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO work_schedules (staff_id, work_date, shift_id, exam_room_id, capacity, status_code)
SELECT v.staff_id, d.work_date, sh.id, er.id, 4, 'ASSIGNED'
FROM (VALUES
    ('11111111-1111-1111-1111-000000000001'::uuid),
    ('11111111-1111-1111-1111-000000000002'::uuid)
) AS v(staff_id)
CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + 60, INTERVAL '1 day') AS d(work_date)
CROSS JOIN shifts sh
CROSS JOIN LATERAL (SELECT id FROM exam_rooms WHERE room_code = 'PKB-01' LIMIT 1) er
WHERE sh.code IN ('MORNING', 'AFTERNOON')
ON CONFLICT (staff_id, work_date, shift_id) DO NOTHING;

-- Nhân viên lễ tân mặc định (tiếp nhận lịch hẹn UC013)
INSERT INTO users (id, email, phone, password_hash, full_name, role_id, status_code)
VALUES (
    '22222222-2222-2222-2222-000000000001',
    'staff.le@pccms.vn',
    '0902000001',
    crypt('staff123', gen_salt('bf')),
    'Nguyễn Thị Lệ',
    (SELECT id FROM roles WHERE code = 'STAFF'),
    'ACTIVE'
) ON CONFLICT (email) DO NOTHING;

INSERT INTO staff_profiles (user_id, staff_code, professional_title, is_service_provider)
VALUES (
    '22222222-2222-2222-2222-000000000001',
    'STAFF-001',
    'Nhân viên tiếp nhận',
    FALSE
) ON CONFLICT (user_id) DO NOTHING;

INSERT INTO medicine_categories (id, name, description) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-000000000001', 'Kháng sinh', 'Nhóm thuốc kháng sinh điều trị nhiễm khuẩn'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000002', 'Giảm đau - Hạ sốt', 'Thuốc giảm đau, hạ sốt cho thú cưng'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000003', 'Vitamin & Bổ sung', 'Vitamin và dưỡng chất bổ sung'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000004', 'Kháng viêm', 'Thuốc kháng viêm, chống dị ứng'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000005', 'Tiêu hóa', 'Thuốc hỗ trợ tiêu hóa')
ON CONFLICT DO NOTHING;

INSERT INTO medicines (medicine_code, name, category_id, unit, default_instruction, current_stock, unit_price_vnd) VALUES
('MED001', 'Amoxicillin 500mg', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001', 'Viên', 'Ngày 2 lần sau ăn', 120, 5000),
('MED002', 'Meloxicam 1.5mg', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002', 'Viên', 'Ngày 1 lần sau ăn', 80, 8000),
('MED003', 'Vitamin B Complex', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003', 'Viên', 'Ngày 1 lần', 200, 3000),
('MED004', 'Prednisolone 5mg', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000004', 'Viên', 'Theo chỉ định bác sĩ', 60, 4500),
('MED005', 'Probiotics Pet', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000005', 'Gói', 'Pha vào thức ăn ngày 1 lần', 45, 12000)
ON CONFLICT DO NOTHING;

INSERT INTO grooming_stations (station_code, name) VALUES
('SPA-01', 'Bàn spa 1'),
('SPA-02', 'Bàn spa 2'),
('TAM-01', 'Bàn tắm 1')
ON CONFLICT DO NOTHING;

INSERT INTO room_types (code, name, base_daily_price_vnd) VALUES
('STANDARD', 'Phòng thường', 150000),
('VIP', 'Phòng VIP', 300000),
('ISOLATION', 'Phòng cách ly', 200000)
ON CONFLICT DO NOTHING;

INSERT INTO service_catalog (service_code, name, category_code, base_price_vnd, duration_minutes) VALUES
('MED-GENERAL', 'Khám tổng quát', 'MEDICAL', 200000, 30),
('MED-VACCINE', 'Tiêm phòng', 'MEDICAL', 150000, 20),
('GRM-BATH', 'Tắm gội cơ bản', 'GROOMING', 100000, 60),
('GRM-FULL', 'Làm đẹp toàn thân', 'GROOMING', 250000, 120),
('BRD-STAY', 'Lưu trú theo ngày', 'BOARDING', 150000, NULL)
ON CONFLICT DO NOTHING;

-- =========================================================
INSERT INTO rooms (room_code, name, room_type_id, floor, capacity)
SELECT 'P10' || gs.n, 'Phòng 10' || gs.n, rt.id, 1, 1
FROM room_types rt, generate_series(1,3) gs(n)
WHERE rt.code = 'STANDARD'
ON CONFLICT DO NOTHING;

INSERT INTO rooms (room_code, name, room_type_id, floor, capacity)
SELECT 'VIP-0' || gs.n, 'Phòng VIP 0' || gs.n, rt.id, 2, 1
FROM room_types rt, generate_series(1,2) gs(n)
WHERE rt.code = 'VIP'
ON CONFLICT DO NOTHING;

-- 14. USEFUL REPORTING VIEWS
-- =========================================================
CREATE OR REPLACE VIEW v_revenue_by_service_day AS
SELECT
    DATE(i.issued_at) AS report_date,
    sc.category_code,
    sc.id AS service_id,
    sc.name AS service_name,
    SUM(il.subtotal_vnd) AS revenue_vnd,
    COUNT(DISTINCT i.id) AS invoice_count
FROM invoices i
JOIN invoice_lines il ON il.invoice_id = i.id
LEFT JOIN service_orders so ON so.id = il.service_order_id
LEFT JOIN service_catalog sc ON sc.id = so.service_id
WHERE i.status_code IN ('PAID','PARTIALLY_PAID')
GROUP BY DATE(i.issued_at), sc.category_code, sc.id, sc.name;

CREATE OR REPLACE VIEW v_pet_service_history AS
SELECT
    so.id AS service_order_id,
    so.pet_id,
    p.owner_id,
    so.category_code,
    so.status_code,
    so.planned_start_at,
    so.completed_at,
    sc.name AS service_name,
    so.final_amount_vnd
FROM service_orders so
JOIN pets p ON p.id = so.pet_id
JOIN service_catalog sc ON sc.id = so.service_id;

-- =========================================================
-- 15. INDEXES FOR MAIN QUERIES AND CONFLICT CHECKS
-- =========================================================
CREATE INDEX idx_users_email_lower ON users (LOWER(email));
CREATE INDEX idx_users_status ON users (status_code);

CREATE INDEX idx_pets_owner ON pets (owner_id, is_active);
CREATE INDEX idx_pets_species ON pets (species_id, breed_id);

CREATE INDEX idx_service_catalog_category_active ON service_catalog (category_code, is_active);
CREATE INDEX idx_medicines_active ON medicines (is_active, name);
CREATE INDEX idx_rooms_type_status ON rooms (room_type_id, status_code);

CREATE INDEX idx_service_orders_owner ON service_orders (owner_id, requested_at DESC);
CREATE INDEX idx_service_orders_pet ON service_orders (pet_id, requested_at DESC);
CREATE INDEX idx_service_orders_status_time ON service_orders (status_code, planned_start_at);
CREATE INDEX idx_service_orders_category ON service_orders (category_code, planned_start_at);

CREATE INDEX idx_appointments_time ON appointments (scheduled_start_at, scheduled_end_at);
CREATE INDEX idx_appointments_staff_time ON appointments (assigned_staff_id, scheduled_start_at, scheduled_end_at);
CREATE INDEX idx_appointments_status ON appointments (status_code, scheduled_start_at);

CREATE INDEX idx_medical_records_pet ON medical_records (pet_id, created_at DESC);
CREATE INDEX idx_medical_records_vet ON medical_records (vet_id, created_at DESC);
CREATE INDEX idx_prescriptions_medical ON prescriptions (medical_record_id);

CREATE INDEX idx_grooming_status ON grooming_tickets (status_code, created_at DESC);

CREATE INDEX idx_boarding_bookings_pet ON boarding_bookings (pet_id, expected_checkin_at DESC);
CREATE INDEX idx_boarding_bookings_status ON boarding_bookings (status_code, expected_checkin_at, expected_checkout_at);
CREATE INDEX idx_room_allocations_room_time ON room_allocations (room_id, allocated_from, allocated_to);
CREATE INDEX idx_care_logs_session_date ON care_logs (session_id, log_date DESC);

CREATE INDEX idx_work_schedules_staff_date ON work_schedules (staff_id, work_date);
CREATE INDEX idx_work_schedules_date_shift ON work_schedules (work_date, shift_id, status_code);

CREATE INDEX idx_invoices_owner_status ON invoices (owner_id, status_code, issued_at DESC);
CREATE INDEX idx_invoice_lines_order ON invoice_lines (service_order_id);
CREATE INDEX idx_payments_invoice_status ON payments (invoice_id, status_code);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id, status_code, created_at DESC);

CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_otp_tokens_expires_at ON otp_tokens (expires_at);
