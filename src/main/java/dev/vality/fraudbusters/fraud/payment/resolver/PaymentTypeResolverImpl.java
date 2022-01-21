package dev.vality.fraudbusters.fraud.payment.resolver;


import com.rbkmoney.fraudo.payment.resolver.PaymentTypeResolver;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import org.springframework.stereotype.Component;

@Component
public class PaymentTypeResolverImpl implements PaymentTypeResolver<PaymentModel> {

    @Override
    public Boolean isMobile(PaymentModel model) {
        return model.isMobile();
    }

    @Override
    public Boolean isRecurrent(PaymentModel model) {
        return model.isRecurrent();
    }

}
