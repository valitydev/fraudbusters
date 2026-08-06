import { check } from "k6";
import { config } from "./config.js";
import { inspectPayment } from "./lib/client.js";
import { inspectionRequest } from "./lib/payloads.js";

const rate = Number(__ENV.FB_RATE || "20");
const duration = __ENV.FB_DURATION || "2m";
const preAllocatedVUs = Number(__ENV.FB_PREALLOCATED_VUS || "20");
const maxVUs = Number(__ENV.FB_MAX_VUS || "100");

export const options = {
  scenarios: {
    inspections: {
      executor: "constant-arrival-rate",
      rate,
      timeUnit: "1s",
      duration,
      preAllocatedVUs,
      maxVUs,
      gracefulStop: "10s",
    },
  },
  thresholds: {
    checks: ["rate>0.99"],
    http_req_failed: ["rate<0.01"],
    "http_req_duration{endpoint:inspect-payment}": ["p(95)<500", "p(99)<1000"],
  },
};

export default function () {
  const response = inspectPayment(inspectionRequest(config.partyId, config.shopId, 100));
  check(response, {
    "inspect status is 200": (r) => r.status === 200,
    "risk score is returned": (r) => ["low", "high", "fatal"].includes(r.json("result")),
  });
}

