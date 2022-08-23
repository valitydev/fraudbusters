package dev.vality.fraudbusters.util;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.fraudbusters.ClientInfo;
import dev.vality.damsel.fraudbusters.*;
import dev.vality.damsel.proxy_inspector.Invoice;
import dev.vality.damsel.proxy_inspector.InvoicePayment;
import dev.vality.damsel.proxy_inspector.Party;
import dev.vality.damsel.proxy_inspector.Shop;
import dev.vality.damsel.proxy_inspector.*;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.domain.TimeProperties;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.geck.common.util.TypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("VariableDeclarationUsageDistance")
@Slf4j
public class BeanUtil {

    public static final String FINGERPRINT = "fingerprint";
    public static final String SHOP_ID = "shopId";
    public static final String PARTY_ID = "partyId";
    public static final String IP = "ip";
    public static final String EMAIL = "email";
    public static final String BIN = "666";
    public static final String SUFIX = "_2";
    public static final Long AMOUNT_SECOND = 1000L;
    public static final Long AMOUNT_FIRST = 10500L;
    public static final String P_ID = "pId";
    public static final String ID_VALUE_SHOP = "2035728";
    public static final String BIN_COUNTRY_CODE = "RUS";

    public static final String SOURCE_NS = "source_ns";

    public static final String PAYMENT_ID = "1";
    public static final String TEST_MAIL_RU = "test@mail.ru";
    public static final String IDENTITY_ID = "identityId";
    public static final String RUB = "RUB";
    public static final String TOKEN = "wewerwer";
    public static final String ACCOUNT_ID = "ACCOUNT_ID";

    public static Context createContext() {
        return createContext(P_ID);
    }

    public static Context createContext(String paymentId) {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmail(EMAIL);
        ShopLocation location = new ShopLocation();
        location.setUrl("http://www.pizza-sushi.com/");
        PaymentInfo payment = new PaymentInfo(
                new Shop(
                        ID_VALUE_SHOP,
                        new Category("pizza", "no category"),
                        new ShopDetails("pizza-sushi"),
                        location
                ),
                new InvoicePayment(
                        paymentId,
                        TypeUtil.temporalToString(Instant.now()),
                        Payer.customer(
                                new CustomerPayer("custId", "1", "rec_paym_tool", createPaymentTool(),
                                        contactInfo
                                )),
                        new Cash(
                                9000L,
                                new CurrencyRef("RUB")
                        )
                ),
                new Invoice(
                        "iId",
                        TypeUtil.temporalToString(Instant.now()),
                        "",
                        new InvoiceDetails("drugs guns murder")
                ),
                new Party(paymentId)
        );

        return new Context(payment);
    }

    public static PaymentTool createPaymentTool() {
        PaymentTool paymentTool = new PaymentTool();
        paymentTool.setBankCard(createBankCard());
        return paymentTool;
    }

    @NotNull
    private static BankCard createBankCard() {
        BankCard value = new BankCard(
                "477bba133c182267fe5f086924abdc5db71f77bfc27f01f2843f2cdc69d89f05",
                BIN,
                "4242"
        );
        value.setIssuerCountry(CountryCode.RUS)
                .setPaymentSystem(new PaymentSystemRef("mastercard"));
        return value;
    }

    public static PaymentModel createPaymentModel() {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setFingerprint(FINGERPRINT);
        paymentModel.setShopId(SHOP_ID);
        paymentModel.setPartyId(PARTY_ID);
        paymentModel.setIp(IP);
        paymentModel.setEmail(EMAIL);
        paymentModel.setBin(BIN);
        paymentModel.setAmount(AMOUNT_FIRST);
        paymentModel.setBinCountryCode(BIN_COUNTRY_CODE);
        paymentModel.setCardToken(TOKEN);
        return paymentModel;
    }

    public static PaymentModel createFraudModelSecond() {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setFingerprint(FINGERPRINT + SUFIX);
        paymentModel.setShopId(SHOP_ID + SUFIX);
        paymentModel.setPartyId(PARTY_ID + SUFIX);
        paymentModel.setIp(IP + SUFIX);
        paymentModel.setEmail(EMAIL + SUFIX);
        paymentModel.setBin(BIN + SUFIX);
        paymentModel.setAmount(AMOUNT_SECOND);
        paymentModel.setBinCountryCode(BIN_COUNTRY_CODE);
        return paymentModel;
    }

    public Command crateCommandTemplate(String localId, String templateString) {
        Command command = new Command();
        Template template = new Template();
        template.setId(localId);
        template.setTemplate(templateString.getBytes());
        command.setCommandBody(CommandBody.template(template));
        command.setCommandType(dev.vality.damsel.fraudbusters.CommandType.CREATE);
        command.setCommandTime(LocalDateTime.now().toString());
        return command;
    }

    public static Command crateCommandTemplateReference(TemplateReference value) {
        Command command = new Command();
        command.setCommandType(CommandType.CREATE);
        command.setCommandBody(CommandBody.reference(value));
        command.setCommandTime(LocalDateTime.now().toString());
        return command;
    }

