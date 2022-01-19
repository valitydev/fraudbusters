package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.service.dto.*;

public interface HistoricalDataService {

    HistoricalPaymentsDto getPayments(FilterDto filter);

    HistoricalRefundsDto getRefunds(FilterDto filter);

    HistoricalChargebacksDto getChargebacks(FilterDto filter);

    HistoricalFraudResultsDto getFraudResults(FilterDto filter);

    HistoricalPaymentsDto getFraudPayments(FilterDto filter);

}
