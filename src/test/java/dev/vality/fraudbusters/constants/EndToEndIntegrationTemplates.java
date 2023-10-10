package dev.vality.fraudbusters.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EndToEndIntegrationTemplates {

    public static final String TEMPLATE = """
            rule:TEMPLATE: count("email", 10, 0, "party_id", "shop_id") > 1
              AND count("email", 10) < 3
              AND sum("email", 10, "party_id", "shop_id") >= 18000
              AND countSuccess("card_token", 10, "party_id", "shop_id") > 1
              AND in(countryBy("country_bank"), "RUS")
              OR sumRefund("card_token", 10, "party_id", "shop_id") > 0
              OR countRefund("card_token", 10, "party_id", "shop_id") > 0
              OR countChargeback("card_token", 10, "party_id", "shop_id") > 0
              OR sumChargeback("card_token", 10, "party_id", "shop_id") > 0
             -> declineAndNotify;
            """;

    public static final String TEMPLATE_CONCRETE =
            "rule:TEMPLATE_CONCRETE: sumSuccess(\"email\", 10) >= 29000 or countError(\"email\", 10) > 2 -> decline;";
    public static final String GROUP_DECLINE =
            "rule:GROUP_DECLINE:  1 >= 0  -> decline;";
    public static final String GROUP_NORMAL =
            "rule:GROUP_NORMAL:  1 < 0  -> decline;";
    public static final String TEMPLATE_CONCRETE_SHOP = """
            rule:TEMPLATE_CONCRETE_SHOP:  sum("email", 10) >= 18000
             AND isTrusted(
                paymentsConditions(
                    condition("RUB",1,1000,10),
                    condition("EUR",2,20)
                ),
                withdrawalsConditions(
                    condition("USD",0,3000,3),
                    condition("CAD",2,4)
                )
             )
             -> accept;";
            """;

}
