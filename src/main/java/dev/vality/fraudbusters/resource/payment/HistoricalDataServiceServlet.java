package dev.vality.fraudbusters.resource.payment;

import dev.vality.damsel.fraudbusters.HistoricalDataServiceSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@WebServlet("/historical_data/v1/")
@RequiredArgsConstructor
public class HistoricalDataServiceServlet extends GenericServlet {

    private final HistoricalDataServiceSrv.Iface paymentHandler;
    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(HistoricalDataServiceSrv.Iface.class, paymentHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
