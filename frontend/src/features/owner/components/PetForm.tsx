import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button, Input, Select, Textarea } from "~/components/atoms";
import { petApi } from "~/shared/api/petApi";
import type { PetRequest } from "~/types/pet";

const petSchema = z.object({
    name: z.string().min(1, "Vui lòng nhập tên thú cưng"),
    speciesId: z.string().min(1, "Vui lòng chọn loài"),
    breedId: z.string().optional(),
    sex: z.enum(["MALE", "FEMALE", "UNKNOWN"]),
    birthDate: z.string().optional(),
    weightKg: z.string().optional(),
    color: z.string().optional(),
    identificationNote: z.string().optional(),
    specialNote: z.string().optional(),
    allergyNote: z.string().optional(),
    nutritionNote: z.string().optional(),
});

type PetFormValues = z.infer<typeof petSchema>;

interface PetFormProps {
    onSuccess: () => void;
    onCancel: () => void;
}

export function PetForm({ onSuccess, onCancel }: PetFormProps) {
    const queryClient = useQueryClient();

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<PetFormValues>({
        resolver: zodResolver(petSchema),
        defaultValues: {
            sex: "UNKNOWN",
        },
    });

    const createPetMutation = useMutation({
        mutationFn: (data: PetRequest) => petApi.createPet(data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["pets"] });
            onSuccess();
        },
    });

    const onSubmit = (data: PetFormValues) => {
        // Transform empty strings to undefined to match optional backend fields
        const requestData: PetRequest = {
            ...data,
            weightKg: data.weightKg ? Number(data.weightKg) : undefined,
        };
        createPetMutation.mutate(requestData);
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input
                    label="Tên thú cưng"
                    placeholder="Milu"
                    {...register("name")}
                    error={errors.name?.message}
                />
                <Select
                    label="Loài"
                    options={["CHÓ", "MÈO", "THỎ", "CHIM"]}
                    {...register("speciesId")}
                    error={errors.speciesId?.message}
                />
                <Input
                    label="Giống"
                    placeholder="Poodle"
                    {...register("breedId")}
                    error={errors.breedId?.message}
                />
                <Input
                    label="Ngày sinh"
                    type="date"
                    {...register("birthDate")}
                    error={errors.birthDate?.message}
                />
                <Input
                    label="Cân nặng (kg)"
                    placeholder="4.5"
                    type="number"
                    step="0.1"
                    {...register("weightKg")}
                    error={errors.weightKg?.message}
                />
                <Select
                    label="Giới tính"
                    options={["MALE", "FEMALE", "UNKNOWN"]}
                    {...register("sex")}
                    error={errors.sex?.message}
                />
            </div>
            <div className="mt-4 grid gap-4 md:grid-cols-[1fr_1fr]">
                <Textarea
                    label="Ghi chú đặc biệt"
                    placeholder="Dị ứng, thói quen, tiền sử bệnh..."
                    {...register("specialNote")}
                    error={errors.specialNote?.message}
                />
                {/* Placeholder for avatar upload */}
                <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 p-5">
                    <p className="font-medium">Ảnh đại diện</p>
                    <p className="mt-2 text-sm text-slate-500">
                        Tính năng này sẽ được cập nhật sau.
                    </p>
                </div>
            </div>

            {createPetMutation.isError && (
                <p className="text-red-500 text-sm">Có lỗi xảy ra khi lưu thú cưng</p>
            )}

            <div className="mt-5 flex gap-2">
                <Button type="submit" disabled={createPetMutation.isPending}>
                    {createPetMutation.isPending ? "Đang lưu..." : "Lưu thú cưng"}
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    onClick={onCancel}
                    disabled={createPetMutation.isPending}
                >
                    Hủy
                </Button>
            </div>
        </form>
    );
}
