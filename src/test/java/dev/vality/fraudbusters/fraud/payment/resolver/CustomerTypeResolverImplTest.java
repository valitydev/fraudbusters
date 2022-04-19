package dev.vality.fraudbusters.fraud.payment.resolver;

import dev.vality.fraudo.model.TrustCondition;
import dev.vality.fraudo.payment.resolver.CustomerTypeResolver;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.pool.CardTokenPool;
import dev.vality.fraudbusters.util.ConditionTemplateFactory;
import dev.vality.trusted.tokens.ConditionTemplate;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static dev.vality.fraudbusters.util.BeanUtil.TOKEN;
import static dev.vality.fraudbusters.util.BeanUtil.createPaymentModel;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerTypeResolverImplTest {

    private CustomerTypeResolver<PaymentModel> customerTypeResolver;

    @Mock
    private CardTokenPool cardTokenPool;

    @Mock
    private TrustedTokensSrv.Iface trustedTokensSrv;

    @Mock
    private ConditionTemplateFactory conditionTemplateFactory;

    @BeforeEach
    void setUp() {
        customerTypeResolver = new CustomerTypeResolverImpl(cardTokenPool, trustedTokensSrv, conditionTemplateFactory);
    }

    @Test
    void isTrustedTest() {
        when(cardTokenPool.isExist(anyString()))
                .thenReturn(false)
                .thenReturn(true);

        PaymentModel paymentModel = createPaymentModel();
        assertFalse(customerTypeResolver.isTrusted(paymentModel));
        assertTrue(customerTypeResolver.isTrusted(paymentModel));

        verify(cardTokenPool, times(2)).isExist(TOKEN);
    }

    @Test
    void isTrustedByConditionTemplateNameTest() throws TException {
        when(trustedTokensSrv.isTokenTrustedByConditionTemplateName(anyString(), anyString()))
                .thenReturn(false)
                .thenReturn(true);

        PaymentModel paymentModel = createPaymentModel();
        String templateName = "templateName";
        assertFalse(customerTypeResolver.isTrusted(paymentModel, templateName));
        assertTrue(customerTypeResolver.isTrusted(paymentModel, templateName));

        verify(trustedTokensSrv, times(2))
                .isTokenTrustedByConditionTemplateName(TOKEN, templateName);
    }

    @Test
    void isTrustedByConditionTemplateTest() throws TException {
        when(trustedTokensSrv.isTokenTrusted(anyString(), any(ConditionTemplate.class)))
                .thenReturn(false)
                .thenReturn(true);
        ConditionTemplate conditionTemplate = TestObjectsFactory.createConditionTemplate();
        when(conditionTemplateFactory.createConditionTemplate(anyList(), anyList()))
                .thenReturn(conditionTemplate);

        PaymentModel paymentModel = createPaymentModel();
        List<TrustCondition> paymentsConditions =
                List.of(TestObjectsFactory.createTrustCondition(1), TestObjectsFactory.createTrustCondition(null));
        List<TrustCondition> withdrawalsConditions =
                List.of(TestObjectsFactory.createTrustCondition(2), TestObjectsFactory.createTrustCondition(null));
        assertFalse(customerTypeResolver.isTrusted(paymentModel, paymentsConditions, withdrawalsConditions));
        assertTrue(customerTypeResolver.isTrusted(paymentModel, paymentsConditions, withdrawalsConditions));

        verify(conditionTemplateFactory, times(2))
                .createConditionTemplate(paymentsConditions, withdrawalsConditions);
        verify(trustedTokensSrv, times(2)).isTokenTrusted(TOKEN, conditionTemplate);

    }
}
