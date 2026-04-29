package com.techlab.ecommerce.payment.service.impl;

import com.techlab.ecommerce.payment.dto.response.PaymentResponse;
import com.techlab.ecommerce.payment.enums.PaymentErrorMessage;
import com.techlab.ecommerce.payment.exception.PaymentException;
import com.techlab.ecommerce.payment.mapper.PaymentMapper;
import com.techlab.ecommerce.payment.repository.PaymentRepository;
import com.techlab.ecommerce.payment.service.PaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        return paymentRepository.findWithAttemptsById(id)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new PaymentException(PaymentErrorMessage.PAYMENT_NOT_FOUND,
                        "Payment " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(Long orderId, Pageable pageable) {
        Page<com.techlab.ecommerce.payment.entity.Payment> page = orderId == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findByOrderId(orderId, pageable);
        return page.map(paymentMapper::toDto);
    }
}
