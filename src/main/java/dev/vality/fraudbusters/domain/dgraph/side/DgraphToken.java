package dev.vality.fraudbusters.domain.dgraph.side;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.vality.fraudbusters.domain.dgraph.DgraphSideObject;
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
public class DgraphToken extends DgraphSideObject {

    public DgraphToken(String tokenId, String lastDigits, String lastActTime) {
        super(lastActTime);
        this.tokenId = tokenId;
        this.lastDigits = lastDigits;
    }

    @JsonProperty("dgraph.type")
    private final String type = "Token";

    private String tokenId;
    private String lastDigits;
    private String tokenizationMethod;
    private String paymentSystem;
    private String issuerCountry;
    private String bankName;
    private String cardholderName;
    private String category;

    private List<DgraphEmail> emails;
    private List<DgraphFingerprint> fingerprints;
    private DgraphBin bin;

}