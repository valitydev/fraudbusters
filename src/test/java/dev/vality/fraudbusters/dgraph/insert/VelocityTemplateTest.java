package dev.vality.fraudbusters.dgraph.insert;

import dev.vality.fraudbusters.config.dgraph.TemplateConfig;
import dev.vality.fraudbusters.dgraph.insert.data.VelocityTestData;
import dev.vality.fraudbusters.domain.dgraph.common.*;
import dev.vality.fraudbusters.factory.TestDgraphObjectFactory;
import dev.vality.fraudbusters.service.template.TemplateService;
import dev.vality.fraudbusters.service.template.insert.chargeback.InsertChargebackQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.chargeback.UpsertChargebackQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.fraud.InsertFraudPaymentQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.fraud.UpsertFraudPaymentQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.payment.InsertPaymentQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.payment.UpsertPaymentQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.refund.InsertRefundQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.refund.UpsertRefundQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.withdrawal.InsertWithdrawalQueryTemplateService;
import dev.vality.fraudbusters.service.template.insert.withdrawal.UpsertWithdrawalQueryTemplateService;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VelocityTemplateTest {

    private final VelocityEngine velocityEngine = new TemplateConfig().velocityEngine();
    private final TemplateService<DgraphPayment> insertPaymentQueryTemplateService =
            new InsertPaymentQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphPayment> upsertPaymentQueryTemplateService =
            new UpsertPaymentQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphFraudPayment> insertFraudPaymentQueryTemplateService =
            new InsertFraudPaymentQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphFraudPayment> upsertFraudPaymentQueryTemplateService =
            new UpsertFraudPaymentQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphRefund> upsertRefundQueryTemplateService =
            new UpsertRefundQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphRefund> insertRefundQueryTemplateService =
            new InsertRefundQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphChargeback> upsertChargebackQueryTemplateService =
            new UpsertChargebackQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphChargeback> insertChargebackQueryTemplateService =
            new InsertChargebackQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphWithdrawal> upsertWithdrawalQueryTemplateService =
            new UpsertWithdrawalQueryTemplateService(velocityEngine);
    private final TemplateService<DgraphWithdrawal> insertWithdrawalQueryTemplateService =
            new InsertWithdrawalQueryTemplateService(velocityEngine);

    @Test
    public void generatePaymentUpsertQueryTest() {
        DgraphPayment smallTestDgraphPayment = TestDgraphObjectFactory.createSmallTestDgraphPayment();
        String firstQuery = upsertPaymentQueryTemplateService.build(smallTestDgraphPayment);
        assertNotNull(firstQuery);
        assertEquals(VelocityTestData.TEST_SHORT_PAYMENT_UPSERT_QUERY, firstQuery);

        DgraphPayment fullTestDgraphPayment = TestDgraphObjectFactory.createFullTestDgraphPayment();
        String secondQuery = upsertPaymentQueryTemplateService.build(fullTestDgraphPayment);
        assertNotNull(secondQuery);
        assertEquals(VelocityTestData.TEST_FULL_PAYMENT_UPSERT_QUERY, secondQuery);
    }

    @Test
    public void generatePaymentInsertTest() {
        DgraphPayment smallTestDgraphPayment = TestDgraphObjectFactory.createSmallTestDgraphPayment();
        String firstInsertBlock = insertPaymentQueryTemplateService.build(smallTestDgraphPayment);
        assertNotNull(firstInsertBlock);
        assertEquals(VelocityTestData.TEST_INSERT_PAYMENT_SHORT_BLOCK, firstInsertBlock);

        DgraphPayment fullTestDgraphPayment = TestDgraphObjectFactory.createFullTestDgraphPayment();
        String secondInsertBlock = insertPaymentQueryTemplateService.build(fullTestDgraphPayment);
        assertNotNull(secondInsertBlock);
        assertEquals(VelocityTestData.TEST_INSERT_FULL_PAYMENT_BLOCK, secondInsertBlock);
    }

    @Test
    public void generateRefundUpsertQueryTemplateTest() {
        DgraphRefund smallTestDgraphRefund = TestDgraphObjectFactory.createSmallTestDgraphRefund();
        String firstQuery = upsertRefundQueryTemplateService.build(smallTestDgraphRefund);
        assertNotNull(firstQuery);
        assertEquals(VelocityTestData.TEST_SMALL_REFUND_UPSERT_QUERY, firstQuery);

        DgraphRefund fullTestDgraphRefund = TestDgraphObjectFactory.createFullTestDgraphRefund();
        String secondQuery = upsertRefundQueryTemplateService.build(fullTestDgraphRefund);
        assertNotNull(secondQuery);
        assertEquals(VelocityTestData.TEST_FULL_REFUND_UPSERT_QUERY, secondQuery);
    }

    @Test
    public void generateInsertRefundTemplateTest() {
        DgraphRefund smallTestDgraphRefund = TestDgraphObjectFactory.createSmallTestDgraphRefund();
        String firstQuery = insertRefundQueryTemplateService.build(smallTestDgraphRefund);
        assertNotNull(firstQuery);
        assertEquals(VelocityTestData.TEST_INSERT_SMALL_REFUND_BLOCK, firstQuery);

        DgraphRefund fullTestDgraphRefund = TestDgraphObjectFactory.createFullTestDgraphRefund();
        String secondQuery = insertRefundQueryTemplateService.build(fullTestDgraphRefund);
        assertNotNull(secondQuery);
        assertEquals(VelocityTestData.TEST_INSERT_FULL_REFUND_BLOCK, secondQuery);
    }

    @Test
    public void generateChargebackUpsertQueryTemplateTest() {
        DgraphChargeback smallTestDgraphChargeback = TestDgraphObjectFactory.createSmallTestDgraphChargeback();
        String firstQuery = upsertChargebackQueryTemplateService.build(smallTestDgraphChargeback);
        assertNotNull(firstQuery);
        assertEquals(VelocityTestData.TEST_SMALL_CHARGEBACK_UPSERT_QUERY, firstQuery);

        DgraphChargeback fullTestDgraphChargeback = TestDgraphObjectFactory.createFullTestDgraphChargeback();
        String secondQuery = upsertChargebackQueryTemplateService.build(fullTestDgraphChargeback);
        assertNotNull(secondQuery);
        assertEquals(VelocityTestData.TEST_FULL_CHARGEBACK_UPSERT_QUERY, secondQuery);
    }

    @Test
    public void generateInsertChargebackTemplateTest() {
        DgraphChargeback smallTestDgraphChargeback = TestDgraphObjectFactory.createSmallTestDgraphChargeback();
        String firstQuery = insertChargebackQueryTemplateService.build(smallTestDgraphChargeback);
        assertNotNull(firstQuery);
        assertEquals(VelocityTestData.TEST_INSERT_SMALL_CHARGEBACK_BLOCK, firstQuery);

        DgraphChargeback fullTestDgraphChargeback = TestDgraphObjectFactory.createFullTestDgraphChargeback();
        String secondQuery = insertChargebackQueryTemplateService.build(fullTestDgraphChargeback);
        assertNotNull(secondQuery);
        assertEquals(VelocityTestData.TEST_INSERT_FULL_CHARGEBACK_BLOCK, secondQuery);
    }

    @Test
    public void generateWithdrawalUpsertQueryTemplateTest() {
        DgraphWithdrawal testSmallDgraphWithdrawal = TestDgraphObjectFactory.createTestSmallDgraphWithdrawal();
        String firstQuery = upsertWithdrawalQueryTemplateService.build(testSmallDgraphWithdrawal);
        assertNotNull(firstQuery);
        assertEquals(VelocityTestData.TEST_SMALL_WITHDRAWAL_UPSERT_QUERY, firstQuery);

        DgraphWithdrawal testFullDgraphWithdrawal = TestDgraphObjectFactory.createTestFullDgraphWithdrawal();
        String secondQuery = upsertWithdrawalQueryTemplateService.build(testFullDgraphWithdrawal);
        assertNotNull(secondQuery);
        assertEquals(VelocityTestData.TEST_FULL_WITHDRAWAL_UPSERT_QUERY, secondQuery);
    }

    @Test
    public void generateInsertWithdrawalTemplateTest() {
        DgraphWithdrawal testSmallDgraphWithdrawal = TestDgraphObjectFactory.createTestSmallDgraphWithdrawal();
        String firstQuery = insertWithdrawalQueryTemplateService.build(testSmallDgraphWithdrawal);
        assertNotNull(firstQuery);
        assertEquals(VelocityTestData.TEST_INSERT_SMALL_WITHDRAWAL_BLOCK, firstQuery);

        DgraphWithdrawal testFullDgraphWithdrawal = TestDgraphObjectFactory.createTestFullDgraphWithdrawal();
        String secondQuery = insertWithdrawalQueryTemplateService.build(testFullDgraphWithdrawal);
        assertNotNull(secondQuery);
        assertEquals(VelocityTestData.TEST_INSERT_FULL_WITHDRAWAL_BLOCK, secondQuery);
    }

    @Test
    public void generateFraudPaymentUpsertQueryTemplateTest() {
        DgraphFraudPayment testFraudDgraphPayment = TestDgraphObjectFactory.createTestFraudDgraphPayment();
        String query = upsertFraudPaymentQueryTemplateService.build(testFraudDgraphPayment);
        assertNotNull(query);
        assertEquals(VelocityTestData.TEST_FRAUD_PAYMENT_UPSERT_QUERY, query);
    }

    @Test
    public void generateInsertFraudPaymentTemplateTest() {
        DgraphFraudPayment testFraudDgraphPayment = TestDgraphObjectFactory.createTestFraudDgraphPayment();
        String insertBlock = insertFraudPaymentQueryTemplateService.build(testFraudDgraphPayment);
        assertNotNull(insertBlock);
        assertEquals(VelocityTestData.TEST_INSERT_FRAUD_PAYMENT_BLOCK, insertBlock);
    }

}
