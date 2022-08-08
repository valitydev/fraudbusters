package dev.vality.fraudbusters;

import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.payment.HistoricalPaymentPoolConfig;
import dev.vality.fraudbusters.config.payment.PaymentFraudoConfig;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.fraud.FraudContextParser;
import dev.vality.fraudbusters.fraud.localstorage.LocalResultStorageRepository;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.CountryByIpResolver;
import dev.vality.fraudbusters.fraud.payment.PaymentContextParserImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.CustomerTypeResolverImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.fraud.payment.resolver.PaymentTypeResolverImpl;
import dev.vality.fraudbusters.fraud.payment.validator.PaymentTemplateValidator;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.repository.clickhouse.impl.ChargebackRepository;
import dev.vality.fraudbusters.repository.clickhouse.impl.PaymentRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.RefundRepository;
import dev.vality.fraudbusters.service.RuleCheckingServiceImpl;
import dev.vality.fraudbusters.service.TimeBoundaryServiceImpl;
import dev.vality.fraudbusters.service.dto.CascadingTemplateDto;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.fraudbusters.util.CheckedResultFactory;
import dev.vality.fraudo.FraudoPaymentParser;
import dev.vality.fraudo.constant.ResultStatus;
import dev.vality.fraudo.payment.resolver.CustomerTypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.vality.fraudbusters.constants.RuleCheckingServiceIntegrationTemplates.*;
import static dev.vality.fraudbusters.util.ReferenceKeyGenerator.generateTemplateKey;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ActiveProfiles("full-prod")
@ExtendWith({SpringExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        PaymentTemplateValidator.class,
        RuleCheckingServiceImpl.class,
        PaymentContextParserImpl.class,
        CheckedResultFactory.class,
        PaymentFraudoConfig.class,
        TimeBoundaryServiceImpl.class,
        PaymentTypeResolverImpl.class,
        DatabasePaymentFieldResolver.class,
        HistoricalPaymentPoolConfig.class})
@Import({MockExternalServiceConfig.class})
class RuleCheckingServiceIntegrationTest {

    @MockBean
    private RefundRepository refundRepository;
    @MockBean
    private ChargebackRepository chargebackRepository;
    @MockBean
    private CountryByIpResolver countryByIpResolver;
    @MockBean
    private CustomerTypeResolverImpl customerTypeResolver;
    @MockBean
    private LocalResultStorageRepository localResultStorageRepository;

    @Autowired
    private RuleCheckingServiceImpl ruleTestingService;
    @Autowired
    private HistoricalPool<List<String>> timeGroupPoolImpl;
    @Autowired
    private HistoricalPool<String> timeReferencePoolImpl;
    @Autowired
    private HistoricalPool<String> timeGroupReferencePoolImpl;
    @Autowired
    private HistoricalPool<ParserRuleContext> timeTemplatePoolImpl;
    @Autowired
    private FraudContextParser<FraudoPaymentParser.ParseContext> paymentContextParser;

    private static final Instant now = Instant.now();
    private static final long TIMESTAMP = now.toEpochMilli();
    private static final long RULE_TIMESTAMP = TIMESTAMP - 1_000;
    private static final long PREVIOUS_TIMESTAMP = RULE_TIMESTAMP - 1_000;
    private static final long PREVIOUS_RULE_TIMESTAMP = PREVIOUS_TIMESTAMP - 1_000;
    private static final String GROUP_REF_PARTY = "GROUP_REF_PARTY";
    private static final String GROUP_REF_SHOP = "GROUP_REF_SHOP";
    private static final String PARTY_SHOP_KEY = generateTemplateKey(BeanUtil.PARTY_ID, BeanUtil.SHOP_ID);

    private static final String RULE_CHECKED = "0";

    @BeforeEach
    void setUp() {
        timeGroupPoolImpl.keySet()
                .forEach(key -> timeGroupPoolImpl.remove(key, null));
        timeReferencePoolImpl.keySet()
                .forEach(key -> timeReferencePoolImpl.remove(key, null));
        timeGroupReferencePoolImpl.keySet()
                .forEach(key -> timeGroupReferencePoolImpl.remove(key, null));
        timeTemplatePoolImpl.keySet()
                .forEach(key -> timeTemplatePoolImpl.remove(key, null));
    }

