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
public class DgraphIp extends DgraphExtendedSideObject {

    public DgraphIp(String ipAddress, String lastActTime) {
        super(lastActTime);
        this.ipAddress = ipAddress;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Ip";

    private String ipAddress;
    private List<DgraphCountry> countries;

}
