package com.techlab.ecommerce.payment.service;

import com.techlab.ecommerce.payment.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentQueryService {

    PaymentResponse getPayment(Long id);

    Page<PaymentResponse> listPayments(Long orderId, Pageable pageable);
}
