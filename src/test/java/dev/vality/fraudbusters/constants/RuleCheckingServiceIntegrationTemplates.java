package dev.vality.fraudbusters.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RuleCheckingServiceIntegrationTemplates {

    public static final String FIRST_GROUP_TEMPLATE_PARTY_KEY = "FIRST_GROUP_TEMPLATE_PARTY_KEY";
    public static final String SECOND_GROUP_TEMPLATE_PARTY_KEY = "SECOND_GROUP_TEMPLATE_PARTY_KEY";
    public static final String FIRST_GROUP_TEMPLATE_SHOP_KEY = "FIRST_GROUP_TEMPLATE_SHOP_KEY";
    public static final String SECOND_GROUP_TEMPLATE_SHOP_KEY = "SECOND_GROUP_TEMPLATE_SHOP_KEY";
    public static final String TEMPLATE_PARTY_KEY = "TEMPLATE_PARTY_KEY";
    public static final String TEMPLATE_SHOP_KEY = "TEMPLATE_SHOP_KEY";
    public static final String PREVIOUS_TEMPLATE_PARTY_KEY = "PREVIOUS_TEMPLATE_PARTY_KEY";
    public static final String PREVIOUS_TEMPLATE_SHOP_KEY = "PREVIOUS_TEMPLATE_SHOP_KEY";

    public static final String FIRST_GROUP_TEMPLATE_PARTY = "rule: amount() > 110 -> accept;";
    public static final String SECOND_GROUP_TEMPLATE_PARTY = "rule: amount() > 100 -> accept;";
    public static final String FIRST_GROUP_TEMPLATE_SHOP = "rule: amount() > 85 -> accept;";
    public static final String SECOND_GROUP_TEMPLATE_SHOP = "rule: amount() > 80 -> accept;";
    public static final String TEMPLATE = "rule: amount() > 5 -> accept;";
    public static final String TEMPLATE_PARTY = "rule: amount() > 60 -> accept;";
    public static final String TEMPLATE_SHOP = "rule: amount() > 55 -> accept;";
    public static final String PREVIOUS_TEMPLATE_PARTY = "rule: rand(40) > 20 -> trust;";
    public static final String PREVIOUS_TEMPLATE_SHOP = "rule: amount() > 100 -> accept;";

}
