package com.servicehub.serv.dto;

import java.util.List;

public record AdminPaymentDashboardDto(
        AdminPaymentSummaryDto summary,
        List<AdminPaymentListItemDto> payments) {
}
