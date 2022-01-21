package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.CascasdingTemplateEmulation;
import dev.vality.damsel.fraudbusters.Template;
import dev.vality.damsel.fraudbusters.TemplateReference;
import dev.vality.fraudbusters.service.dto.CascadingTemplateDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CascadingTemplateEmulationToCascadingTemplateDtoConverterTest {

    private final CascadingTemplateEmulationToCascadingTemplateDtoConverter converter =
            new CascadingTemplateEmulationToCascadingTemplateDtoConverter();

    @Test
    void convertFullEntity() {
        final String templateId = UUID.randomUUID().toString();
        final String template = UUID.randomUUID().toString();
        final String shopId = UUID.randomUUID().toString();
        final String partyId = UUID.randomUUID().toString();
        final Instant time = Instant.now();

        CascadingTemplateDto expected = new CascadingTemplateDto();
        expected.setTemplate(template);
        expected.setTimestamp(time.toEpochMilli());
        expected.setShopId(shopId);
        expected.setPartyId(partyId);

        CascasdingTemplateEmulation input = new CascasdingTemplateEmulation()
                .setTemplate(new Template()
                        .setId(templateId)
                        .setTemplate(template.getBytes()))
                .setRef(new TemplateReference()
                        .setTemplateId(templateId)
                        .setPartyId(partyId)
                        .setShopId(shopId)
                        .setIsGlobal(true))
                .setRuleSetTimestamp(time.toString());

        assertEquals(expected, converter.convert(input));
    }

    @Test
    void convert() {
        CascadingTemplateDto expected = new CascadingTemplateDto();
        CascasdingTemplateEmulation input = new CascasdingTemplateEmulation();
        assertEquals(expected, converter.convert(input));
    }
}
