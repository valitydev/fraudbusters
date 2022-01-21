package dev.vality.fraudbusters.service.template.insert.payment;

import dev.vality.fraudbusters.domain.dgraph.common.DgraphPayment;
import dev.vality.fraudbusters.service.template.AbstractDgraphTemplateService;
import dev.vality.fraudbusters.service.template.TemplateService;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

@Service
public class InsertPaymentQueryTemplateService
        extends AbstractDgraphTemplateService implements TemplateService<DgraphPayment> {

    private static final String VELOCITY_VARIABLE_NAME = "payment";
    private static final String VELOCITY_TEMPLATE = "vm/insert/payment/insert_payment_to_dgraph.vm";

    public InsertPaymentQueryTemplateService(VelocityEngine velocityEngine) {
        super(velocityEngine);
    }

    @Override
    public String build(DgraphPayment dgraphPayment) {
        return buildDgraphTemplate(VELOCITY_TEMPLATE, VELOCITY_VARIABLE_NAME, dgraphPayment);

    }

}
