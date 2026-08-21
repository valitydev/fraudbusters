package dev.vality.fraudbusters.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum FraudResultField implements FilterField {

    RESULT_STATUS("resultStatus"),
    CHECKED_RULE("checkedRule"),
    CHECKED_TEMPLATE("checkedTemplate");

    @Getter
    private final String value;

}
