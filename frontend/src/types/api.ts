export interface ApiResponse<T> {
    success: boolean;
    code: number;
    message: string;
    data: T;
    errorCode?: string;
    errors?: string[] | Record<string, string>;
}

export interface PageResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    isLast: boolean;
}
