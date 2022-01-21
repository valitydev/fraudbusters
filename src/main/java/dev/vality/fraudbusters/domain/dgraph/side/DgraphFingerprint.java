package dev.vality.fraudbusters.domain.dgraph.side;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.vality.fraudbusters.domain.dgraph.DgraphSideObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@ToString(callSuper = true)
public class DgraphFingerprint extends DgraphSideObject {

    public DgraphFingerprint(String fingerprintData, String lastActTime) {
        super(lastActTime);
        this.fingerprintData = fingerprintData;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Fingerprint";

    private String fingerprintData;
    private List<DgraphEmail> emails;
    private List<DgraphToken> tokens;

}
