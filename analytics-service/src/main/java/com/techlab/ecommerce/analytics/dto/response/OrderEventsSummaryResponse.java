package com.techlab.ecommerce.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventsSummaryResponse {

    private long orderCreatedCount;
    private long paymentSucceededCount;
    private long paymentFailedCount;
    private long inventoryFailedCount;
    private long notificationFailedCount;
}
