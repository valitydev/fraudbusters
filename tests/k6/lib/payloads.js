function randomSuffix() {
  return `${__VU}-${__ITER}-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

export function payment(partyId, shopId, amount = 100, overrides = {}) {
  const suffix = randomSuffix();
  return {
    id: overrides.id || `k6-payment-${suffix}`,
    payerType: overrides.payerType || "customer",
    merchant: {
      id: partyId,
      shop: {
        id: shopId,
        name: "k6 shop",
        category: "test",
        location: "test",
      },
    },
    provider: {
      id: "k6-provider",
      terminalId: "k6-terminal",
      country: "RUS",
    },
    paymentResource: {
      type: "bank_card",
      cardToken: overrides.cardToken || `k6-card-${suffix}`,
      lastDigits: "4242",
      bin: "424242",
      countryCode: "RUS",
      bankName: "k6 bank",
      paymentSystem: "visa",
      cardType: "credit",
    },
    cash: {
      amount,
      currency: overrides.currency || "RUB",
    },
    customer: {
      name: "k6 customer",
      device: {
        ip: overrides.ip || "192.0.2.10",
        fingerprint: overrides.fingerprint || `k6-fingerprint-${suffix}`,
      },
      contact: {
        email: overrides.email || `k6-${suffix}@example.test`,
        phone: "+70000000000",
      },
    },
    createdAt: new Date().toISOString(),
    description: "k6 fraudbusters test",
  };
}

export function inspectionRequest(partyId, shopId, amount, overrides = {}) {
  return { payment: payment(partyId, shopId, amount, overrides) };
}

export function paymentChangeRequest(partyId, shopId, amount, status = "processed", overrides = {}) {
  return {
    paymentsChanges: [
      {
        payment: payment(partyId, shopId, amount, overrides),
        paymentStatus: status,
        eventTime: new Date().toISOString(),
      },
    ],
  };
}

export function template(id, expression) {
  return {
    id,
    template: expression,
    lastUpdateDate: new Date().toISOString(),
    modifiedByUser: "k6",
  };
}

export function reference(id, partyId, shopId, templateId) {
  return {
    id,
    partyId,
    shopId,
    templateId,
    lastUpdateDate: new Date().toISOString(),
    modifiedByUser: "k6",
  };
}

