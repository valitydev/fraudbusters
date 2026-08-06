import { check, group } from "k6";
import { cleanupRule, provisionRule } from "./lib/provision.js";
import { ingestPayments, inspectPayment, requireStatus } from "./lib/client.js";
import { inspectionRequest, paymentChangeRequest } from "./lib/payloads.js";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ["rate==1"],
    http_req_failed: ["rate==0"],
    "http_req_duration{endpoint:inspect-payment}": ["p(95)<1000"],
  },
};

export function setup() {
  return provisionRule("k6-smoke");
}

export default function (state) {
  group("configured rule returns fatal", () => {
    const response = requireStatus(
      inspectPayment(inspectionRequest(state.partyId, state.shopId, 100)),
      200,
      "inspect fatal payment",
    );
    check(response, { "risk score is fatal": (r) => r.json("result") === "fatal" });
  });

  group("configured rule leaves low amount at default", () => {
    const response = requireStatus(
      inspectPayment(inspectionRequest(state.partyId, state.shopId, 10)),
      200,
      "inspect default payment",
    );
    check(response, { "risk score is high": (r) => r.json("result") === "high" });
  });

  group("payment event reaches runtime ingestion API", () => {
    requireStatus(
      ingestPayments(paymentChangeRequest(state.partyId, state.shopId, 100, "processed")),
      [200, 201],
      "ingest payment",
    );
  });
}

export function teardown(state) {
  cleanupRule(state);
}

