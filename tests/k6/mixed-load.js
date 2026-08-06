import { check } from "k6";
import { config } from "./config.js";
import { filterTemplates, ingestPayments, inspectPayment } from "./lib/client.js";
import { inspectionRequest, paymentChangeRequest } from "./lib/payloads.js";

export const options = {
  scenarios: {
    inspections: {
      executor: "constant-arrival-rate",
      exec: "inspection",
      rate: Number(__ENV.FB_INSPECT_RATE || "20"),
      timeUnit: "1s",
      duration: __ENV.FB_DURATION || "2m",
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
    event_ingestion: {
      executor: "constant-arrival-rate",
      exec: "eventIngestion",
      rate: Number(__ENV.FB_INGEST_RATE || "5"),
      timeUnit: "1s",
      duration: __ENV.FB_DURATION || "2m",
      preAllocatedVUs: 5,
      maxVUs: 30,
    },
    management_reads: {
      executor: "constant-vus",
      exec: "managementRead",
      vus: Number(__ENV.FB_MANAGEMENT_VUS || "2"),
      duration: __ENV.FB_DURATION || "2m",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{endpoint:inspect-payment}": ["p(95)<500", "p(99)<1000"],
    "http_req_duration{endpoint:payments}": ["p(95)<1000"],
    "http_req_duration{endpoint:filter-templates}": ["p(95)<1000"],
  },
};

export function inspection() {
  const response = inspectPayment(inspectionRequest(config.partyId, config.shopId, 100));
  check(response, { "inspection succeeds": (r) => r.status === 200 });
}

export function eventIngestion() {
  const response = ingestPayments(paymentChangeRequest(config.partyId, config.shopId, 100, "processed"));
  check(response, { "event ingestion succeeds": (r) => [200, 201].includes(r.status) });
}

export function managementRead() {
  const response = filterTemplates(20);
  check(response, { "management read succeeds": (r) => [200, 201].includes(r.status) });
}

