import { describe, it, expect } from 'vitest';
import { petFormSchema, PET_MESSAGES } from '~/features/owner/schema/petSchema';

const validBase = {
  name: 'Milu',
  speciesId: 'species-1',
  breedId: '',
  sex: 'MALE' as const,
  birthDate: '2023-01-01',
  estimatedAgeMonths: '',
  weightKg: '5.5',
  color: '',
  identificationNote: '',
  specialNote: '',
  allergyNote: '',
  nutritionNote: '',
};

describe('petFormSchema', () => {
  it('accepts valid form values', () => {
    const result = petFormSchema.safeParse(validBase);
    expect(result.success).toBe(true);
  });

  it('rejects empty required name', () => {
    const result = petFormSchema.safeParse({ ...validBase, name: '' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe(PET_MESSAGES.requiredFields);
    }
  });

  it('rejects invalid weight', () => {
    const zero = petFormSchema.safeParse({ ...validBase, weightKg: '0' });
    expect(zero.success).toBe(false);
    if (!zero.success) {
      expect(zero.error.issues.some((i) => i.message === PET_MESSAGES.invalidWeight)).toBe(true);
    }

    const negative = petFormSchema.safeParse({ ...validBase, weightKg: '-1' });
    expect(negative.success).toBe(false);
  });

  it('requires birth date or estimated age', () => {
    const result = petFormSchema.safeParse({
      ...validBase,
      birthDate: '',
      estimatedAgeMonths: '',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('birthDate'))).toBe(true);
    }
  });

  it('accepts estimated age instead of birth date', () => {
    const result = petFormSchema.safeParse({
      ...validBase,
      birthDate: '',
      estimatedAgeMonths: '12',
    });
    expect(result.success).toBe(true);
  });

  it('rejects special characters in pet name', () => {
    const result = petFormSchema.safeParse({ ...validBase, name: 'Milu@' });
    expect(result.success).toBe(false);
  });
});
