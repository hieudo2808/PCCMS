import { EmptyState } from "~/components/molecules";

interface ReportEmptyStateProps {
    message?: string;
}

export function ReportEmptyState({ message = "Không có dữ liệu thống kê trong khoảng thời gian được chọn" }: ReportEmptyStateProps) {
    return (
        <EmptyState
            title={message}
            description="Hãy thay đổi tiêu chí hoặc khoảng thời gian để xem dữ liệu thống kê khác."
        />
    );
}
