package dev.vality.fraudbusters.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {

    private Long timestamp;
    private String currency;
    private String invoiceId;
    private String paymentId;
    private String lastDigits;
    private String bankName;

    private String payerType;
    private String tokenProvider;

}
