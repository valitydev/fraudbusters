package dev.vality.fraudbusters.fraud.validator;

import dev.vality.damsel.fraudbusters.Template;
import dev.vality.damsel.fraudbusters.TemplateValidateError;
import dev.vality.damsel.fraudbusters.ValidateTemplateResponse;
import dev.vality.fraudbusters.fraud.FraudTemplateValidator;
import dev.vality.fraudbusters.fraud.ListTemplateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ListTemplateValidatorImpl implements ListTemplateValidator {

    private final FraudTemplateValidator validator;

    @Override
    public ValidateTemplateResponse validateCompilationTemplate(List<Template> list) {
        return new ValidateTemplateResponse()
                .setErrors(list.stream()
                        .map(this::validateAndMap)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    @Nullable
    private TemplateValidateError validateAndMap(Template template) {
        String templateString = new String(template.getTemplate(), StandardCharsets.UTF_8);
        List<String> validate = validator.validate(templateString);
        if (!CollectionUtils.isEmpty(validate)) {
            return new TemplateValidateError()
                    .setId(template.getId())
                    .setReason(validate);
        }
        return null;
    }
}
