package dev.vality.fraudbusters.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoadDataIntegrationsTemplates {

    public static final String TEMPLATE = """
            rule:TEMPLATE: sum("card_token", 1000, "party_id", "shop_id", "mobile") > 0
             and unique("email", "ip", 1444, "recurrent") < 2 and isRecurrent()
             and count("card_token", 1000, "party_id", "shop_id") > 5  -> decline;
            """;

    public static final String TEMPLATE_2 =
            "rule:TEMPLATE: count(\"card_token\", 1000, \"party_id\", \"shop_id\") > 2 -> decline;";

    public static final String TEMPLATE_CONCRETE =
            "rule:TEMPLATE_CONCRETE: count(\"card_token\", 10) > 0  -> accept;";

}
