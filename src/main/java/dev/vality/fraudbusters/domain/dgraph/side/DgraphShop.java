package dev.vality.fraudbusters.domain.dgraph.side;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.vality.fraudbusters.domain.dgraph.DgraphExtendedSideObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DgraphShop extends DgraphExtendedSideObject {

    public DgraphShop(String shopId, String lastActTime) {
        super(lastActTime);
        this.shopId = shopId;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Shop";

    private DgraphParty party;
    private String shopId;

}
