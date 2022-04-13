package dev.vality.fraudbusters.listener.payment;

import dev.vality.fraudo.FraudoPaymentParser;
import dev.vality.damsel.fraudbusters.Command;
import dev.vality.damsel.fraudbusters.CommandType;
import dev.vality.damsel.fraudbusters.Template;
import dev.vality.fraudbusters.fraud.FraudContextParser;
import dev.vality.fraudbusters.fraud.payment.validator.PaymentTemplateValidator;
import dev.vality.fraudbusters.listener.AbstractPoolCommandListenerExecutor;
import dev.vality.fraudbusters.listener.CommandListener;
import dev.vality.fraudbusters.pool.Pool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateListener extends AbstractPoolCommandListenerExecutor implements CommandListener {

    private final FraudContextParser<FraudoPaymentParser.ParseContext> paymentContextParser;
    private final PaymentTemplateValidator paymentTemplateValidator;
    private final Pool<ParserRuleContext> templatePoolImpl;

    @Override
    @KafkaListener(topics = "${kafka.topic.template}", containerFactory = "templateListenerContainerFactory")
    public void listen(@Payload Command command) {
        log.info("TemplateListener command: {}", command);
        if (command != null && command.isSetCommandBody() && command.getCommandBody().isSetTemplate()) {
            Template template = command.getCommandBody().getTemplate();
            String templateString = new String(template.getTemplate(), StandardCharsets.UTF_8);
            log.info("TemplateListener templateString: {}", templateString);
            if (CommandType.CREATE.equals(command.command_type)) {
                validateTemplate(template.getId(), templateString);
            }
            execCommand(command, template.getId(), templatePoolImpl, paymentContextParser::parse, templateString);
        }
    }

    private void validateTemplate(String id, String templateString) {
        List<String> validate = paymentTemplateValidator.validate(templateString);
        if (!CollectionUtils.isEmpty(validate)) {
            log.warn("TemplateListener templateId: {} validateError: {}", id, validate);
        }
    }

}
