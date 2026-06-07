---
trigger: always_on
---

# API CONTRACTS & DTO SPECIFICATIONS

## 1. General DTO Rules

- NEVER expose Entity classes directly in Controllers. Always create `RequestDTO` and `ResponseDTO` classes.
- Use MapStruct (or explicit builder patterns) to map between Entities and DTOs.
- Use `javax.validation` annotations (`@NotBlank`, `@NotNull`, `@Min`) on all Request DTOs.

## 2. Specific Contract: Pet Profile

- **CreatePetRequest:** Exclude `is_active` and `id` (these are handled by the system).
- **PetProfileResponse:** Must include the Owner's `fullName` and `phoneNumber`, but MUST STRICTLY EXCLUDE any `Account` authentication data (`password_hash`, tokens).

## 3. Specific Contract: Medical Record

- **UpdateVitalsRequest:** Only contain fields for temperature, heart rate, spo2, etc.
- **PrescriptionItemRequest:** Must explicitly require `medicineId` and `quantity`. Agent must ensure these are non-null before the Service layer processes the stock check.
