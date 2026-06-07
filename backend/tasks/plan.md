# Implementation Plan: IAM Account & Pet Administration

## Dependency Graph
```
[User & Auth DB] <── [Token Management]
      │
      ▼
[Pet Profiles DB] <── [Pet Service & Validation]
      │
      ▼
[IDOR Security Guardrails]
      │
      ▼
[Event Publisher (Auto-Cancel Appointments)]
```

## Vertical Slices & Tasks

### Slice 1: User Account Administration & Token Revocation
- **Objective**: Admin can lock/disable users and their sessions are automatically invalidated.
- **Tasks**:
  1. Add Admin APIs in `UserController` (Lock, Disable).
  2. Implement `RefreshTokenService.revokeAllUserTokens(userId)` and integrate with User status update.
  3. Write Integration Test for Token Revocation when account is locked.

### Slice 2: Pet Profiles Core & Validation (TDD)
- **Objective**: Establish Pet entities and business validation (Age, Weight, Soft Delete).
- **Tasks**:
  1. Create `Pets`, `PetSpecies`, `PetBreeds` entities.
  2. Write `pet-age-validation.csv` and `PetServiceTest` for age/weight rules.
  3. Implement `PetService` passing the tests.

### Slice 3: Pet APIs & IDOR Prevention
- **Objective**: Ensure Customers only access their own pets.
- **Tasks**:
  1. Create DTOs (`PetRequest`, `PetResponse`) via Java Records & MapStruct.
  2. Implement Ownership guard clause in `PetService`.
  3. Implement `PetController` and write `MockMvc` security tests (403 Forbidden).

### Slice 4: Event-Driven Deactivation
- **Objective**: Auto-cancel pending appointments when a pet is deactivated.
- **Tasks**:
  1. Define `PetDeactivatedEvent`.
  2. Emit event in `PetService.deletePet()`.
  3. Write Unit Test verifying `ApplicationEventPublisher` publishes the event.

## Verification Checkpoints
- After Slice 1: Security tests confirm revoked tokens cannot be refreshed.
- After Slice 2: TDD coverage for Pet Service is >85%, parameterized tests pass.
- After Slice 3: 403 Forbidden correctly returned for unauthorized access.
- After Slice 4: Event is correctly published.
