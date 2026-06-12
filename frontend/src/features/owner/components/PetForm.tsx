import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Input, Select, Textarea } from "~/components/atoms";
import { petApi } from "~/shared/api/petApi";
import { petCatalogApi } from "~/shared/api/petCatalogApi";
import type { PetRequest, PetResponse } from "~/types/pet";

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
    pet?: PetResponse;
}

function toDefaultValues(pet?: PetResponse): Partial<PetFormValues> {
    return {
        name: pet?.name ?? "",
        speciesId: pet?.speciesId ?? "",
        breedId: pet?.breedId ?? "",
        sex: pet?.sex ?? "UNKNOWN",
        birthDate: pet?.birthDate ?? "",
        weightKg: pet?.weightKg ? String(pet.weightKg) : "",
        color: pet?.color ?? "",
        identificationNote: pet?.identificationNote ?? "",
        specialNote: pet?.specialNote ?? "",
        allergyNote: pet?.allergyNote ?? "",
        nutritionNote: pet?.nutritionNote ?? "",
    };
}

export function PetForm({ onSuccess, onCancel, pet }: PetFormProps) {
    const queryClient = useQueryClient();
    const isEditing = Boolean(pet);

    const {
        register,
        handleSubmit,
        watch,
        reset,
        formState: { errors },
    } = useForm<PetFormValues>({
        resolver: zodResolver(petSchema),
        defaultValues: toDefaultValues(pet),
    });

    const selectedSpeciesId = watch("speciesId");

    useEffect(() => {
        reset(toDefaultValues(pet));
    }, [pet, reset]);

    const { data: speciesList = [] } = useQuery({
        queryKey: ["species"],
        queryFn: petCatalogApi.getSpecies,
    });

    const { data: breedList = [] } = useQuery({
        queryKey: ["breeds", selectedSpeciesId],
        queryFn: () => petCatalogApi.getBreedsBySpecies(selectedSpeciesId),
        enabled: Boolean(selectedSpeciesId),
    });

    const speciesOptions = speciesList.map((species: any) => ({
        value: species.id,
        label: species.name,
    }));

    const breedOptions = breedList.map((breed: any) => ({
        value: breed.id,
        label: breed.name,
    }));

    const createPetMutation = useMutation({
        mutationFn: (data: PetRequest) => petApi.createPet(data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["pets"] });
            onSuccess();
        },
    });

    const updatePetMutation = useMutation({
        mutationFn: (data: PetRequest) => petApi.updatePet(pet!.id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["pets"] });
            onSuccess();
        },
    });

    const onSubmit = (data: PetFormValues) => {
        const requestData: PetRequest = {
            ...data,
            breedId: data.breedId || undefined,
            birthDate: data.birthDate || undefined,
            weightKg: data.weightKg ? Number(data.weightKg) : undefined,
        };

        if (isEditing) {
            updatePetMutation.mutate(requestData);
            return;
        }

        createPetMutation.mutate(requestData);
    };

    const isPending = createPetMutation.isPending || updatePetMutation.isPending;
    const isError = createPetMutation.isError || updatePetMutation.isError;

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
                    placeholder="Chọn loài"
                    options={speciesOptions}
                    {...register("speciesId")}
                    error={errors.speciesId?.message}
                />
                <Select
                    label="Giống"
                    placeholder={selectedSpeciesId ? "Chọn giống thú cưng" : "Chọn loài trước"}
                    options={breedOptions}
                    disabled={!selectedSpeciesId}
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
                    options={[
                        { value: "MALE", label: "Đực" },
                        { value: "FEMALE", label: "Cái" },
                        { value: "UNKNOWN", label: "Chưa rõ" },
                    ]}
                    {...register("sex")}
                    error={errors.sex?.message}
                />
                <Input
                    label="Màu lông"
                    placeholder="Vàng nâu"
                    {...register("color")}
                    error={errors.color?.message}
                />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
                <Textarea
                    label="Dấu hiệu nhận diện"
                    placeholder="Vòng cổ, chip, đặc điểm riêng..."
                    {...register("identificationNote")}
                    error={errors.identificationNote?.message}
                />
                <Textarea
                    label="Ghi chú đặc biệt"
                    placeholder="Thói quen, tiền sử bệnh..."
                    {...register("specialNote")}
                    error={errors.specialNote?.message}
                />
                <Textarea
                    label="Dị ứng"
                    placeholder="Thức ăn, thuốc, mỹ phẩm..."
                    {...register("allergyNote")}
                    error={errors.allergyNote?.message}
                />
                <Textarea
                    label="Dinh dưỡng"
                    placeholder="Khẩu phần, thức ăn quen dùng..."
                    {...register("nutritionNote")}
                    error={errors.nutritionNote?.message}
                />
            </div>

            {isError && (
                <p className="text-sm text-red-500">Có lỗi xảy ra khi lưu thú cưng</p>
            )}

            <div className="flex gap-2">
                <Button type="submit" disabled={isPending}>
                    {isPending ? "Đang lưu..." : isEditing ? "Cập nhật thú cưng" : "Lưu thú cưng"}
                </Button>
                <Button type="button" variant="outline" onClick={onCancel} disabled={isPending}>
                    Hủy
                </Button>
            </div>
        </form>
    );
}
