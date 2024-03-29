package dev.vality.fraudbusters.converter;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import dev.vality.fraudbusters.domain.*;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudResultToEventConverter implements BatchConverter<FraudResult, Event> {

    private final ColumbusServiceSrv.Iface geoIpService;

    @Override
    public Event convert(FraudResult fraudResult) {
        Event event = new Event();
        PaymentModel paymentModel = fraudResult.getFraudRequest().getFraudModel();
        event.setAmount(paymentModel.getAmount());
        event.setBin(paymentModel.getBin());
        event.setEmail(paymentModel.getEmail());

        TimeProperties timeProperties = TimestampUtil.generateTimeProperties();
        event.setTimestamp(timeProperties.getTimestamp());
        event.setEventTime(timeProperties.getEventTime());
        event.setEventTimeHour(timeProperties.getEventTimeHour());

        event.setFingerprint(paymentModel.getFingerprint());
        String ip = paymentModel.getIp();
        String country = getCountryCode(ip);
        event.setCountry(country);
        event.setIp(ip);
        event.setPartyId(paymentModel.getPartyId());
        CheckedResultModel resultModel = fraudResult.getResultModel();
        event.setCheckedTemplate(resultModel.getCheckedTemplate());
        Optional.ofNullable(resultModel.getResultModel())
                .ifPresent(result -> {
                    event.setCheckedRule(result.getRuleChecked());
                    event.setResultStatus(result.getResultStatus().name());
                });

        event.setShopId(paymentModel.getShopId());
        event.setBankCountry(paymentModel.getBinCountryCode());
        event.setCardToken(paymentModel.getCardToken());
        Metadata metadata = fraudResult.getFraudRequest().getMetadata();
        if (metadata != null) {
            event.setBankName(metadata.getBankName());
            event.setCurrency(metadata.getCurrency());
            event.setInvoiceId(metadata.getInvoiceId());
            event.setLastDigits(metadata.getLastDigits());
            event.setPaymentId(metadata.getPaymentId());
            event.setPayerType(metadata.getPayerType());
            event.setTokenProvider(metadata.getTokenProvider());
        }
        return event;
    }

    private String getCountryCode(String ip) {
        String country = null;
        try {
            country = geoIpService.getLocationIsoCode(ip);
        } catch (TException e) {
            log.error("Error when getCountryCode e: ", e);
        }
        return country != null ? country : ClickhouseUtilsValue.UNKNOWN;
    }
}
