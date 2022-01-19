package dev.vality.fraudbusters.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FraudRequest {

    private PaymentModel fraudModel;
    private Metadata metadata;

}
