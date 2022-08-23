package dev.vality.fraudbusters.domain;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Event {

    private String id;

    private LocalDate timestamp;
    private Long eventTimeHour;
    private Long eventTime;

    private String ip;
    private String email;
    private String bin;
    private String fingerprint;
    private String shopId;
    private String partyId;
    private String resultStatus;
    private String country;
    private Long amount;
    private String checkedRule;
    private String bankCountry;
    private String currency;
    private String invoiceId;
    private String lastDigits;
    private String bankName;
    private String cardToken;
    private String paymentId;
    private String checkedTemplate;

    private String payerType;
    private String tokenProvider;

    private boolean mobile;
    private boolean recurrent;

}
