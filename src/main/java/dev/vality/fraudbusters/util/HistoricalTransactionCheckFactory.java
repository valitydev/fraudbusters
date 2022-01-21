package dev.vality.fraudbusters.util;

import dev.vality.damsel.fraudbusters.HistoricalTransactionCheck;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.fraudbusters.converter.CheckedResultModelToCheckResultConverter;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class HistoricalTransactionCheckFactory {

    private final CheckedResultModelToCheckResultConverter checkResultConverter;

    public HistoricalTransactionCheck createHistoricalTransactionCheck(
            Payment payment,
            CheckedResultModel checkedResultModel
    ) {
        return new HistoricalTransactionCheck()
                .setTransaction(payment)
                .setCheckResult(checkResultConverter.convert(checkedResultModel));
    }

}
