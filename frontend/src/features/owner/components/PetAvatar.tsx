import { PawPrint } from 'lucide-react';
import type { PetResponse } from '~/types/pet';

interface PetAvatarProps {
  pet: Pick<PetResponse, 'name'>;
  imageUrl?: string | null;
  className?: string;
}

export function PetAvatar({ pet, imageUrl, className = 'h-36' }: PetAvatarProps) {
  const initial = pet.name.trim().charAt(0).toUpperCase() || '?';

  if (imageUrl) {
    return (
      <img
        src={imageUrl}
        alt={`Ảnh đại diện ${pet.name}`}
        className={`${className} w-full object-cover`}
      />
    );
  }

  return (
    <div
      className={`${className} flex w-full items-center justify-center bg-linear-to-br from-amber-100 via-emerald-50 to-sky-100`}
      aria-hidden
    >
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-white/80 text-xl font-bold text-emerald-700 shadow-sm">
        {initial !== '?' ? initial : <PawPrint className="h-8 w-8" />}
      </div>
    </div>
  );
}
