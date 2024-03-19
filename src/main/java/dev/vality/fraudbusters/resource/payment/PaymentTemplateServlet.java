package dev.vality.fraudbusters.resource.payment;

import dev.vality.damsel.fraudbusters.PaymentServiceSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@WebServlet("/fraud_payment_validator/v1/")
@RequiredArgsConstructor
public class PaymentTemplateServlet extends GenericServlet {

    private final PaymentServiceSrv.Iface paymentHandler;
    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(PaymentServiceSrv.Iface.class, paymentHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
