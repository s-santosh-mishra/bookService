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
});
