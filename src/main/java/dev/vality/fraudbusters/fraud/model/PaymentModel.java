package dev.vality.fraudbusters.fraud.model;

import dev.vality.fraudo.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentModel extends BaseModel {

    private String bin;
    private String lastDigits;
    private String binCountryCode;
    private String cardToken;
    private String shopId;
    private String partyId;

    private Long timestamp;

    private boolean mobile;
    private boolean recurrent;

}
