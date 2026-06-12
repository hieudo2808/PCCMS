package com.astral.express.pccms.report.controller;

import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.report.dto.response.ReportSummaryResponse;
import com.astral.express.pccms.report.entity.ReportType;
import com.astral.express.pccms.report.service.ReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/reports")
@RequiredArgsConstructor
public class ReportSummaryController {

    private final ReportSummaryService reportSummaryService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<ReportSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) ServiceCategory categoryCode,
            @RequestParam(required = false) UUID serviceId) {
        return ApiResponse.success(reportSummaryService.getSummary(
                fromDate,
                toDate,
                reportType,
                categoryCode,
                serviceId
        ));
    }
}

