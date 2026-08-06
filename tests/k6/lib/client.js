import http from "k6/http";
import { check, fail } from "k6";
import { authHeaders, config } from "../config.js";

function jsonRequest(method, url, body, token, tags) {
  const params = {
    headers: authHeaders(token),
    tags,
    timeout: "30s",
  };
  return http.request(method, url, body === null ? null : JSON.stringify(body), params);
}

export function inspectPayment(body) {
  return jsonRequest("POST", `${config.apiBaseUrl}/inspect-payment`, body, config.apiToken, {
    service: "fraudbusters-api",
    endpoint: "inspect-payment",
  });
}

export function ingestPayments(body) {
  return jsonRequest("POST", `${config.apiBaseUrl}/payments`, body, config.apiToken, {
    service: "fraudbusters-api",
    endpoint: "payments",
  });
}

export function createTemplate(body) {
  return jsonRequest(
    "POST",
    `${config.managementBaseUrl}/payments-templates`,
    body,
    config.managementToken,
    { service: "fraudbusters-management", endpoint: "create-template" },
  );
}

export function createReferences(body) {
  return jsonRequest(
    "POST",
    `${config.managementBaseUrl}/payments-references`,
    body,
    config.managementToken,
    { service: "fraudbusters-management", endpoint: "create-reference" },
  );
}

export function filterTemplates(size = 20) {
  return jsonRequest(
    "GET",
    `${config.managementBaseUrl}/payments-templates?size=${size}`,
    null,
    config.managementToken,
    { service: "fraudbusters-management", endpoint: "filter-templates" },
  );
}

export function removeTemplate(id) {
  return jsonRequest(
    "DELETE",
    `${config.managementBaseUrl}/payments-templates/${encodeURIComponent(id)}`,
    null,
    config.managementToken,
    { service: "fraudbusters-management", endpoint: "remove-template" },
  );
}

export function removeReference(id) {
  return jsonRequest(
    "DELETE",
    `${config.managementBaseUrl}/payments-references/${encodeURIComponent(id)}`,
    null,
    config.managementToken,
    { service: "fraudbusters-management", endpoint: "remove-reference" },
  );
}

export function requireStatus(response, expected, label) {
  const expectedStatuses = Array.isArray(expected) ? expected : [expected];
  const ok = check(response, {
    [`${label}: expected status`]: (r) => expectedStatuses.includes(r.status),
  });
  if (!ok) {
    fail(`${label} failed: status=${response.status}, body=${response.body}`);
  }
  return response;
}

