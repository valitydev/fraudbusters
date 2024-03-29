package dev.vality.fraudbusters.fraud.finder;

import dev.vality.damsel.wb_list.CountInfo;
import dev.vality.damsel.wb_list.Result;
import dev.vality.damsel.wb_list.RowInfo;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.finder.PaymentInListFinderImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudo.finder.InListFinder;
import dev.vality.fraudo.model.Pair;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InGreyListFinderImplTest {

    private static final String PARTY_ID = "partyId";
    private static final String SHOP_ID = "shopId";
    private static final String VALUE = "1234123";

    private InListFinder<PaymentModel, PaymentCheckedField> inGreyListFinder;

    @Mock
    private WbListServiceSrv.Iface wbListServiceSrv;
    @Mock
    private PaymentRepository analyticRepository;

    @BeforeEach
    public void init() {
        inGreyListFinder =
                new PaymentInListFinderImpl(wbListServiceSrv, new DatabasePaymentFieldResolver(), analyticRepository);
    }

    @Test
    public void findInList() throws TException {
        when(wbListServiceSrv.getRowInfo(any())).thenReturn(new Result().setRowInfo(new RowInfo()));

        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setPartyId(PARTY_ID);
        paymentModel.setShopId(SHOP_ID);
        Boolean inList = inGreyListFinder.findInGreyList(
                List.of(new Pair<>(PaymentCheckedField.CARD_TOKEN, VALUE)),
                paymentModel
        );

        assertFalse(inList);

        Instant now = Instant.now();
        Result result = new Result().setRowInfo(RowInfo
                .count_info(new CountInfo()
                        .setCount(5L)
                        .setTimeToLive(now.plusSeconds(10L).toString())
                        .setStartCountTime(now.toString())));

        when(wbListServiceSrv.getRowInfo(any())).thenReturn(result);
        when(analyticRepository.countOperationByFieldWithGroupBy(any(), any(), any(), any(), any())).thenReturn(6);

        inList = inGreyListFinder.findInGreyList(
                List.of(new Pair<>(PaymentCheckedField.CARD_TOKEN, VALUE)),
                paymentModel
        );
        assertFalse(inList);

        when(analyticRepository.countOperationByFieldWithGroupBy(any(), any(), any(), any(), any())).thenReturn(4);
        inList = inGreyListFinder.findInGreyList(
                List.of(new Pair<>(PaymentCheckedField.CARD_TOKEN, VALUE)),
                paymentModel
        );
        assertTrue(inList);
    }

    @Test
    public void testFindInList() throws TException {
        Instant now = Instant.now();
        Result result = new Result().setRowInfo(RowInfo
                .count_info(new CountInfo()
                        .setCount(5L)
                        .setTimeToLive(now.plusSeconds(10L).toString())
                        .setStartCountTime(now.toString())));
        when(wbListServiceSrv.getRowInfo(any())).thenReturn(result);
        when(analyticRepository.countOperationByFieldWithGroupBy(anyString(), any(), anyLong(), anyLong(), anyList()))
                .thenReturn(4);
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setPartyId(PARTY_ID);
        paymentModel.setShopId(SHOP_ID);
        Boolean inList = inGreyListFinder.findInGreyList(
                List.of(new Pair<>(PaymentCheckedField.CARD_TOKEN, VALUE)),
                paymentModel
        );

        assertTrue(inList);
    }
}
