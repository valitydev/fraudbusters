package dev.vality.fraudbusters.stream.impl;

import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.util.CheckedResultFactory;
import dev.vality.fraudo.FraudoPaymentParser;
import dev.vality.fraudo.constant.ResultStatus;
import dev.vality.fraudo.model.ResultModel;
import dev.vality.fraudo.model.RuleResult;
import dev.vality.fraudo.visitor.TemplateVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.vality.fraudbusters.factory.TestObjectsFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleCheckingApplierImplTest {

    private RuleCheckingApplierImpl<PaymentModel> ruleCheckingApplier;

    @Mock
    private TemplateVisitor<PaymentModel, ResultModel> templateVisitor;

    @Mock
    private CheckedResultFactory checkedResultFactory;

    @Mock
    private HistoricalPool<ParserRuleContext> templatePool;

    private static final String ACCEPT_TEMPLATE_STRING = "ACCEPT_TEMPLATE_STRING";
    private static final String DECLINE_TEMPLATE_STRING = "DECLINE_TEMPLATE_STRING";
    private static final String ACCEPT_RULE_CHECKED = "0";
    private static final String DECLINE_RULE_CHECKED = "1";
    private static final RuleResult ACCEPTED_RULE_RESULT = createRuleResult(ACCEPT_RULE_CHECKED, ResultStatus.ACCEPT);
    private static final RuleResult DECLINED_RULE_RESULT = createRuleResult(DECLINE_RULE_CHECKED, ResultStatus.DECLINE);
    private static final Long TIMESTAMP = Instant.now().toEpochMilli();

    @BeforeEach
    void setUp() {
        ruleCheckingApplier =
                new RuleCheckingApplierImpl<>(templateVisitor, templatePool, checkedResultFactory);
    }

    @Test
    void applyNullContext() {
        when(templatePool.get(ACCEPT_TEMPLATE_STRING, TIMESTAMP)).thenReturn(null);

        Optional<CheckedResultModel> actual =
                ruleCheckingApplier.apply(createPaymentModel(), ACCEPT_TEMPLATE_STRING, TIMESTAMP);

        assertEquals(Optional.empty(), actual);
        verify(templatePool, times(1)).get(ACCEPT_TEMPLATE_STRING, TIMESTAMP);
    }

    @Test
    void apply() {
        FraudoPaymentParser.ParseContext parseContext =
                new FraudoPaymentParser.ParseContext(new ParserRuleContext(), 0);
        PaymentModel paymentModel = createPaymentModel();
        ResultModel resultModel = createResultModel(List.of(ACCEPTED_RULE_RESULT, DECLINED_RULE_RESULT));
        CheckedResultModel checkedResultModel =
                createCheckedResultModel(ACCEPT_TEMPLATE_STRING, ACCEPT_RULE_CHECKED, ResultStatus.ACCEPT);

        when(templatePool.get(ACCEPT_TEMPLATE_STRING, TIMESTAMP)).thenReturn(parseContext);
        when(templateVisitor.visit(parseContext, paymentModel)).thenReturn(resultModel);
        when(checkedResultFactory.createCheckedResultWithNotifications(ACCEPT_TEMPLATE_STRING, resultModel))
                .thenReturn(checkedResultModel);

        Optional<CheckedResultModel> actual =
                ruleCheckingApplier.apply(paymentModel, ACCEPT_TEMPLATE_STRING, TIMESTAMP);
        assertEquals(Optional.of(checkedResultModel), actual);

        verify(templatePool, times(1)).get(ACCEPT_TEMPLATE_STRING, TIMESTAMP);
        verify(templateVisitor, times(1)).visit(parseContext, paymentModel);
        verify(checkedResultFactory, times(1))
                .createCheckedResultWithNotifications(ACCEPT_TEMPLATE_STRING, resultModel);
    }

    @Test
    void applyForAny() {
        FraudoPaymentParser.ParseContext parseContext =
                new FraudoPaymentParser.ParseContext(new ParserRuleContext(), 0);
        PaymentModel paymentModel = createPaymentModel();
        ResultModel resultModel = createResultModel(List.of(ACCEPTED_RULE_RESULT, DECLINED_RULE_RESULT));
        CheckedResultModel checkedResultModel =
                createCheckedResultModel(ACCEPT_TEMPLATE_STRING, ACCEPT_RULE_CHECKED, ResultStatus.ACCEPT);

        when(templatePool.get(ACCEPT_TEMPLATE_STRING, TIMESTAMP)).thenReturn(parseContext);
        when(templateVisitor.visit(parseContext, paymentModel)).thenReturn(resultModel);
        when(checkedResultFactory.createCheckedResultWithNotifications(ACCEPT_TEMPLATE_STRING, resultModel))
                .thenReturn(checkedResultModel);

        Optional<CheckedResultModel> actual = ruleCheckingApplier.applyForAny(
                paymentModel,
                List.of(ACCEPT_TEMPLATE_STRING, DECLINE_TEMPLATE_STRING),
                TIMESTAMP
        );
        assertEquals(Optional.of(checkedResultModel), actual);

        verify(templatePool, times(1)).get(ACCEPT_TEMPLATE_STRING, TIMESTAMP);
        verify(templateVisitor, times(1)).visit(parseContext, paymentModel);
        verify(checkedResultFactory, times(1))
                .createCheckedResultWithNotifications(ACCEPT_TEMPLATE_STRING, resultModel);

    }

    @Test
    void applyForAnyNullList() {
        assertEquals(Optional.empty(), ruleCheckingApplier.applyForAny(createPaymentModel(), null, null));
    }

    @Test
    void applyForAnyEmptyList() {
        assertEquals(Optional.empty(), ruleCheckingApplier.applyForAny(createPaymentModel(), new ArrayList<>(), null));
    }

    @Test
    void applyWithContextNullContext() {
        assertEquals(
                Optional.empty(),
                ruleCheckingApplier.applyWithContext(createPaymentModel(), ACCEPT_TEMPLATE_STRING, null)
        );
    }

    @Test
    void applyWithContext() {
        FraudoPaymentParser.ParseContext parseContext =
                new FraudoPaymentParser.ParseContext(new ParserRuleContext(), 0);
        PaymentModel paymentModel = createPaymentModel();
        ResultModel resultModel = createResultModel(List.of(ACCEPTED_RULE_RESULT, DECLINED_RULE_RESULT));
        CheckedResultModel checkedResultModel =
                createCheckedResultModel(ACCEPT_TEMPLATE_STRING, ACCEPT_RULE_CHECKED, ResultStatus.ACCEPT);

        when(templateVisitor.visit(parseContext, paymentModel)).thenReturn(resultModel);
        when(checkedResultFactory.createCheckedResultWithNotifications(ACCEPT_TEMPLATE_STRING, resultModel))
                .thenReturn(checkedResultModel);

        Optional<CheckedResultModel> actual =
                ruleCheckingApplier.applyWithContext(paymentModel, ACCEPT_TEMPLATE_STRING, parseContext);
        assertEquals(Optional.of(checkedResultModel), actual);

        verify(templatePool, times(0)).get(anyString(), anyLong());
        verify(templateVisitor, times(1)).visit(parseContext, paymentModel);
        verify(checkedResultFactory, times(1))
                .createCheckedResultWithNotifications(ACCEPT_TEMPLATE_STRING, resultModel);
    }

}
