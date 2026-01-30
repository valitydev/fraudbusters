package dev.vality.fraudbusters.resource.payment;

import dev.vality.damsel.fraudbusters.InspectorServiceSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@WebServlet("/inspector/v1/")
@RequiredArgsConstructor
public class InspectorServiceServlet extends GenericServlet {

    private final InspectorServiceSrv.Iface inspectorServiceHandler;
    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(InspectorServiceSrv.Iface.class, inspectorServiceHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
