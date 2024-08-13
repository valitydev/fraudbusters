package dev.vality.fraudbusters;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.domain.RiskScore;
import dev.vality.damsel.fraudbusters.*;
import dev.vality.damsel.proxy_inspector.Context;
import dev.vality.damsel.proxy_inspector.InspectorProxySrv;
import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.OtelConfig;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.KafkaTopics;
import dev.vality.fraudbusters.config.properties.OtelProperties;
import dev.vality.fraudbusters.constants.EndToEndIntegrationTemplates;
import dev.vality.fraudbusters.constants.TestProperties;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.repository.clickhouse.impl.ChargebackRepository;
import dev.vality.fraudbusters.repository.clickhouse.impl.PaymentRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.RefundRepository;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.fraudbusters.util.ReferenceKeyGenerator;
import dev.vality.testcontainers.annotations.KafkaSpringBootTest;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.trusted.tokens.ConditionTemplate;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import dev.vality.woody.thrift.impl.http.THClientBuilder;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static dev.vality.fraudbusters.factory.TestObjectsFactory.createCommandTemplate;
import static dev.vality.fraudbusters.util.BeanUtil.crateCommandTemplateReference;
import static dev.vality.fraudbusters.util.BeanUtil.createTemplateReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@ActiveProfiles("full-prod")
@KafkaSpringBootTest
@KafkaTestcontainer(
        topicsKeys = {
                "kafka.topic.template",
                "kafka.topic.group-list",
                "kafka.topic.reference",
                "kafka.topic.group-reference",
                "kafka.topic.full-template"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = {FraudBustersApplication.class, TestClickhouseConfig.class},
        properties = {
                "kafka.listen.result.concurrency=1",
                "kafka.historical.listener.enable=true",
                "spring.main.allow-bean-definition-overriding=true"
        })
@ExtendWith({ClickHouseContainerExtension.class})
@Import({MockExternalServiceConfig.class})
class EndToEndIntegrationTest {

    public static final String CAPTURED = "captured";
    public static final String PROCESSED = "processed";

    public static final String PENDING = "pending";
    public static final String FAILED = "failed";

    private static final String P_ID = "test";
    private static final String GROUP_P_ID = "group_1";

    private static final String GLOBAL_REF = UUID.randomUUID().toString();
    private static final String PARTY_TEMPLATE = UUID.randomUUID().toString();
    private static final String SHOP_REF = UUID.randomUUID().toString();
    private static final String GROUP_TEMPLATE_DECLINE = UUID.randomUUID().toString();
    private static final String GROUP_TEMPLATE_NORMAL = UUID.randomUUID().toString();
    private static final String GROUP_ID = UUID.randomUUID().toString();

    private static final String FRAUD_INSPECTOR_SERVICE_URL = "http://localhost:%s/fraud_inspector/v1";
    private static final String HISTORICAL_SERVICE_URL = "http://localhost:%s/historical_data/v1/";

    @Autowired
    ColumbusServiceSrv.Iface geoIpServiceSrv;
    @Autowired
    TrustedTokensSrv.Iface trustedTokensSrv;

    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;
    @Autowired
    protected KafkaTopics kafkaTopics;
    @Autowired
    PaymentRepositoryImpl paymentRepository;
    @Autowired
    ChargebackRepository chargebackRepository;
    @Autowired
    RefundRepository refundRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    HistoricalPool<ParserRuleContext> timeTemplatePoolImpl;
    @Autowired
    private HistoricalPool<List<String>> timeGroupPoolImpl;
    @Autowired
    private HistoricalPool<String> timeReferencePoolImpl;
    @Autowired
    private HistoricalPool<String> timeGroupReferencePoolImpl;

    @LocalServerPort
    int serverPort;

    @BeforeEach
    public void init() throws InterruptedException, TException {
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(),
                createCommandTemplate(GLOBAL_REF, EndToEndIntegrationTemplates.TEMPLATE));
        testThriftKafkaProducer.send(kafkaTopics.getFullReference(),
                crateCommandTemplateReference(createTemplateReference(true, null, null, GLOBAL_REF)));

        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(),
                createCommandTemplate(PARTY_TEMPLATE, EndToEndIntegrationTemplates.TEMPLATE_CONCRETE));
        TemplateReference templateReference = createTemplateReference(false, P_ID, null, PARTY_TEMPLATE);
        testThriftKafkaProducer.send(
                kafkaTopics.getFullReference(),
                ReferenceKeyGenerator.generateTemplateKey(templateReference),
                crateCommandTemplateReference(templateReference));

        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(),
                createCommandTemplate(SHOP_REF, EndToEndIntegrationTemplates.TEMPLATE_CONCRETE_SHOP));
        templateReference = createTemplateReference(false, P_ID, BeanUtil.ID_VALUE_SHOP, SHOP_REF);
        testThriftKafkaProducer.send(
                kafkaTopics.getFullReference(),
                ReferenceKeyGenerator.generateTemplateKey(templateReference),
                crateCommandTemplateReference(templateReference));

        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(),
                createCommandTemplate(GROUP_TEMPLATE_DECLINE, EndToEndIntegrationTemplates.GROUP_DECLINE));
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(),
                createCommandTemplate(GROUP_TEMPLATE_NORMAL, EndToEndIntegrationTemplates.GROUP_NORMAL));

        Command groupCommand = BeanUtil.createGroupCommand(GROUP_ID, List.of(new PriorityId()
                .setId(GROUP_TEMPLATE_DECLINE)
                .setPriority(2L), new PriorityId()
                .setId(GROUP_TEMPLATE_NORMAL)
                .setPriority(1L)));
        testThriftKafkaProducer.send(kafkaTopics.getFullGroupList(),
                GROUP_ID,
                groupCommand);

        testThriftKafkaProducer.send(kafkaTopics.getFullGroupReference(),
                ReferenceKeyGenerator.generateTemplateKeyByList(GROUP_P_ID, null),
                BeanUtil.createGroupReferenceCommand(GROUP_P_ID, null, GROUP_ID));
        Mockito.when(geoIpServiceSrv.getLocationIsoCode(any())).thenReturn("RUS");
        Mockito.when(trustedTokensSrv.isTokenTrusted(anyString(), any(ConditionTemplate.class))).thenReturn(true);

        Thread.sleep(TestProperties.TIMEOUT);
    }

    @Test
    public void test() throws URISyntaxException, TException, InterruptedException {
        testFraudRules();
        testValidation();
        testHistoricalPoolLinks();
        testApplyRuleOnHistoricalDataSets();
    }

    private void testFraudRules() throws URISyntaxException, InterruptedException, TException {
        THClientBuilder clientBuilder = new THClientBuilder()
                .withAddress(new URI(String.format(FRAUD_INSPECTOR_SERVICE_URL, serverPort)))
                .withNetworkTimeout(300000);
        InspectorProxySrv.Iface client = clientBuilder.build(InspectorProxySrv.Iface.class);

        Context context = BeanUtil.createContext();
        RiskScore riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.high, riskScore);

        paymentRepository.insertBatch(List.of(BeanUtil.convertContextToPayment(context, PENDING)));
        paymentRepository.insertBatch(List.of(BeanUtil.convertContextToPayment(context, PROCESSED)));
        paymentRepository.insertBatch(List.of(BeanUtil.convertContextToPayment(context, CAPTURED)));

        context = BeanUtil.createContext();
        riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.fatal, riskScore);

        paymentRepository.insertBatch(List.of(BeanUtil.convertContextToPayment(context, FAILED)));

        context = BeanUtil.createContext(P_ID);
        riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.low, riskScore);

        paymentRepository.insertBatch(List.of(BeanUtil.convertContextToPayment(context, PROCESSED)));
        paymentRepository.insertBatch(List.of(BeanUtil.convertContextToPayment(context, CAPTURED)));

        //test groups templates
        context = BeanUtil.createContext(GROUP_P_ID);
        riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.fatal, riskScore);

        //test chargeback functions
        String chargeTest = "charge-test";
        context = BeanUtil.createContext(chargeTest);
        context.getPayment().getShop().setId(chargeTest);
        context.getPayment().getParty().setPartyId(chargeTest);
        riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.high, riskScore);

        chargebackRepository.insertBatch(List.of(BeanUtil.convertContextToChargeback(
                context,
                ChargebackStatus.accepted.name()
        )));

        riskScore = client.inspectPayment(context);

        assertEquals(RiskScore.fatal, riskScore);

        //test refund functions
        String refundShopId = "refund-test";
        context.getPayment().getShop().setId(refundShopId);
        riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.high, riskScore);

        refundRepository.insertBatch(List.of(BeanUtil.convertContextToRefund(context, RefundStatus.failed.name())));

        riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.high, riskScore);

        refundRepository.insertBatch(List.of(BeanUtil.convertContextToRefund(context, RefundStatus.succeeded.name())));

        riskScore = client.inspectPayment(context);
        assertEquals(RiskScore.fatal, riskScore);
    }

    private void testValidation() throws URISyntaxException, TException {
        THClientBuilder clientBuilder = new THClientBuilder()
                .withAddress(new URI(String.format("http://localhost:%s/fraud_payment_validator/v1/", serverPort)))
                .withNetworkTimeout(300000);
        PaymentServiceSrv.Iface client = clientBuilder.build(PaymentServiceSrv.Iface.class);

        ValidateTemplateResponse validateTemplateResponse = client.validateCompilationTemplate(
                List.of(new Template()
                        .setId("dfsdf")
                        .setTemplate(EndToEndIntegrationTemplates.TEMPLATE.getBytes()))
        );

        assertTrue(validateTemplateResponse.getErrors().isEmpty());
    }

    private void testHistoricalPoolLinks() {
        assertEquals(5, timeTemplatePoolImpl.size());
        long timestamp = Instant.now().plus(Duration.ofDays(30)).toEpochMilli();
        assertNotNull(timeTemplatePoolImpl.get(GLOBAL_REF, timestamp));
        assertNotNull(timeTemplatePoolImpl.get(PARTY_TEMPLATE, timestamp));
        assertNotNull(timeTemplatePoolImpl.get(SHOP_REF, timestamp));
        assertNotNull(timeTemplatePoolImpl.get(GROUP_TEMPLATE_DECLINE, timestamp));
        assertNotNull(timeTemplatePoolImpl.get(GROUP_TEMPLATE_NORMAL, timestamp));

        String partyTemplateRefId = timeReferencePoolImpl.get(P_ID, timestamp);
        assertEquals(PARTY_TEMPLATE, partyTemplateRefId);
        assertNotNull(timeTemplatePoolImpl.get(partyTemplateRefId, timestamp));

        String groupRefId = timeGroupReferencePoolImpl.get(GROUP_P_ID, timestamp);
        assertEquals(GROUP_ID, groupRefId);
        List<String> groupTemplateIds = timeGroupPoolImpl.get(groupRefId, timestamp);
        for (String groupTemplateId : groupTemplateIds) {
            assertNotNull(timeTemplatePoolImpl.get(groupTemplateId, timestamp));
        }

        assertNull(timeTemplatePoolImpl.get(GLOBAL_REF, 0L));
        assertNull(timeTemplatePoolImpl.get(PARTY_TEMPLATE, 0L));
        assertNull(timeTemplatePoolImpl.get(SHOP_REF, 0L));
        assertNull(timeTemplatePoolImpl.get(GROUP_TEMPLATE_DECLINE, 0L));
        assertNull(timeTemplatePoolImpl.get(GROUP_TEMPLATE_NORMAL, 0L));
    }


    private void testApplyRuleOnHistoricalDataSets() throws URISyntaxException, TException {
        HistoricalDataServiceSrv.Iface client = new THSpawnClientBuilder()
                .withNetworkTimeout(30_000)
                .withAddress(new URI(String.format(HISTORICAL_SERVICE_URL, serverPort)))
                .build(HistoricalDataServiceSrv.Iface.class);

        testApplyRuleOnHistoricalDataSetsSingleRule(client);
        testApplyRuleOnHistoricalDataSetsWithinRuleSet(client);
    }

    private void testApplyRuleOnHistoricalDataSetsSingleRule(HistoricalDataServiceSrv.Iface client) throws TException {
        String rule = "rule: amount() < 1 -> decline;";
        Payment firstPayment = TestObjectsFactory.createPayment(10L);
        Payment secondPayment = TestObjectsFactory.createPayment(0L);
        HistoricalDataSetCheckResult result = client.applyRuleOnHistoricalDataSet(
                TestObjectsFactory.createEmulationRuleApplyRequest(
                        rule,
                        UUID.randomUUID().toString(),
                        firstPayment,
                        secondPayment)
        );
        assertEquals(2, result.getHistoricalTransactionCheck().size());
        var check = findHistoricalTransactionCheck(result.getHistoricalTransactionCheck(), firstPayment.getId());
        assertEquals(firstPayment, check.getTransaction());
        assertEquals(rule, check.getCheckResult().getCheckedTemplate());
        assertNull(check.getCheckResult().getConcreteCheckResult().getRuleChecked());
        assertEquals(new ArrayList<>(), check.getCheckResult().getConcreteCheckResult().getNotificationsRule());
        assertEquals(
                ResultStatus.normal(new Normal()),
                check.getCheckResult().getConcreteCheckResult().getResultStatus()
        );
        check = findHistoricalTransactionCheck(result.getHistoricalTransactionCheck(), secondPayment.getId());
        assertEquals(secondPayment, check.getTransaction());
        assertEquals(rule, check.getCheckResult().getCheckedTemplate());
        assertEquals("0", check.getCheckResult().getConcreteCheckResult().getRuleChecked());
        assertEquals(new ArrayList<>(), check.getCheckResult().getConcreteCheckResult().getNotificationsRule());
        assertEquals(
                ResultStatus.decline(new Decline()),
                check.getCheckResult().getConcreteCheckResult().getResultStatus()
        );

        String notifyRule = "rule: amount() < 1 -> notify;";
        result = client.applyRuleOnHistoricalDataSet(
                TestObjectsFactory.createEmulationRuleApplyRequest(
                        notifyRule,
                        UUID.randomUUID().toString(),
                        secondPayment)
        );
        check = findHistoricalTransactionCheck(result.getHistoricalTransactionCheck(), secondPayment.getId());
        assertEquals(secondPayment, check.getTransaction());
        assertEquals(notifyRule, check.getCheckResult().getCheckedTemplate());
        assertNull(check.getCheckResult().getConcreteCheckResult().getRuleChecked());
        assertEquals(List.of("0"), check.getCheckResult().getConcreteCheckResult().getNotificationsRule());
        assertEquals(
                ResultStatus.notify(new Notify()),
                check.getCheckResult().getConcreteCheckResult().getResultStatus()
        );
    }

    private void testApplyRuleOnHistoricalDataSetsWithinRuleSet(HistoricalDataServiceSrv.Iface client)
            throws TException {
        String rule = "rule: amount() < 1 -> decline;";
        Payment firstPayment = TestObjectsFactory.createPayment(10L, P_ID, BeanUtil.SHOP_ID);
        Payment secondPayment = TestObjectsFactory.createPayment(0L, P_ID, BeanUtil.SHOP_ID);
        HistoricalDataSetCheckResult result = client.applyRuleOnHistoricalDataSet(
                TestObjectsFactory.createCascadingEmulationRuleApplyRequest(
                        rule,
                        UUID.randomUUID().toString(),
                        P_ID,
                        null,
                        firstPayment,
                        secondPayment)
        );
        assertEquals(2, result.getHistoricalTransactionCheck().size());
        var check = findHistoricalTransactionCheck(result.getHistoricalTransactionCheck(), firstPayment.getId());
        assertEquals(firstPayment, check.getTransaction());
        assertEquals(rule, check.getCheckResult().getCheckedTemplate());
        assertNull(check.getCheckResult().getConcreteCheckResult().getRuleChecked());
        assertEquals(new ArrayList<>(), check.getCheckResult().getConcreteCheckResult().getNotificationsRule());
        assertEquals(
                ResultStatus.normal(new Normal()),
                check.getCheckResult().getConcreteCheckResult().getResultStatus()
        );
        check = findHistoricalTransactionCheck(result.getHistoricalTransactionCheck(), secondPayment.getId());
        assertEquals(secondPayment, check.getTransaction());
        assertEquals(rule, check.getCheckResult().getCheckedTemplate());
        assertEquals("0", check.getCheckResult().getConcreteCheckResult().getRuleChecked());
        assertEquals(new ArrayList<>(), check.getCheckResult().getConcreteCheckResult().getNotificationsRule());
        assertEquals(
                ResultStatus.decline(new Decline()),
                check.getCheckResult().getConcreteCheckResult().getResultStatus()
        );
    }

    private HistoricalTransactionCheck findHistoricalTransactionCheck(Set<HistoricalTransactionCheck> checks,
                                                                      String id) {
        return checks.stream()
                .filter(check -> check.getTransaction().getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

}
