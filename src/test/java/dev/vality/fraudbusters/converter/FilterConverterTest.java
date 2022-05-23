package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.Filter;
import dev.vality.damsel.fraudbusters.Page;
import dev.vality.damsel.fraudbusters.Sort;
import dev.vality.damsel.fraudbusters.SortOrder;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import org.junit.jupiter.api.Test;

import java.util.Set;

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

        assertTrue(dto.getSearchFields().isEmpty());
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
        Set<SearchFieldDto> searchFields = dto.getSearchFields();
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.CARD_TOKEN))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getCardToken().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.EMAIL))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getEmail().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.FINGERPRINT))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getFingerprint().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.PARTY_ID))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getPartyId().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.SHOP_ID))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getShopId().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.STATUS))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getStatus().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.BANK_COUNTRY))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getProviderCountry().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.TERMINAL))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getTerminal().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.ID))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getPaymentId().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.INVOICE_ID))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getInvoiceId().equals(value)));
        assertTrue(searchFields.stream()
                .filter(searchFieldDto -> searchFieldDto.getField().equals(PaymentField.MASKED_PAN))
                .map(SearchFieldDto::getValue)
                .anyMatch(value -> filter.getMaskedPan().equals(value)));
        assertEquals(sort.getField(), dto.getSort().getField());
        assertEquals(sort.getOrder(), SortOrder.valueOf(dto.getSort().getOrder().name()));
    }
}