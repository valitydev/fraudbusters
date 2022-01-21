package dev.vality.fraudbusters.service;

import dev.vality.damsel.fraudbusters.Chargeback;
import dev.vality.damsel.fraudbusters.Refund;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.domain.Event;
import dev.vality.fraudbusters.domain.FraudPaymentRow;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.service.dto.*;
import dev.vality.fraudbusters.util.CompositeIdUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricalDataServiceImpl implements HistoricalDataService {

    private final Repository<CheckedPayment> paymentRepository;
    private final Repository<Refund> refundRepository;
    private final Repository<Chargeback> chargebackRepository;
    private final Repository<Event> fraudResultRepository;
    private final Repository<FraudPaymentRow> fraudPaymentRepository;

    @Override
    public HistoricalPaymentsDto getPayments(FilterDto filter) {
        List<CheckedPayment> payments = paymentRepository.getByFilter(filter);
        String lastId = buildLastPaymentId(filter.getSize(), payments);
        return HistoricalPaymentsDto.builder()
                .payments(payments)
                .lastId(lastId)
                .build();
    }

    @Override
    public HistoricalRefundsDto getRefunds(FilterDto filter) {
        List<Refund> refunds = refundRepository.getByFilter(filter);
        String lastId = buildLastRefundId(filter.getSize(), refunds);
        return HistoricalRefundsDto.builder()
                .refunds(refunds)
                .lastId(lastId)
                .build();
    }

    @Override
    public HistoricalChargebacksDto getChargebacks(FilterDto filter) {
        List<Chargeback> chargebacks = chargebackRepository.getByFilter(filter);
        String lastId = buildLastChargebackId(filter.getSize(), chargebacks);
        return HistoricalChargebacksDto.builder()
                .chargebacks(chargebacks)
                .lastId(lastId)
                .build();
    }

    @Override
    public HistoricalFraudResultsDto getFraudResults(FilterDto filter) {
        List<Event> fraudResults = fraudResultRepository.getByFilter(filter);
        String lastId = filter.getSize() == fraudResults.size()
                ? fraudResults.get(fraudResults.size() - 1).getPaymentId()
                : null;
        return HistoricalFraudResultsDto.builder()
                .fraudResults(fraudResults)
                .lastId(lastId)
                .build();
    }

    @Override
    public HistoricalPaymentsDto getFraudPayments(FilterDto filter) {
        List<FraudPaymentRow> payments = fraudPaymentRepository.getByFilter(filter);
        String lastId = buildLastPaymentId(filter.getSize(), payments);
        return HistoricalPaymentsDto.builder()
                .payments(payments)
                .lastId(lastId)
                .build();
    }

    @Nullable
    private String buildLastPaymentId(Long filterSize, List<? extends CheckedPayment> payments) {
        if (payments.size() == filterSize) {
            CheckedPayment lastPayment = payments.get(payments.size() - 1);
            return CompositeIdUtil.create(lastPayment.getId(), lastPayment.getPaymentStatus());
        }
        return null;
    }

    @Nullable
    private String buildLastRefundId(Long filterSize, List<Refund> refunds) { // TODO перейти на внутреннюю модель
        if (refunds.size() == filterSize) {
            Refund lastRefund = refunds.get(refunds.size() - 1);
            return CompositeIdUtil.create(lastRefund.getId(), lastRefund.getStatus().name());
        }
        return null;
    }

    @Nullable
    private String buildLastChargebackId(Long filterSize,
                                         List<Chargeback> chargebacks) { // TODO перейти на внутреннюю модель
        if (chargebacks.size() == filterSize) {
            Chargeback lastChargeback = chargebacks.get(chargebacks.size() - 1);
            return CompositeIdUtil.create(lastChargeback.getId(), lastChargeback.getStatus().name());
        }
        return null;
    }
}
