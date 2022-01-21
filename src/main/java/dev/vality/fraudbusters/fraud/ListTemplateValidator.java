package dev.vality.fraudbusters.fraud;

import dev.vality.damsel.fraudbusters.Template;
import dev.vality.damsel.fraudbusters.ValidateTemplateResponse;

import java.util.List;

public interface ListTemplateValidator {

    ValidateTemplateResponse validateCompilationTemplate(List<Template> list);

}
