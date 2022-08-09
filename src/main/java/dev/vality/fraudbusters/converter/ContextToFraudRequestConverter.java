package dev.vality.fraudbusters.converter;

import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.Payer;
import dev.vality.damsel.proxy_inspector.Context;
import dev.vality.damsel.proxy_inspector.Party;
import dev.vality.damsel.proxy_inspector.PaymentInfo;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import dev.vality.fraudbusters.domain.FraudRequest;
import dev.vality.fraudbusters.domain.Metadata;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.util.PayerFieldExtractor;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import dev.vality.geck.common.util.TypeUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class ContextToFraudRequestConverter implements Converter<Context, FraudRequest> {

    private final PaymentTypeByContextResolver paymentTypeByContextResolver;

    @Override
    public FraudRequest convert(Context context) {
        PaymentModel paymentModel = new PaymentModel();
        PaymentInfo payment = context.getPayment();
        Party party = payment.getParty();
        paymentModel.setPartyId(party.getPartyId());
        Payer payer = context.getPayment().getPayment().getPayer();

        PayerFieldExtractor.getBankCard(payer)
                .ifPresent(bankCard -> {
                    paymentModel.setBin(bankCard.getBin());
                    paymentModel.setBinCountryCode(bankCard.isSetIssuerCountry()
                            ? bankCard.getIssuerCountry().name()
                            : ClickhouseUtilsValue.UNKNOWN);
                    paymentModel.setCardToken(bankCard.getToken());
                    paymentModel.setRecurrent(
                            paymentTypeByContextResolver.isRecurrent(context.getPayment().getPayment().getPayer())
                    );
                    paymentModel.setMobile(paymentTypeByContextResolver.isMobile(bankCard));
                });

        PayerFieldExtractor.getContactInfo(payer)
                .ifPresent(contract ->
                        paymentModel.setEmail(contract.getEmail())
                );

        paymentModel.setShopId(payment.getShop().getId());
        Cash cost = payment.getPayment().getCost();
        paymentModel.setAmount(cost.getAmount());
        paymentModel.setCurrency(cost.getCurrency().symbolic_code);

        PayerFieldExtractor.getClientInfo(payer).ifPresent(info -> {
            paymentModel.setIp(info.getIpAddress());
            paymentModel.setFingerprint(info.getFingerprint());
        });
        FraudRequest fraudRequest = new FraudRequest();
        fraudRequest.setFraudModel(paymentModel);
        Metadata metadata = initMetadata(context);
        fraudRequest.setMetadata(metadata);
        return fraudRequest;
    }

    @NotNull
    private Metadata initMetadata(Context context) {
        Metadata metadata = new Metadata();
        PaymentInfo payment = context.getPayment();
        LocalDateTime localDateTime = TypeUtil.stringToLocalDateTime(payment.getPayment().getCreatedAt());
        metadata.setTimestamp(localDateTime.toEpochSecond(ZoneOffset.UTC));
        metadata.setCurrency(payment.getPayment().getCost().getCurrency().symbolic_code);
        metadata.setInvoiceId(payment.getInvoice().getId());
        metadata.setPaymentId(payment.getPayment().getId());
        PayerFieldExtractor.getBankCard(context.getPayment().getPayment().getPayer())
                .ifPresent(bankCard -> {
                    metadata.setMaskedPan(bankCard.getLastDigits());
                    metadata.setBankName(bankCard.getBankName());
                    metadata.setPayerType(
                            PayerFieldExtractor.getPayerType(context.getPayment().getPayment().getPayer()));
                    metadata.setTokenProvider(paymentTypeByContextResolver.isMobile(bankCard)
                            ? bankCard.getPaymentToken().getId()
                            : ClickhouseUtilsValue.UNKNOWN);
                });
        return metadata;
    }

}
