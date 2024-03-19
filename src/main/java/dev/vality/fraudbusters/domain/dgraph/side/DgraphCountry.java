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
public class DgraphCountry extends DgraphExtendedSideObject { //TODO: check fingerprints filling

    public DgraphCountry(String countryName) {
        this.countryName = countryName;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Country";

    private String countryName;
    private List<DgraphIp> ips;

}