    @Test
    void applyOneRuleOnly() {
        PaymentModel firstTransaction = TestObjectsFactory.createPaymentModel(25L);
        PaymentModel secondTransaction = TestObjectsFactory.createPaymentModel(2L);
        String firstTransactionId = UUID.randomUUID().toString();
        String secondTransactionId = UUID.randomUUID().toString();

        Map<String, CheckedResultModel> result = ruleTestingService.checkSingleRule(
                Map.of(firstTransactionId, firstTransaction,
                        secondTransactionId, secondTransaction),
                TEMPLATE
        );

        assertEquals(2, result.size());
        CheckedResultModel firstCheckedResult = result.get(firstTransactionId);
        assertEquals(TEMPLATE, firstCheckedResult.getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, firstCheckedResult.getResultModel().getResultStatus());
        assertEquals(RULE_CHECKED, firstCheckedResult.getResultModel().getRuleChecked());
        assertEquals(new ArrayList<>(), firstCheckedResult.getResultModel().getNotificationsRule());
        CheckedResultModel secondCheckedResult = result.get(secondTransactionId);
        assertNotNull(secondCheckedResult.getResultModel());
        assertEquals(ResultStatus.NORMAL, secondCheckedResult.getResultModel().getResultStatus());
        assertNull(secondCheckedResult.getResultModel().getRuleChecked());
        assertEquals(new ArrayList<>(), secondCheckedResult.getResultModel().getNotificationsRule());
    }

    @Test
    void applyOneRuleOnlyRuleNotTriggered() {
        PaymentModel firstTransaction = TestObjectsFactory.createPaymentModel(25L);
        String firstTransactionId = UUID.randomUUID().toString();
        String ruleTemplate = "rule: amount() < 1 -> accept;";
        Map<String, CheckedResultModel> result = ruleTestingService.checkSingleRule(
                Map.of(firstTransactionId, firstTransaction),
                ruleTemplate
        );

        assertEquals(1, result.size());
        CheckedResultModel firstCheckedResult = result.get(firstTransactionId);
        assertEquals(ruleTemplate, firstCheckedResult.getCheckedTemplate());
        assertEquals(ResultStatus.NORMAL, firstCheckedResult.getResultModel().getResultStatus());
        assertNull(firstCheckedResult.getResultModel().getRuleChecked());
        assertEquals(new ArrayList<>(), firstCheckedResult.getResultModel().getNotificationsRule());
    }

    @Test
    void applyRuleWithinRulesetNoTimestampDifferentPartyShop() {
        // single templates
        addPartyAndShopTemplateRules();
        addTemplateRule(
                PREVIOUS_TEMPLATE_PARTY,
                PREVIOUS_TEMPLATE_PARTY_KEY,
                BeanUtil.PARTY_ID,
                PREVIOUS_RULE_TIMESTAMP
        );
        addTemplateRule(
                PREVIOUS_TEMPLATE_SHOP,
                PREVIOUS_TEMPLATE_SHOP_KEY,
                PARTY_SHOP_KEY,
                PREVIOUS_RULE_TIMESTAMP
        );

        String partyTransactionId = UUID.randomUUID().toString();
        String partyShopTransactionId = UUID.randomUUID().toString();
        String previousPartyTransactionId = UUID.randomUUID().toString();
        String previousPartyShopTransactionId = UUID.randomUUID().toString();

        CascadingTemplateDto dto = new CascadingTemplateDto();
        dto.setTemplate(TEMPLATE);
        dto.setPartyId(UUID.randomUUID().toString());
        dto.setShopId(UUID.randomUUID().toString());

        Map<String, CheckedResultModel> actual = ruleTestingService.checkRuleWithinRuleset(
                Map.of(
                        partyTransactionId, TestObjectsFactory.createPaymentModel(65L, TIMESTAMP),
                        partyShopTransactionId, TestObjectsFactory.createPaymentModel(57L, TIMESTAMP),
                        previousPartyTransactionId, TestObjectsFactory.createPaymentModel(210L, PREVIOUS_TIMESTAMP),
                        previousPartyShopTransactionId, TestObjectsFactory.createPaymentModel(110L, PREVIOUS_TIMESTAMP)
                ),
                dto
        );
        assertEquals(4, actual.size());
        assertEquals(TEMPLATE_PARTY_KEY, actual.get(partyTransactionId).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(partyTransactionId).getResultModel().getResultStatus());
        assertEquals(TEMPLATE_SHOP_KEY, actual.get(partyShopTransactionId).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(partyShopTransactionId).getResultModel().getResultStatus());
        assertEquals(PREVIOUS_TEMPLATE_PARTY_KEY, actual.get(previousPartyTransactionId).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(previousPartyTransactionId).getResultModel().getResultStatus());
        assertEquals(PREVIOUS_TEMPLATE_SHOP_KEY, actual.get(previousPartyShopTransactionId).getCheckedTemplate());
        assertEquals(
                ResultStatus.ACCEPT,
                actual.get(previousPartyShopTransactionId).getResultModel().getResultStatus()
        );
    }