    @NotNull
    public static Command createGroupCommand(String localId, List<PriorityId> priorityIds) {
        Command command = new Command();
        Group group = new Group();
        group.setGroupId(localId);
        group.setTemplateIds(priorityIds);
        command.setCommandBody(CommandBody.group(group));
        command.setCommandType(dev.vality.damsel.fraudbusters.CommandType.CREATE);
        command.setCommandTime(LocalDateTime.now().toString());
        return command;
    }

    @NotNull
    public static Command deleteGroupCommand(String localId, List<PriorityId> priorityIds) {
        Command command = new Command();
        Group group = new Group();
        group.setGroupId(localId);
        group.setTemplateIds(priorityIds);
        command.setCommandBody(CommandBody.group(group));
        command.setCommandType(CommandType.DELETE);
        return command;
    }

    @NotNull
    public static Command createGroupReferenceCommand(String party, String shopId, String idGroup) {
        Command command = new Command();
        command.setCommandType(CommandType.CREATE);
        command.setCommandBody(CommandBody.group_reference(new GroupReference()
                .setGroupId(idGroup)
                .setPartyId(party)
                .setShopId(shopId)));
        command.setCommandTime(LocalDateTime.now().toString());
        return command;
    }

    @NotNull
    public static Command createDeleteGroupReferenceCommand(String party, String shopId, String idGroup) {
        Command command = new Command();
        command.setCommandType(CommandType.DELETE);
        command.setCommandBody(CommandBody.group_reference(new GroupReference()
                .setGroupId(idGroup)
                .setPartyId(party)
                .setShopId(shopId)));
        return command;
    }

    @NotNull
    public static Chargeback convertContextToChargeback(Context context, String status) {
        Chargeback chargeback = new Chargeback();
        chargeback.setEventTime(Instant.now().toString());
        Cash cost = context.getPayment().getPayment().getCost();
        chargeback.setCost(cost);
        chargeback.setStatus(ChargebackStatus.valueOf(status));
        Payer payer = context.getPayment().getPayment().getPayer();
        ReferenceInfo referenceInfo = new ReferenceInfo();
        referenceInfo.setMerchantInfo(new MerchantInfo()
                .setPartyId(context.getPayment().getParty().getPartyId())
                .setShopId(context.getPayment().getShop().getId()));
        chargeback.setReferenceInfo(referenceInfo);
        chargeback.setPayerType(PayerType.payment_resource);
        ClientInfo clientInfo = new ClientInfo();
        PayerFieldExtractor.getClientInfo(payer)
                .ifPresent(cf ->
                        clientInfo
                                .setFingerprint(cf.getFingerprint())
                                .setIp(cf.getIpAddress()
                                )
                );
        PayerFieldExtractor.getContactInfo(payer)
                .ifPresent(contactInfo -> clientInfo.setEmail(contactInfo.getEmail()));
        chargeback.setClientInfo(clientInfo);
        PayerFieldExtractor.getBankCard(payer)
                .ifPresent(bankCard -> {
                    PaymentTool paymentTool = new PaymentTool();
                    paymentTool.setBankCard(bankCard);
                    chargeback.setPaymentTool(paymentTool);
                });
        chargeback.setCategory(ChargebackCategory.fraud);
        log.info("Converted chargeback: {}", chargeback);
        return chargeback;
    }

    @NotNull
    public static Refund convertContextToRefund(Context context, String status) {
        Refund refund = new Refund();
        refund.setEventTime(Instant.now().toString());
        Cash cost = context.getPayment().getPayment().getCost();
        refund.setCost(cost);
        refund.setStatus(RefundStatus.valueOf(status));
        Payer payer = context.getPayment().getPayment().getPayer();
        ReferenceInfo referenceInfo = new ReferenceInfo();
        referenceInfo.setMerchantInfo(new MerchantInfo()
                .setPartyId(context.getPayment().getParty().getPartyId())
                .setShopId(context.getPayment().getShop().getId()));
        refund.setReferenceInfo(referenceInfo);
        refund.setPayerType(PayerType.payment_resource);
        ClientInfo clientInfo = new ClientInfo();
        PayerFieldExtractor.getClientInfo(payer)
                .ifPresent(cf ->
                        clientInfo
                                .setFingerprint(cf.getFingerprint())
                                .setIp(cf.getIpAddress()
                                )
                );
        PayerFieldExtractor.getContactInfo(payer)
                .ifPresent(contactInfo -> clientInfo.setEmail(contactInfo.getEmail()));
        refund.setClientInfo(clientInfo);
        PayerFieldExtractor.getBankCard(payer)
                .ifPresent(bankCard -> {
                    PaymentTool paymentTool = new PaymentTool();
                    paymentTool.setBankCard(bankCard);
                    refund.setPaymentTool(paymentTool);
                });
        log.info("Converted refund: {}", refund);
        return refund;
    }

