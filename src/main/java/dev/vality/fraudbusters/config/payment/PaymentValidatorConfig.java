package dev.vality.fraudbusters.config.payment;

import dev.vality.fraudbusters.fraud.FraudTemplateValidator;
import dev.vality.fraudbusters.fraud.ListTemplateValidator;
import dev.vality.fraudbusters.fraud.validator.ListTemplateValidatorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentValidatorConfig {

    @Bean
    public ListTemplateValidator paymentTemplatesValidator(FraudTemplateValidator paymentTemplateValidator) {
        return new ListTemplateValidatorImpl(paymentTemplateValidator);
    }

}