    @Test
    void applyRuleWithinRulesetGroupRules() {
        addPartyAndShopGroupTemplateRules();

        String firstPartyTransaction = UUID.randomUUID().toString();
        String secondPartyTransaction = UUID.randomUUID().toString();
        String firstPartyShopTransaction = UUID.randomUUID().toString();
        String secondPartyShopTransaction = UUID.randomUUID().toString();

        CascadingTemplateDto dto = new CascadingTemplateDto();
        dto.setTemplate(TEMPLATE);
        dto.setPartyId(BeanUtil.PARTY_ID);
        dto.setShopId(BeanUtil.SHOP_ID);
        dto.setTimestamp(TIMESTAMP);

        Map<String, CheckedResultModel> actual = ruleTestingService.checkRuleWithinRuleset(
                Map.of(
                        firstPartyTransaction, TestObjectsFactory.createPaymentModel(115L, TIMESTAMP),
                        secondPartyTransaction, TestObjectsFactory.createPaymentModel(105L, TIMESTAMP),
                        firstPartyShopTransaction, TestObjectsFactory.createPaymentModel(90L, PREVIOUS_TIMESTAMP),
                        secondPartyShopTransaction, TestObjectsFactory.createPaymentModel(83L, PREVIOUS_TIMESTAMP)
                ),
                dto
        );
        assertEquals(4, actual.size());
        assertEquals(FIRST_GROUP_TEMPLATE_PARTY_KEY, actual.get(firstPartyTransaction).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(firstPartyTransaction).getResultModel().getResultStatus());
        assertEquals(SECOND_GROUP_TEMPLATE_PARTY_KEY, actual.get(secondPartyTransaction).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(secondPartyTransaction).getResultModel().getResultStatus());
        assertEquals(FIRST_GROUP_TEMPLATE_SHOP_KEY, actual.get(firstPartyShopTransaction).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(firstPartyShopTransaction).getResultModel().getResultStatus());
        assertEquals(SECOND_GROUP_TEMPLATE_SHOP_KEY, actual.get(secondPartyShopTransaction).getCheckedTemplate());
        assertEquals(
                ResultStatus.ACCEPT,
                actual.get(secondPartyShopTransaction).getResultModel().getResultStatus()
        );
    }

    @Test
    void applyRuleWithinRulesetChangeTemplateByParty() {
        addPartyAndShopTemplateRules();
        addPartyAndShopGroupTemplateRules();

        String checkTemplateTransactionId = UUID.randomUUID().toString();

        CascadingTemplateDto dto = new CascadingTemplateDto();
        dto.setTemplate(TEMPLATE);
        dto.setPartyId(BeanUtil.PARTY_ID);
        dto.setTimestamp(TIMESTAMP);

        Map<String, CheckedResultModel> actual = ruleTestingService.checkRuleWithinRuleset(
                Map.of(checkTemplateTransactionId, TestObjectsFactory.createPaymentModel(10L, RULE_TIMESTAMP)),
                dto
        );
        assertEquals(1, actual.size());
        assertEquals(TEMPLATE, actual.get(checkTemplateTransactionId).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(checkTemplateTransactionId).getResultModel().getResultStatus());
    }

    @Test
    void applyRuleWithinRulesetChangeTemplateByShop() {
        addPartyAndShopTemplateRules();
        addPartyAndShopGroupTemplateRules();

        String checkTemplateTransactionId = UUID.randomUUID().toString();

        CascadingTemplateDto dto = new CascadingTemplateDto();
        dto.setTemplate(TEMPLATE);
        dto.setPartyId(BeanUtil.PARTY_ID);
        dto.setShopId(BeanUtil.SHOP_ID);
        dto.setTimestamp(TIMESTAMP);

        Map<String, CheckedResultModel> actual = ruleTestingService.checkRuleWithinRuleset(
                Map.of(checkTemplateTransactionId, TestObjectsFactory.createPaymentModel(10L, RULE_TIMESTAMP)),
                dto
        );
        assertEquals(1, actual.size());
        assertEquals(TEMPLATE, actual.get(checkTemplateTransactionId).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(checkTemplateTransactionId).getResultModel().getResultStatus());
    }

    @Test
    void applyRuleWithinRulesetOnlyRuleFromDtoOnPartyShopLevel() {
        String checkTemplateTransactionId = UUID.randomUUID().toString();

        CascadingTemplateDto dto = new CascadingTemplateDto();
        dto.setTemplate(TEMPLATE);
        dto.setPartyId(BeanUtil.PARTY_ID);
        dto.setShopId(BeanUtil.SHOP_ID);
        dto.setTimestamp(TIMESTAMP);

        Map<String, CheckedResultModel> actual = ruleTestingService.checkRuleWithinRuleset(
                Map.of(checkTemplateTransactionId, TestObjectsFactory.createPaymentModel(10L, RULE_TIMESTAMP)),
                dto
        );
        assertEquals(1, actual.size());
        assertEquals(TEMPLATE, actual.get(checkTemplateTransactionId).getCheckedTemplate());
        assertEquals(ResultStatus.ACCEPT, actual.get(checkTemplateTransactionId).getResultModel().getResultStatus());
    }

