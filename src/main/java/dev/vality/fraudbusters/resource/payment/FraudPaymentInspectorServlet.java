package dev.vality.fraudbusters.resource.payment;

import dev.vality.damsel.proxy_inspector.InspectorProxySrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import lombok.RequiredArgsConstructor;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/fraud_inspector/v1")
@RequiredArgsConstructor
public class FraudPaymentInspectorServlet extends GenericServlet {

    private final InspectorProxySrv.Iface fraudInspectorHandler;
    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(InspectorProxySrv.Iface.class, fraudInspectorHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
