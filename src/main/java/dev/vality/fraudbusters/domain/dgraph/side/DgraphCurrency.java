package dev.vality.fraudbusters.domain.dgraph.side;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.vality.fraudbusters.domain.dgraph.DgraphSideObject;
import dev.vality.fraudbusters.domain.dgraph.common.DgraphWithdrawal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DgraphCurrency extends DgraphSideObject {

    public DgraphCurrency(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Currency";

    private String currencyCode;
    private List<DgraphWithdrawal> withdrawals;

}
