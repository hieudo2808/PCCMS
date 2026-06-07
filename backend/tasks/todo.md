- [x] Slice 1: User Account Administration & Token Revocation
  - Acceptance: Admin can lock/disable users. Tokens are revoked automatically.
  - Verify: Run Integration Test for User Lock & Token Revocation.
  - Files: `UserController.java`, `UserService.java`, `RefreshTokenRepository.java`, `UserServiceTest.java`.

- [ ] Slice 2: Pet Profiles Core & Validation (TDD)
  - Acceptance: Age/Weight validation strictly enforced. Pets are soft-deleted.
  - Verify: Run Parameterized tests via `PetServiceTest`.
  - Files: `Pets.java`, `PetSpecies.java`, `PetBreeds.java`, `PetService.java`, `PetServiceTest.java`, `pet-age-validation.csv`.

- [/] Slice 3: Pet APIs & IDOR Prevention
  - Acceptance: Customers get 403 when modifying others' pets. No sensitive fields returned.
  - Verify: Run MockMvc tests in `PetControllerTest`.
  - Files: `PetController.java`, `PetService.java`, `PetControllerTest.java`, `PetMapper.java`.

- [ ] Slice 4: Event-Driven Deactivation
  - Acceptance: `PetDeactivatedEvent` is fired when a pet becomes inactive.
  - Verify: Verify `ApplicationEventPublisher.publishEvent()` in `PetServiceTest`.
  - Files: `PetDeactivatedEvent.java`, `PetService.java`.
