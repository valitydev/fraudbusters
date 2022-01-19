package dev.vality.fraudbusters.service.template.insert.withdrawal;

import dev.vality.fraudbusters.domain.dgraph.common.DgraphWithdrawal;
import dev.vality.fraudbusters.service.template.AbstractDgraphTemplateService;
import dev.vality.fraudbusters.service.template.TemplateService;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

@Service
public class InsertWithdrawalQueryTemplateService
        extends AbstractDgraphTemplateService implements TemplateService<DgraphWithdrawal> {

    private static final String VELOCITY_VARIABLE_NAME = "withdrawal";
    private static final String VELOCITY_TEMPLATE = "vm/insert/withdrawal/insert_withdrawal_to_dgraph.vm";

    public InsertWithdrawalQueryTemplateService(VelocityEngine velocityEngine) {
        super(velocityEngine);
    }

    @Override
    public String build(DgraphWithdrawal dgraphWithdrawal) {
        return buildDgraphTemplate(VELOCITY_TEMPLATE, VELOCITY_VARIABLE_NAME, dgraphWithdrawal);
    }
}
