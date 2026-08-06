import { sleep } from "k6";
import { config } from "../config.js";
import {
  createReferences,
  createTemplate,
  removeReference,
  removeTemplate,
  requireStatus,
} from "./client.js";
import { reference, template } from "./payloads.js";

export function provisionRule(prefix, expression = "rule:k6_amount:amount() >= 20 -> decline;") {
  const runId = `${prefix}-${Date.now()}`;
  const state = {
    templateId: `${runId}-template`,
    referenceId: `${runId}-reference`,
    partyId: `${runId}-party`,
    shopId: `${runId}-shop`,
  };

  requireStatus(createTemplate(template(state.templateId, expression)), [200, 201], "create template");
  requireStatus(
    createReferences([reference(state.referenceId, state.partyId, state.shopId, state.templateId)]),
    [200, 201],
    "create reference",
  );

  sleep(config.activationWaitSeconds);
  return state;
}

export function cleanupRule(state) {
  if (!config.cleanup || !state) {
    return;
  }
  removeReference(state.referenceId);
  removeTemplate(state.templateId);
}

