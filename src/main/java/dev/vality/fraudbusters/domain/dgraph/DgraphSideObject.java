package dev.vality.fraudbusters.domain.dgraph;

import dev.vality.fraudbusters.domain.dgraph.common.DgraphChargeback;
import dev.vality.fraudbusters.domain.dgraph.common.DgraphPayment;
import dev.vality.fraudbusters.domain.dgraph.common.DgraphRefund;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;


@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class DgraphSideObject extends DgraphObject {

    public DgraphSideObject(String lastActTime) {
        this.lastActTime = lastActTime;
    }

    private String lastActTime;
    private List<DgraphPayment> payments;
    private List<DgraphRefund> refunds;
    private List<DgraphChargeback> chargebacks;

}
