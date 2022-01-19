package dev.vality.fraudbusters.domain.dgraph;

import dev.vality.fraudbusters.domain.dgraph.side.DgraphEmail;
import dev.vality.fraudbusters.domain.dgraph.side.DgraphFingerprint;
import dev.vality.fraudbusters.domain.dgraph.side.DgraphToken;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
public abstract class DgraphExtendedSideObject extends DgraphSideObject {

    public DgraphExtendedSideObject(String lastActTime) {
        super(lastActTime);
    }

    private List<DgraphToken> tokens;
    private List<DgraphEmail> emails;
    private List<DgraphFingerprint> fingerprints;

}
