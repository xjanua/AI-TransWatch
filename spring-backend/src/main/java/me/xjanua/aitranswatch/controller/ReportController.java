package me.xjanua.aitranswatch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.report.ReportOverviewResponse;
import me.xjanua.aitranswatch.service.ReportService;

/** API số liệu tổng hợp cho tab báo cáo của dashboard. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public RestResponse<ReportOverviewResponse> overview() {
        return RestResponse.success(reportService.overview());
    }
}
