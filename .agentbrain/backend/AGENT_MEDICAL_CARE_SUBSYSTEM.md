# SUBSYSTEM: MEDICAL CARE (Records & Prescriptions)

## 1. Domain Models (Entities)

### `MedicalRecord` (Mapped to `medical_records` table)

### `Prescription` & `PrescriptionItem`

### `Medicine` (Master Data)

## 2. Strict Business Rules (Agent must implement these checks in Service Layer)

1. **Medical Record Lock (Immutability):** - Before executing `MedicalRecordService.update()`, check `status_code`. If `status_code == 'FINALIZED'`, throw a `BusinessException` (Record locked, cannot modify vitals or diagnosis).
2. **Prescription Inventory Check:**
   - In `PrescriptionService`, before saving a `PrescriptionItem`, agent must fetch `Medicine` by ID.
   - If `item.quantity > medicine.current_stock`, throw `BusinessException` (Insufficient stock).
3. **Vital Signs Validation Constraints:**
   - If provided, `spo2_percent` must be between 0 and 100.
   - `temperature_c` must be a realistic float value.
