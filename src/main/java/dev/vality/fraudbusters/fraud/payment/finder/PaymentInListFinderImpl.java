package dev.vality.fraudbusters.fraud.payment.finder;

import dev.vality.damsel.wb_list.*;
import dev.vality.fraudbusters.aspect.BasicMetric;
import dev.vality.fraudbusters.constant.EventField;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.util.TimestampUtil;
import dev.vality.fraudo.finder.InListFinder;
import dev.vality.fraudo.model.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PaymentInListFinderImpl implements InListFinder<PaymentModel, PaymentCheckedField> {

    private static final int CURRENT_ONE = 1;
    private static final String LIST_ERROR_LOG = "InListFinderImpl error when findInList e: ";

    private final WbListServiceSrv.Iface wbListServiceSrv;
    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final PaymentRepository paymentRepository;

    @Override
    @BasicMetric("findInBlackList")
    public Boolean findInBlackList(List<Pair<PaymentCheckedField, String>> fields, PaymentModel model) {
        return checkInList(fields, model, ListType.black);
    }

    @Override
    @BasicMetric("findInWhiteList")
    public Boolean findInWhiteList(List<Pair<PaymentCheckedField, String>> fields, PaymentModel model) {
        return checkInList(fields, model, ListType.white);
    }

    @Override
    @BasicMetric("findInGreyList")
    public Boolean findInGreyList(List<Pair<PaymentCheckedField, String>> fields, PaymentModel model) {
        try {
            return fields.stream()
                    .anyMatch(entry ->
                            StringUtils.hasLength(entry.getSecond())
                            && findInList(model.getPartyId(), model.getShopId(), entry.getFirst(), entry.getSecond()));
        } catch (Exception e) {
            log.warn(LIST_ERROR_LOG, e);
            throw new RuleFunctionException(e);
        }
    }

    private Boolean findInList(String partyId, String shopId, PaymentCheckedField field, String value) {
        try {
            if (StringUtils.hasLength(value)) {
                Row row = createRow(ListType.grey, partyId, shopId, field, value);
                Result result = wbListServiceSrv.getRowInfo(row);
                if (result.getRowInfo() != null && result.getRowInfo().isSetCountInfo()) {
                    String resolveField = databasePaymentFieldResolver.resolve(field);
                    return countLessThanWbList(partyId, shopId, value, result, resolveField);
                }
            }
            return false;
        } catch (Exception e) {
            log.warn(LIST_ERROR_LOG, e);
            throw new RuleFunctionException(e);
        }
    }

    @Override
    @BasicMetric("findInNamingList")
    public Boolean findInList(String name, List<Pair<PaymentCheckedField, String>> fields, PaymentModel model) {
        return checkInList(fields, model, ListType.naming);
    }

    @NotNull
    private Boolean countLessThanWbList(
            String partyId,
            String shopId,
            String value,
            Result result,
            String resolveField) {
        log.debug("countLessThanWbList partyId: {} shopId: {} value: {} result: {} resolveField: {}", partyId, shopId,
                value, result, resolveField
        );
        RowInfo rowInfo = result.getRowInfo();
        String startCountTime = rowInfo.getCountInfo().getStartCountTime();
        String ttl = rowInfo.getCountInfo().getTimeToLive();
        Long to = TimestampUtil.generateTimestampWithParse(ttl);
        Long from = TimestampUtil.generateTimestampWithParse(startCountTime);
        if (Instant.now().getEpochSecond() > to || from >= to) {
            return false;
        }
        int currentCount = paymentRepository.countOperationByFieldWithGroupBy(resolveField, value, from, to,
                createFieldModels(partyId, shopId)
        );
        log.debug("countLessThanWbList currentCount: {} rowInfo: {}", currentCount, rowInfo);
        return currentCount + CURRENT_ONE <= rowInfo.getCountInfo().getCount();
    }

    @NotNull
    private List<FieldModel> createFieldModels(String partyId, String shopId) {
        return List.of(
                new FieldModel(EventField.partyId.name(), partyId),
                new FieldModel(EventField.shopId.name(), shopId)
        );
    }

    @NotNull
    private Boolean checkInList(List<Pair<PaymentCheckedField, String>> fields, PaymentModel model, ListType white) {
        try {
            String partyId = model.getPartyId();
            String shopId = model.getShopId();
            List<Row> rows = fields.stream()
                    .filter(entry -> entry.getFirst() != null && StringUtils.hasLength(entry.getSecond()))
                    .map(entry -> createRow(white, partyId, shopId, entry.getFirst(), entry.getSecond()))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(rows)) {
                return wbListServiceSrv.isAnyExist(rows);
            }
            return false;
        } catch (Exception e) {
            log.warn(LIST_ERROR_LOG, e);
            throw new RuleFunctionException(e);
        }
    }

    private Row createRow(ListType listType, String partyId, String shopId, PaymentCheckedField field, String value) {
        return new Row()
                .setId(IdInfo.payment_id(new PaymentId()
                        .setPartyId(partyId)
                        .setShopId(shopId)))
                .setListType(listType)
                .setListName(field.name())
                .setValue(value);
    }
}
