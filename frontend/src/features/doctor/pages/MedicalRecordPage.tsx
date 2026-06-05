import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, FormProvider } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Tag } from "~/components/atoms";
import { EmptyState } from "~/components/molecules";
import { VitalSignsForm, vitalSignsSchema } from '../components/VitalSignsForm';
import { PrescriptionTable, prescriptionFormSchema } from '../components/PrescriptionTable';
import { medicalRecordApi } from '~/shared/api/medicalRecordApi';
import { PetProfileSummary } from '~/shared/components/pet';
import { toast } from 'react-hot-toast';
import { useEffect } from 'react';

const fullRecordSchema = z.object({
  vitalSigns: vitalSignsSchema,
  prescription: prescriptionFormSchema,
});

type FullRecordFormValues = z.infer<typeof fullRecordSchema>;

export function MedicalRecordPage() {
    const { id } = useParams<{ id: string }>();
    const queryClient = useQueryClient();

    const { data: record, isLoading, isError } = useQuery({
        queryKey: ['medicalRecord', id],
        queryFn: () => medicalRecordApi.getMedicalRecordById(id as string),
        enabled: !!id,
    });

    const isFinalized = record?.recordStatus === 'FINALIZED';

    const methods = useForm<FullRecordFormValues>({
        resolver: zodResolver(fullRecordSchema) as any,
        defaultValues: {
            vitalSigns: {},
            prescription: { items: [] },
        }
    });

    useEffect(() => {
        if (record) {
            methods.reset({
                vitalSigns: {
                    temperatureC: record.temperatureC,
                    heartRateBpm: record.heartRateBpm,
                    respiratoryRateBpm: record.respiratoryRateBpm,
                    weightKg: record.weightKg,
                    bloodPressure: record.bloodPressure,
                    spo2Percent: record.spo2Percent,
                    mucousMembraneColor: record.mucousMembraneColor,
                    capillaryRefillSeconds: record.capillaryRefillSeconds,
                    preliminaryDiagnosis: record.preliminaryDiagnosis,
                    finalDiagnosis: record.finalDiagnosis,
                },
                prescription: { items: [] } // In a real app we'd fetch prescription items too
            });
        }
    }, [record, methods]);

    const saveDraftMutation = useMutation({
        mutationFn: async (data: FullRecordFormValues) => {
            if (!id) throw new Error("Missing ID");
            await medicalRecordApi.updateMedicalRecord(id, data.vitalSigns as any);
            if (data.prescription.items.length > 0) {
                await medicalRecordApi.createPrescription(id, { items: data.prescription.items });
            }
        },
        onSuccess: () => {
            toast.success("Đã lưu nháp thành công");
            queryClient.invalidateQueries({ queryKey: ['medicalRecord', id] });
        }
    });

    const finalizeMutation = useMutation({
        mutationFn: async (data: FullRecordFormValues) => {
            if (!id) throw new Error("Missing ID");
            await medicalRecordApi.finalizeMedicalRecord(id, {
                finalDiagnosis: data.vitalSigns.finalDiagnosis || '',
            });
            if (data.prescription.items.length > 0) {
                await medicalRecordApi.createPrescription(id, { items: data.prescription.items });
            }
        },
        onSuccess: () => {
            toast.success("Đã chốt bệnh án thành công");
            queryClient.invalidateQueries({ queryKey: ['medicalRecord', id] });
        }
    });

    if (isLoading) {
        return (
            <div className="flex justify-center p-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    if (isError || !record) {
        return <EmptyState title="Lỗi" description="Không thể tải bệnh án" />;
    }

    return (
        <FormProvider {...methods}>
            <div className="mb-4 flex justify-between items-center">
                <h2 className="text-xl font-bold">Chi tiết bệnh án: {record.recordCode}</h2>
                {isFinalized && <Tag tone="green">Đã chốt</Tag>}
            </div>
            <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
                <VitalSignsForm 
                    disabled={isFinalized}
                    initialData={methods.getValues().vitalSigns}
                    onSaveDraft={(data) => {
                        methods.setValue('vitalSigns', data);
                        saveDraftMutation.mutate(methods.getValues());
                    }}
                    onFinalize={(data) => {
                        methods.setValue('vitalSigns', data);
                        finalizeMutation.mutate(methods.getValues());
                    }}
                    isSaving={saveDraftMutation.isPending}
                    isFinalizing={finalizeMutation.isPending}
                />

                <div className="space-y-6">
                    {record.petId && (
                        <PetProfileSummary petId={record.petId} showClinicalNotes />
                    )}
                    <PrescriptionTable disabled={isFinalized} />
                </div>
            </div>
        </FormProvider>
    );
}

