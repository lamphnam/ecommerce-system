package com.techlab.ecommerce.inventory.dto.response;

import com.techlab.ecommerce.inventory.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from the synchronous stock reservation endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long orderId;
    private boolean success;
    private String failureReason;
    private List<ReservationItem> reservations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationItem {
        private Long reservationId;
        private Long productId;
        private Integer quantity;
        private ReservationStatus status;
    }
}
