import type { ElementType } from "react";

interface EmptyStateProps {
    icon: ElementType;
    title: string;
    description?: string;
    action?: React.ReactNode;
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
    return (
        <div className="flex flex-col items-center justify-center p-8 text-center bg-white rounded-lg border border-gray-100 border-dashed">
            <div className="flex items-center justify-center w-12 h-12 mb-4 bg-gray-100 rounded-full">
                <Icon className="w-6 h-6 text-gray-400" />
            </div>
            <h3 className="mb-1 text-sm font-medium text-gray-900">{title}</h3>
            {description && <p className="mb-4 text-sm text-gray-500">{description}</p>}
            {action && <div>{action}</div>}
        </div>
    );
}
