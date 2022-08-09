package dev.vality.fraudbusters.repository.clickhouse.setter;

import dev.vality.damsel.domain.PaymentSystemRef;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.fraudbusters.Error;
import dev.vality.damsel.fraudbusters.*;
import dev.vality.fraudbusters.constant.PaymentToolType;
import dev.vality.fraudbusters.domain.TimeProperties;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import dev.vality.fraudbusters.util.TimestampUtil;
import dev.vality.geck.common.util.TBaseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static dev.vality.fraudbusters.constant.ClickhouseUtilsValue.UNKNOWN;

@RequiredArgsConstructor
public class RefundBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    public static final String FIELDS = """
            timestamp, eventTimeHour, eventTime, id, email, ip, fingerprint, bin, maskedPan, cardToken, paymentSystem,
            paymentTool, terminal, providerId, bankCountry, partyId, shopId, amount, currency, status, errorCode,
            errorReason, paymentId, payerType, tokenProvider
            """;

    public static final String FIELDS_MARK = "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";

    private final List<Refund> batch;
    private final PaymentTypeByContextResolver paymentTypeByContextResolver;

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        Refund event = batch.get(i);
        int l = 1;
        TimeProperties timeProperties = TimestampUtil.generateTimePropertiesByString(event.getEventTime());
        ps.setObject(l++, timeProperties.getTimestamp());
        ps.setLong(l++, timeProperties.getEventTimeHour());
        ps.setLong(l++, timeProperties.getEventTime());

        ps.setString(l++, event.getId());

        ClientInfo clientInfo = event.getClientInfo();
        ps.setString(l++, clientInfo.getEmail());
        ps.setString(l++, clientInfo.getIp());
        ps.setString(l++, clientInfo.getFingerprint());

        PaymentTool paymentTool = event.getPaymentTool();
        ps.setString(l++, paymentTool.isSetBankCard() ? paymentTool.getBankCard().getBin() : UNKNOWN);
        ps.setString(l++, paymentTool.isSetBankCard() ? paymentTool.getBankCard().getLastDigits() : UNKNOWN);
        ps.setString(l++, paymentTool.isSetBankCard() ? paymentTool.getBankCard().getToken() : UNKNOWN);
        ps.setString(l++, paymentTool.isSetBankCard()
                ? Optional.ofNullable(paymentTool.getBankCard().getPaymentSystem())
                .map(PaymentSystemRef::getId).orElse(null)
                : UNKNOWN);
        ps.setString(l++, TBaseUtil.unionFieldToEnum(paymentTool, PaymentToolType.class).name());

        ProviderInfo providerInfo = event.getProviderInfo();
        ps.setString(
                l++,
                providerInfo != null && providerInfo.isSetTerminalId() ? providerInfo.getTerminalId() : UNKNOWN
        );
        ps.setString(
                l++,
                providerInfo != null && providerInfo.isSetProviderId() ? providerInfo.getProviderId() : UNKNOWN
        );
        ps.setString(l++, providerInfo != null && providerInfo.isSetCountry() ? providerInfo.getCountry() : UNKNOWN);

        ReferenceInfo referenceInfo = event.getReferenceInfo();
        MerchantInfo merchantInfo = event.getReferenceInfo().getMerchantInfo();
        ps.setString(l++, referenceInfo.isSetMerchantInfo() ? merchantInfo.getPartyId() : UNKNOWN);
        ps.setString(l++, referenceInfo.isSetMerchantInfo() ? merchantInfo.getShopId() : UNKNOWN);

        ps.setLong(l++, event.getCost().getAmount());
        ps.setString(l++, event.getCost().getCurrency().getSymbolicCode());

        ps.setObject(l++, event.getStatus());

        Error error = event.getError();
        ps.setString(l++, error == null ? null : error.getErrorCode());
        ps.setString(l++, error == null ? null : error.getErrorReason());
        ps.setString(l++, event.getPaymentId());

        ps.setString(l++, event.isSetPayerType() ? event.getPayerType().name() : UNKNOWN);
        ps.setString(l, paymentTool.isSetBankCard() && paymentTypeByContextResolver.isMobile(paymentTool.getBankCard())
                ? paymentTool.getBankCard().getPaymentToken().getId()
                : UNKNOWN
        );
    }

    @Override
    public int getBatchSize() {
        return batch.size();
    }
}
