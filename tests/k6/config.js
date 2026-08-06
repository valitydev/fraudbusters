function env(name, fallback) {
  return __ENV[name] || fallback;
}

export const config = {
  apiBaseUrl: env("FB_API_URL", "http://host.docker.internal:9999"),
  managementBaseUrl: env("FB_MANAGEMENT_URL", "http://host.docker.internal:8085/fb-management/v1"),
  apiToken: env("FB_API_TOKEN", env("FB_TOKEN", "")),
  managementToken: env("FB_MANAGEMENT_TOKEN", env("FB_TOKEN", "")),
  activationWaitSeconds: Number(env("FB_ACTIVATION_WAIT_SECONDS", "5")),
  cleanup: env("FB_CLEANUP", "false") === "true",
  partyId: env("FB_PARTY_ID", "k6-load-party"),
  shopId: env("FB_SHOP_ID", "k6-load-shop"),
  templateId: env("FB_TEMPLATE_ID", "k6-load-template"),
};

export function authHeaders(token) {
  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

