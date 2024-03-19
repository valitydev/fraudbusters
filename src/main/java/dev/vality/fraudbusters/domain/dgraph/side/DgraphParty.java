package dev.vality.fraudbusters.domain.dgraph.side;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.vality.fraudbusters.domain.dgraph.DgraphExtendedSideObject;
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
public class DgraphParty extends DgraphExtendedSideObject {

    public DgraphParty(String partyId, String lastActTime) {
        super(lastActTime);
        this.partyId = partyId;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Party";

    private String partyId;
    private List<DgraphShop> shops;

}