    @Test
    void applyRuleWithinRulesetDefaultResult() {
        // single templates
        timeReferencePoolImpl.add(BeanUtil.PARTY_ID, RULE_TIMESTAMP, TEMPLATE_PARTY);
        timeReferencePoolImpl.add(PARTY_SHOP_KEY, RULE_TIMESTAMP, TEMPLATE_SHOP);

        //groups of rules
        timeGroupReferencePoolImpl.add(BeanUtil.PARTY_ID, RULE_TIMESTAMP, GROUP_REF_PARTY);
        timeGroupReferencePoolImpl.add(PARTY_SHOP_KEY, RULE_TIMESTAMP, GROUP_REF_SHOP);
        timeGroupPoolImpl.add(GROUP_REF_PARTY, RULE_TIMESTAMP,
                List.of(FIRST_GROUP_TEMPLATE_PARTY, SECOND_GROUP_TEMPLATE_PARTY));
        timeGroupPoolImpl.add(GROUP_REF_SHOP, RULE_TIMESTAMP,
                List.of(FIRST_GROUP_TEMPLATE_SHOP, SECOND_GROUP_TEMPLATE_SHOP));

        String checkTemplateTransactionId = UUID.randomUUID().toString();

        CascadingTemplateDto dto = new CascadingTemplateDto();
        dto.setTemplate(TEMPLATE);
        dto.setPartyId(BeanUtil.PARTY_ID);
        dto.setShopId(BeanUtil.SHOP_ID);
        dto.setTimestamp(TIMESTAMP);

        Map<String, CheckedResultModel> actual = ruleTestingService.checkRuleWithinRuleset(
                Map.of(
                        checkTemplateTransactionId, TestObjectsFactory.createPaymentModel(-5L, RULE_TIMESTAMP)
                ),
                dto
        );

        assertEquals(1, actual.size());
        assertEquals(TEMPLATE, actual.get(checkTemplateTransactionId).getCheckedTemplate());
        assertEquals(ResultStatus.NORMAL, actual.get(checkTemplateTransactionId).getResultModel().getResultStatus());
    }

    private void addPartyAndShopTemplateRules() {
        addTemplateRule(TEMPLATE_PARTY, TEMPLATE_PARTY_KEY, BeanUtil.PARTY_ID, RULE_TIMESTAMP);
        addTemplateRule(TEMPLATE_SHOP, TEMPLATE_SHOP_KEY, PARTY_SHOP_KEY, RULE_TIMESTAMP);
    }

    private void addPartyAndShopGroupTemplateRules() {
        addGroupRule(
                List.of(FIRST_GROUP_TEMPLATE_PARTY, SECOND_GROUP_TEMPLATE_PARTY),
                List.of(FIRST_GROUP_TEMPLATE_PARTY_KEY, SECOND_GROUP_TEMPLATE_PARTY_KEY),
                GROUP_REF_PARTY,
                BeanUtil.PARTY_ID,
                RULE_TIMESTAMP
        );
        addGroupRule(
                List.of(FIRST_GROUP_TEMPLATE_SHOP, SECOND_GROUP_TEMPLATE_SHOP),
                List.of(FIRST_GROUP_TEMPLATE_SHOP_KEY, SECOND_GROUP_TEMPLATE_SHOP_KEY),
                GROUP_REF_SHOP,
                PARTY_SHOP_KEY,
                RULE_TIMESTAMP
        );
    }

    private void addTemplateRule(String template, String templateKey, String refKey, Long timestamp) {
        timeReferencePoolImpl.add(refKey, timestamp, templateKey);
        timeTemplatePoolImpl.add(templateKey, timestamp, paymentContextParser.parse(template));
    }

    private void addGroupRule(List<String> templates, List<String> templateKeys, String groupRefKey, String groupKey,
                              Long timestamp) {
        timeGroupReferencePoolImpl.add(groupKey, timestamp, groupRefKey);
        timeGroupPoolImpl.add(groupRefKey, timestamp, templateKeys);
        for (int i = 0; i < templates.size(); i++) {
            timeTemplatePoolImpl.add(templateKeys.get(i), timestamp, paymentContextParser.parse(templates.get(i)));
        }
    }

}
