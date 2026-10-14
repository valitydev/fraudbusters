package dev.vality.fraudbusters.serde;

import dev.vality.fraudbusters.domain.FraudResult;
import dev.vality.fraudo.constant.ResultStatus;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FraudResultJsonCompatibilityTest {

    private static final String LEGACY_JSON = """
            {
              "fraudRequest": {
                "fraudModel": {"amount": 1200, "shopId": "shop", "mobile": true, "recurrent": false},
                "metadata": {"timestamp": 1760000000000, "currency": "RUB", "invoiceId": "invoice"}
              },
              "resultModel": {
                "resultModel": {"resultStatus": "DECLINE", "ruleChecked": "limit", "notificationsRule": []},
                "checkedTemplate": "template"
              }
            }
            """;

    @Test
    void readsExistingKafkaMessages() {
        FraudResult result = new FraudResultDeserializer().deserialize("result", legacyMessage());

        assertNotNull(result);
        assertEquals(1200L, result.getFraudRequest().getFraudModel().getAmount());
        assertEquals("RUB", result.getFraudRequest().getMetadata().getCurrency());
        assertEquals(ResultStatus.DECLINE, result.getResultModel().getResultModel().getResultStatus());
        assertEquals("template", result.getResultModel().getCheckedTemplate());
    }

    @Test
    void roundTripsMessagesThroughJackson3KafkaSerializer() {
        var deserializer = new FraudResultDeserializer();
        FraudResult expected = deserializer.deserialize("result", legacyMessage());

        try (var serializer = new JacksonJsonSerializer<FraudResult>()) {
            byte[] data = serializer.serialize("result", expected);
            assertEquals(expected, deserializer.deserialize("result", data));
            var mapper = JsonMapper.builder().build();
            assertEquals(mapper.readTree(LEGACY_JSON).get("resultModel"), mapper.readTree(data).get("resultModel"));
        }
    }

    @Test
    void keepsInvalidMessageHandling() {
        assertNull(new FraudResultDeserializer().deserialize("result", "invalid".getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] legacyMessage() {
        return LEGACY_JSON.getBytes(StandardCharsets.UTF_8);
    }
}
