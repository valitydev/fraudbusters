package dev.vality.fraudbusters.domain.dgraph.side;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.vality.fraudbusters.domain.dgraph.DgraphSideObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;


@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DgraphEmail extends DgraphSideObject {

    public DgraphEmail(String userEmail, String lastActTime) {
        super(lastActTime);
        this.userEmail = userEmail;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Email";

    private String userEmail;
    private List<DgraphFingerprint> fingerprints;
    private List<DgraphToken> tokens;

}
