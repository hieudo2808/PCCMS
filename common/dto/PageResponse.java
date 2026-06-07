package com.astral.express.pccms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        boolean success,
        int code,
        String message,
        PageData<T> data
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        PageData<T> pageData = new PageData<>(
                page.getContent(),
                page.getNumber() + 1, // Page trong Spring Data bắt đầu từ 0
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
        return new PageResponse<>(true, 200, "Thành công", pageData);
    }

    public record PageData<T>(
            List<T> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean isFirst,
            boolean isLast
    ) {
    }
}
