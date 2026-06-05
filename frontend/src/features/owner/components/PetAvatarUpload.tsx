import { useRef, useState } from 'react';
import { PET_AVATAR_MAX_BYTES, PET_MESSAGES } from '../schema/petSchema';

interface PetAvatarUploadProps {
  previewUrl: string | null;
  onPreviewChange: (url: string | null) => void;
  onError: (message: string | null) => void;
}

export function PetAvatarUpload({ previewUrl, onPreviewChange, onError }: PetAvatarUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [fileName, setFileName] = useState<string | null>(null);

  const handleFile = (file: File | undefined) => {
    if (!file) return;

    if (file.size > PET_AVATAR_MAX_BYTES) {
      onError(PET_MESSAGES.imageTooLarge);
      if (inputRef.current) inputRef.current.value = '';
      return;
    }

    if (!file.type.startsWith('image/')) {
      onError('Vui lòng chọn file ảnh hợp lệ');
      return;
    }

    onError(null);
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = () => onPreviewChange(reader.result as string);
    reader.readAsDataURL(file);
  };

  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 p-5">
      <p className="font-medium">Ảnh đại diện</p>
      <p className="mt-1 text-sm text-slate-500">Tối đa 2MB (JPG, PNG, WebP)</p>

      {previewUrl && (
        <img
          src={previewUrl}
          alt="Xem trước ảnh đại diện"
          className="mt-3 h-28 w-28 rounded-2xl object-cover border border-slate-200"
        />
      )}

      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="mt-3 block w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-emerald-600 file:px-3 file:py-2 file:text-sm file:font-medium file:text-white hover:file:bg-emerald-700"
        onChange={(e) => handleFile(e.target.files?.[0])}
      />

      {fileName && (
        <p className="mt-2 text-xs text-slate-500">
          Đã chọn: {fileName} (lưu server sẽ bổ sung sau)
        </p>
      )}
    </div>
  );
}
