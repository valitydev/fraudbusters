package dev.vality.fraudbusters.domain;

import dev.vality.fraudbusters.constant.CommandType;
import dev.vality.fraudbusters.constant.TemplateLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RuleTemplate {

    private CommandType commandType;
    private String globalId;
    private String localId;
    private TemplateLevel lvl;
    private String template;

}