    @NotNull
    public static CheckedPayment convertContextToPayment(Context context, String status) {
        TimeProperties timeProperties = TimestampUtil.generateTimeProperties();
        CheckedPayment payment = new CheckedPayment();
        payment.setTimestamp(timeProperties.getTimestamp());
        payment.setEventTime(timeProperties.getEventTime());
        payment.setEventTimeHour(timeProperties.getEventTimeHour());
        Cash cost = context.getPayment().getPayment().getCost();
        payment.setAmount(cost.getAmount());
        payment.setCurrency(cost.getCurrency().getSymbolicCode());
        payment.setPaymentStatus(status);
        Payer payer = context.getPayment().getPayment().getPayer();
        PayerFieldExtractor.getBankCard(payer)
                .ifPresent(bankCard -> {
                    payment.setBankCountry(bankCard.isSetIssuerCountry()
                            ? bankCard.getIssuerCountry().name()
                            : ClickhouseUtilsValue.UNKNOWN);
                    payment.setCardToken(bankCard.getToken());
                    payment.setCardCategory(bankCard.getCategory());
                });
        PayerFieldExtractor.getClientInfo(payer)
                .ifPresent(clientInfo -> {
                    payment.setFingerprint(clientInfo.getFingerprint());
                    payment.setIp(clientInfo.getIpAddress());
                });
        PayerFieldExtractor.getContactInfo(payer)
                .ifPresent(contactInfo -> {
                    payment.setEmail(contactInfo.getEmail());
                    payment.setPhone(contactInfo.getPhoneNumber());
                });
        payment.setPartyId(context.getPayment().getParty().getPartyId());
        payment.setShopId(context.getPayment().getShop().getId());
        return payment;
    }

    public static Payment createPayment(PaymentStatus status) {
        return createPayment(status, "payment_id");
    }

    public static Payment createPayment(PaymentStatus status, String id) {
        return new Payment()
                .setStatus(status)
                .setClientInfo(createEmail())
                .setCost(createCash())
                .setEventTime(String.valueOf(LocalDateTime.now()))
                .setId(id)
                .setPaymentTool(createBankCardResult())
                .setProviderInfo(createProviderInfo())
                .setReferenceInfo(createReferenceInfo());
    }

    public static ProviderInfo createProviderInfo() {
        return new ProviderInfo()
                .setProviderId("provider_id")
                .setCountry("RUS")
                .setTerminalId("terminal_id");
    }

    public static Refund createRefund(RefundStatus status) {
        return new Refund()
                .setStatus(status)
                .setPaymentId("payment_id")
                .setClientInfo(createEmail())
                .setCost(createCash())
                .setEventTime(String.valueOf(LocalDateTime.now()))
                .setId("payment_id")
                .setPaymentTool(createBankCardResult())
                .setProviderInfo(createProviderInfo())
                .setReferenceInfo(createReferenceInfo());
    }

    private static Cash createCash() {
        return new Cash()
                .setAmount(9000L)
                .setCurrency(createRubCurrency());
    }

    private static CurrencyRef createRubCurrency() {
        return new CurrencyRef().setSymbolicCode("RUB");
    }

    public static Chargeback createChargeback(ChargebackStatus status) {
        return new Chargeback()
                .setStatus(status)
                .setPaymentId("payment_id")
                .setClientInfo(createEmail())
                .setCost(createCash())
                .setEventTime(String.valueOf(LocalDateTime.now()))
                .setId("payment_id")
                .setCategory(ChargebackCategory.fraud)
                .setChargebackCode("123")
                .setPaymentTool(createBankCardResult())
                .setProviderInfo(createProviderInfo())
                .setReferenceInfo(createReferenceInfo());
    }


    public static Withdrawal createChargeback(WithdrawalStatus status) {
        final Resource resource = new Resource();
        resource.setBankCard(createBankCard());
        return new Withdrawal()
                .setStatus(status)
                .setId("id")
                .setCost(createCash())
                .setAccount(createAccount())
                .setEventTime(String.valueOf(LocalDateTime.now()))
                .setDestinationResource(resource)
                .setProviderInfo(createProviderInfo());
    }

    public static TemplateReference createTemplateReference(boolean isGlobal,
                                                            String party,
                                                            String shopId,
                                                            String idTemplate) {
        TemplateReference value = new TemplateReference();
        value.setTemplateId(idTemplate);
        value.setPartyId(party);
        value.setShopId(shopId);
        value.setIsGlobal(isGlobal);
        return value;
    }

    private static Account createAccount() {
        return new Account()
                .setCurrency(createRubCurrency())
                .setId(ACCOUNT_ID)
                .setIdentity(IDENTITY_ID);
    }

    @NotNull
    private static ReferenceInfo createReferenceInfo() {
        return ReferenceInfo.merchant_info(
                new MerchantInfo()
                        .setPartyId(P_ID)
                        .setShopId(ID_VALUE_SHOP));
    }

    @NotNull
    private static PaymentTool createBankCardResult() {
        return PaymentTool.bank_card(new BankCard()
                .setBin("1234")
                .setToken(TOKEN)
                .setLastDigits("433242")
                .setPaymentSystem(new PaymentSystemRef("visa"))
        );
    }

    private static ClientInfo createEmail() {
        return new ClientInfo()
                .setEmail(EMAIL)
                .setFingerprint("fingerprint")
                .setIp("123.123.123.123");
    }
}
