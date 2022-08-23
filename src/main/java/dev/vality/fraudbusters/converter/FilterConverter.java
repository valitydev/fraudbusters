package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.Filter;
import dev.vality.damsel.fraudbusters.Page;
import dev.vality.damsel.fraudbusters.Sort;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.service.dto.SortDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class FilterConverter {

    public FilterDto convert(Filter filter, Page page, Sort sort) {
        FilterDto filterDto = new FilterDto();
        Set<SearchFieldDto> searchFields = assembleSearchFields(filter);
        filterDto.setSearchFields(searchFields);
        filterDto.setLastId(page.getContinuationId());
        if (page.getSize() > 0) {
            filterDto.setSize(page.getSize());
        }
        if (filter.isSetInterval()) {
            filterDto.setTimeFrom(filter.getInterval().getLowerBound().getBoundTime());
            filterDto.setTimeTo(filter.getInterval().getUpperBound().getBoundTime());
        }
        SortDto sortDto = new SortDto();
        sortDto.setField(sort.getField());
        sortDto.setOrder(Objects.nonNull(sort.getOrder())
                ? SortOrder.valueOf(sort.getOrder().name())
                : null);
        filterDto.setSort(sortDto);
        return filterDto;
    }

    private Set<SearchFieldDto> assembleSearchFields(Filter filter) {
        Set<SearchFieldDto> searchFields = new HashSet<>();
        if (filter.isSetCardToken() && StringUtils.hasLength(filter.getCardToken())) {
            addSearchField(searchFields, PaymentField.CARD_TOKEN, FieldType.STRING, filter.getCardToken());
        }
        if (filter.isSetEmail() && StringUtils.hasLength(filter.getEmail())) {
            addSearchField(searchFields, PaymentField.EMAIL, FieldType.STRING, filter.getEmail());
        }
        if (filter.isSetStatus() && StringUtils.hasLength(filter.getStatus())) {
            addSearchField(searchFields, PaymentField.STATUS, FieldType.ENUM, filter.getStatus());
        }
        if (filter.isSetShopId() && StringUtils.hasLength(filter.getShopId())) {
            addSearchField(searchFields, PaymentField.SHOP_ID, FieldType.STRING, filter.getShopId());
        }
        if (filter.isSetPartyId() && StringUtils.hasLength(filter.getPartyId())) {
            addSearchField(searchFields, PaymentField.PARTY_ID, FieldType.STRING, filter.getPartyId());
        }
        if (filter.isSetProviderCountry() && StringUtils.hasLength(filter.getProviderCountry())) {
            addSearchField(searchFields, PaymentField.BANK_COUNTRY, FieldType.STRING, filter.getProviderCountry());
        }
        if (filter.isSetFingerprint() && StringUtils.hasLength(filter.getFingerprint())) {
            addSearchField(searchFields, PaymentField.FINGERPRINT, FieldType.STRING, filter.getFingerprint());
        }
        if (filter.isSetTerminal() && StringUtils.hasLength(filter.getTerminal())) {
            addSearchField(searchFields, PaymentField.TERMINAL, FieldType.STRING, filter.getTerminal());
        }
        if (filter.isSetPaymentId() && StringUtils.hasLength(filter.getPaymentId())) {
            addSearchField(searchFields, PaymentField.ID, FieldType.STRING, filter.getPaymentId());
        }
        if (filter.isSetMaskedPan() && StringUtils.hasLength(filter.getMaskedPan())) {
            addSearchField(searchFields, PaymentField.LAST_DIGITS, FieldType.STRING, filter.getMaskedPan());
        }
        if (filter.isSetInvoiceId() && StringUtils.hasLength(filter.getInvoiceId())) {
            addSearchField(searchFields, PaymentField.INVOICE_ID, FieldType.STRING, filter.getInvoiceId());
        }
        return searchFields;
    }

    private void addSearchField(Set<SearchFieldDto> searchFields,
                                PaymentField cardToken,
                                FieldType string,
                                String filter) {
        searchFields.add(
                SearchFieldDto.builder()
                        .field(cardToken)
                        .type(string)
                        .value(filter)
                        .build()
        );
    }

}
