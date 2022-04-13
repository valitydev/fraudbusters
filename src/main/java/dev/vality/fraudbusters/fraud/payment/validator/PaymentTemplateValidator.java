package dev.vality.fraudbusters.fraud.payment.validator;

import dev.vality.fraudo.FraudoPaymentLexer;
import dev.vality.fraudo.FraudoPaymentParser;
import dev.vality.fraudbusters.fraud.FraudTemplateValidator;
import dev.vality.fraudbusters.fraud.listener.ValidateErrorListener;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PaymentTemplateValidator implements FraudTemplateValidator {

    @Override
    public List<String> validate(String template) {
        log.info("PaymentTemplateValidator validate: {}", template);
        FraudoPaymentLexer lexer = new FraudoPaymentLexer(CharStreams.fromString(template));
        FraudoPaymentParser parser = new FraudoPaymentParser(new CommonTokenStream(lexer));
        ValidateErrorListener validateErrorListener = new ValidateErrorListener();
        parser.addErrorListener(validateErrorListener);
        parser.parse();
        log.info("PaymentTemplateValidator validated errors: {}", validateErrorListener.getErrors());
        return validateErrorListener.getErrors();
    }

}
