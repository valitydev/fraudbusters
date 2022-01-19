package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.Filter;
import dev.vality.damsel.fraudbusters.Page;
import dev.vality.damsel.fraudbusters.Sort;
import dev.vality.damsel.fraudbusters.SortOrder;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.service.dto.FilterDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class FilterConverterTest {

    private final FilterConverter filterConverter = new FilterConverter();

    @Test
    void convertWithEmptyFiled() {
        Filter filter = new Filter();
        filter.setPaymentId("");
        filter.setTerminal(null);
        Page page = new Page();
        Sort sort = new Sort();

        FilterDto dto = filterConverter.convert(filter, page, sort);

        assertTrue(dto.getSearchPatterns().isEmpty());
        assertEquals(10L, dto.getSize());
        assertNull(dto.getLastId());
        assertNull(dto.getSort().getOrder());
        assertNull(dto.getSort().getField());
    }

    @Test
    void convert() {
        Filter filter = TestObjectsFactory.testFilter();
        Page page = TestObjectsFactory.testPage();
        Sort sort = TestObjectsFactory.testSort();

        FilterDto dto = filterConverter.convert(filter, page, sort);

        assertEquals(page.getSize(), dto.getSize());
        assertEquals(page.getContinuationId(), dto.getLastId());
        assertEquals(filter.getInterval().getLowerBound().getBoundTime(), dto.getTimeFrom());
        assertEquals(filter.getInterval().getUpperBound().getBoundTime(), dto.getTimeTo());
        Map<PaymentField, String> searchPatterns = dto.getSearchPatterns();
        assertEquals(filter.getCardToken(), searchPatterns.get(PaymentField.CARD_TOKEN));
        assertEquals(filter.getEmail(), searchPatterns.get(PaymentField.EMAIL));
        assertEquals(filter.getFingerprint(), searchPatterns.get(PaymentField.FINGERPRINT));
        assertEquals(filter.getPartyId(), searchPatterns.get(PaymentField.PARTY_ID));
        assertEquals(filter.getShopId(), searchPatterns.get(PaymentField.SHOP_ID));
        assertEquals(filter.getStatus(), searchPatterns.get(PaymentField.STATUS));
        assertEquals(filter.getProviderCountry(), searchPatterns.get(PaymentField.BANK_COUNTRY));
        assertEquals(filter.getTerminal(), searchPatterns.get(PaymentField.TERMINAL));
        assertEquals(filter.getPaymentId(), searchPatterns.get(PaymentField.ID));
        assertEquals(filter.getInvoiceId(), searchPatterns.get(PaymentField.INVOICE_ID));
        assertEquals(filter.getMaskedPan(), searchPatterns.get(PaymentField.MASKED_PAN));
        assertEquals(sort.getField(), dto.getSort().getField());
        assertEquals(sort.getOrder(), SortOrder.valueOf(dto.getSort().getOrder().name()));
    }
}