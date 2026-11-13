const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

describe("Worker Settlement Management Frontend Route & Workflow Tests", () => {
  const jsSource = fs.readFileSync(
    path.join(__dirname, "WorkerSettlementManagement.js"),
    "utf-8"
  );

  it("defines correct base URL matching AdminWorkerSettlementController", () => {
    assert.match(
      jsSource,
      /const SETTLEMENT_API_URL = `\${API_ROOT}\/api\/admin\/settlements`;/,
      "SETTLEMENT_API_URL must match Spring Boot controller @RequestMapping('/api/admin/settlements')"
    );
  });

  it("contains all required endpoint paths and HTTP methods", () => {
    // 1. Summary: GET /summary
    assert.match(
      jsSource,
      /settlementRequest\(\s*["']\/summary["']\s*\)/,
      "Must fetch GET /summary"
    );

    // 2. Batches: GET /batches
    assert.match(
      jsSource,
      /settlementRequest\(\s*["']\/batches["']\s*\)/,
      "Must fetch GET /batches"
    );

    // 3. Batch payments: GET /batches/:businessDate/payments
    assert.match(
      jsSource,
      /settlementRequest\([\s\S]*?`\/batches\/\${encodeURIComponent\(businessDate\)}\/payments`/,
      "Must fetch GET /batches/:businessDate/payments"
    );

    // 4. History: GET /api/admin/settlements
    assert.match(
      jsSource,
      /settlementRequest\(\s*suffix\s*\)/,
      "Must fetch GET /api/admin/settlements with query suffix"
    );

    // 5. Create Settlement: POST /api/admin/settlements
    assert.match(
      jsSource,
      /settlementRequest\(\s*["']["'],\s*\{\s*method:\s*["']POST["']/,
      "Must send POST for creating settlements"
    );

    // 6. Details: GET /:settlementId
    assert.match(
      jsSource,
      /settlementRequest\(\s*`\/\${encodeURIComponent\(settlementId\)}`\s*\)/,
      "Must fetch GET /:settlementId"
    );

    // 7. Status update: PATCH /:settlementId/:action (processing, settled, failed)
    assert.match(
      jsSource,
      /let path = `\/\${encodeURIComponent\(settlementId\)}\/\${action}`;/,
      "Must route PATCH to /:settlementId/:action"
    );
    assert.match(
      jsSource,
      /method:\s*["']PATCH["']/,
      "Must use HTTP PATCH for settlement status updates"
    );

    // 8. Reversal: POST /:settlementId/reversal
    assert.match(
      jsSource,
      /`\/\${encodeURIComponent\(activeSettlementId\)}\/reversal`/,
      "Must route reversal to /:settlementId/reversal"
    );
    assert.match(
      jsSource,
      /method:\s*["']POST["']/,
      "Must use HTTP POST for recording reversal"
    );
  });

  it("supports clicking pending settlement row or view details button to open interface", () => {
    assert.match(
      jsSource,
      /tr\[data-settlement-id\]/,
      "Table rows must carry data-settlement-id for row clickability"
    );
    assert.match(
      jsSource,
      /\.view-settlement-button,\s*tr\[data-settlement-id\]/,
      "Click listener must trigger on row or view details button"
    );
  });

  it("handles failed details requests with meaningful error display", () => {
    assert.match(
      jsSource,
      /settlementDetailsContent\.innerHTML = `[\s\S]*?Unable to load settlement details[\s\S]*?`/,
      "Must render error message in modal content when settlement request fails"
    );
  });

  it("only logs out the admin on 401, not on 403", () => {
    // Must NOT contain the old pattern that treats 403 the same as 401
    assert.doesNotMatch(
      jsSource,
      /response\.status\s*===\s*401\s*\|\|\s*response\.status\s*===\s*403[\s\S]*?sessionStorage\.removeItem/,
      "settlementRequest must NOT clear session on 403 — only on 401"
    );

    // Must redirect to login page on 401
    assert.match(
      jsSource,
      /response\.status\s*===\s*401[\s\S]*?window\.location\.href\s*=\s*["']\/Admin\/adminLogin\.html["']/,
      "Must redirect to login on 401"
    );

    // Must clear session storage items on 401
    assert.match(
      jsSource,
      /response\.status\s*===\s*401[\s\S]*?sessionStorage\.removeItem\(\s*["']servicehub_access_token["']\s*\)/,
      "Must clear access token on 401"
    );
  });

  it("displays error message for 403 without destroying admin session", () => {
    // After the 401 check, the code should fall through to the generic !response.ok handler
    // which throws an Error with the response message but does NOT clear sessionStorage
    assert.match(
      jsSource,
      /if\s*\(\s*!response\.ok\s*\)\s*\{[\s\S]*?throw new Error\(message\)/,
      "Non-2xx responses (including 403) must throw Error with the response message"
    );

    // The generic !response.ok handler must NOT clear sessionStorage
    const genericErrorBlock = jsSource.match(
      /if\s*\(\s*!response\.ok\s*\)\s*\{([\s\S]*?)throw new Error/
    );
    assert.ok(genericErrorBlock, "Must have a generic !response.ok error handler");
    assert.ok(
      !genericErrorBlock[1].includes("sessionStorage.removeItem"),
      "Generic error handler must NOT clear sessionStorage"
    );
    assert.ok(
      !genericErrorBlock[1].includes("window.location.href"),
      "Generic error handler must NOT redirect to login"
    );
  });

  it("restores loading state after settlement creation failure", () => {
    // createSettlement must have a finally block that restores the button
    assert.match(
      jsSource,
      /async function createSettlement\(\)[\s\S]*?finally\s*\{/,
      "createSettlement must use try/finally to always restore button state"
    );
  });

  it("preserves session on 400 validation error and 500 server error", () => {
    // Neither 400 nor 500 should ever clear session storage
    assert.doesNotMatch(
      jsSource,
      /response\.status\s*===\s*400[\s\S]*?sessionStorage\.removeItem/,
      "400 Bad Request must not clear session"
    );
    assert.doesNotMatch(
      jsSource,
      /response\.status\s*===\s*500[\s\S]*?sessionStorage\.removeItem/,
      "500 Internal Server Error must not clear session"
    );
  });

  it("leaves confirmation modal open and displays error inside modal on creation failure", () => {
    // On catch in createSettlement, error is rendered in createSettlementError alert and modal is NOT closed
    assert.match(
      jsSource,
      /createSettlementError/,
      "Must render inline error alert inside the settlement modal"
    );
    assert.match(
      jsSource,
      /catch\s*\(\s*error\s*\)\s*\{[\s\S]*?return;\s*\}\s*finally[\s\S]*?closeModal\(\s*["']createSettlementModal["']\s*\)/,
      "closeModal must only be called AFTER successful creation, not in error catch"
    );
  });

  it("refreshes dashboard and closes modal on successful creation", () => {
    assert.match(
      jsSource,
      /closeModal\(\s*["']createSettlementModal["']\s*\);[\s\S]*?closeModal\(\s*["']paymentSelectionModal["']\s*\);[\s\S]*?refreshDashboard\(\)/,
      "Successful settlement creation must close modals and refresh dashboard"
    );
  });
});
