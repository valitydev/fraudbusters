package dev.vality.fraudbusters.extension;

import dev.vality.clickhouse.initializer.ChInitializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ClickHouseContainer;

import java.util.List;

@Slf4j
public class ClickHouseContainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String VERSION = "clickhouse/clickhouse-server:22.3.4";

    public static ClickHouseContainer CLICKHOUSE_CONTAINER;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        CLICKHOUSE_CONTAINER = new ClickHouseContainer(VERSION);
        CLICKHOUSE_CONTAINER.start();
        log.info("{}", CLICKHOUSE_CONTAINER.getFirstMappedPort());
        ChInitializer.initAllScripts(CLICKHOUSE_CONTAINER, List.of(
                "sql/db_init.sql",
                "sql/V3__create_fraud_payments.sql",
                "sql/V4__create_payment.sql",
                "sql/V5__add_fields.sql",
                "sql/V6__add_result_fields_payment.sql",
                "sql/V7__add_fields.sql",
                "sql/V8__create_withdrawal.sql",
                "sql/V9__add_phone_category_card.sql",
                "sql/V10__add_id_inspect_result.sql"
        ));
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        CLICKHOUSE_CONTAINER.stop();
    }
}
