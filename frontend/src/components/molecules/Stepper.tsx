import { Check } from "lucide-react";
import { cx } from "~/utils/cx";

interface Step {
    label: string;
}

interface StepperProps {
    steps: Step[];
    currentStep: number;
    className?: string;
}

export function Stepper({ steps, currentStep, className }: StepperProps) {
    return (
        <div className={cx("w-full", className)}>
            <div className="flex items-center justify-between">
                {steps.map((step, index) => {
                    const status =
                        index < currentStep
                            ? "completed"
                            : index === currentStep
                              ? "current"
                              : "upcoming";

                    return (
                        <div
                            key={step.label}
                            className="flex flex-1 flex-col items-center relative"
                        >
                            {/* Line connecting steps */}
                            {index !== steps.length - 1 && (
                                <div
                                    className={cx(
                                        "absolute top-4 left-1/2 w-full h-0.5",
                                        status === "completed" ? "bg-primary-600" : "bg-slate-200"
                                    )}
                                />
                            )}

                            {/* Circle Indicator */}
                            <div
                                className={cx(
                                    "relative z-10 flex h-8 w-8 items-center justify-center rounded-full border-2 bg-white transition-colors duration-300",
                                    status === "completed"
                                        ? "border-primary-600 bg-primary-600 text-white"
                                        : status === "current"
                                          ? "border-primary-600 text-primary-600"
                                          : "border-slate-300 text-slate-400"
                                )}
                            >
                                {status === "completed" ? (
                                    <Check className="h-4 w-4" />
                                ) : (
                                    <span className="text-[13px] font-semibold">{index + 1}</span>
                                )}
                            </div>

                            {/* Label */}
                            <span
                                className={cx(
                                    "mt-2 text-center text-[12px] font-medium transition-colors",
                                    status === "current"
                                        ? "text-primary-700"
                                        : status === "completed"
                                          ? "text-text-main"
                                          : "text-slate-400"
                                )}
                            >
                                {step.label}
                            </span>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
