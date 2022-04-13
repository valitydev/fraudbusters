package dev.vality.fraudbusters.fraud.payment;

import dev.vality.fraudo.FraudoPaymentLexer;
import dev.vality.fraudo.FraudoPaymentParser;
import dev.vality.fraudbusters.fraud.FraudContextParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Component;

@Component
public class PaymentContextParserImpl implements FraudContextParser<FraudoPaymentParser.ParseContext> {

    @Override
    public FraudoPaymentParser.ParseContext parse(String template) {
        FraudoPaymentLexer lexer = new FraudoPaymentLexer(CharStreams.fromString(template));
        FraudoPaymentParser parser = new FraudoPaymentParser(new CommonTokenStream(lexer));
        return parser.parse();
    }

}
