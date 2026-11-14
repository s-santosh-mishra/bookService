const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

describe("Customer Dashboard Service Pricing Visibility Tests", () => {
  const jsSource = fs.readFileSync(
    path.join(__dirname, "CustomerDashboard.js"),
    "utf-8"
  );
  const htmlSource = fs.readFileSync(
    path.join(__dirname, "../HTML/CustomerDashboard.html"),
    "utf-8"
  );

  it("CustomerDashboard.js contains formatServicePrice helper function", () => {
    assert.match(
      jsSource,
      /function formatServicePrice\(value\)/,
      "Must define formatServicePrice function"
    );
  });

  it("createServiceCard extracts and displays both Base Price and Minimum Price", () => {
    assert.match(
      jsSource,
      /service\.basePricePerHour/,
      "Must reference service.basePricePerHour"
    );
    assert.match(
      jsSource,
      /service\.minimumServiceFee/,
      "Must reference service.minimumServiceFee"
    );
    assert.match(
      jsSource,
      /Base Price:/,
      "Must label Base Price clearly"
    );
    assert.match(
      jsSource,
      /Minimum Price:/,
      "Must label Minimum Price clearly"
    );
    assert.match(
      jsSource,
      /\/ hour/,
      "Must specify / hour rate unit"
    );
    assert.match(
      jsSource,
      /₹/,
      "Must use Indian Rupee (₹) symbol"
    );
  });

  it("shared createServiceCard component is used by Popular Services, Category Results, and Search Results", () => {
    // 1. Popular services uses createServiceCard
    assert.match(
      jsSource,
      /popularServicesContainer\.appendChild\(createServiceCard\(service\)\)/,
      "Popular services must use shared createServiceCard"
    );

    // 2. Category services uses createServiceCard
    assert.match(
      jsSource,
      /categoryServicesContainer\.appendChild\(createServiceCard\(service\)\)/,
      "Category results must use shared createServiceCard"
    );

    // 3. Search results uses createServiceCard
    assert.match(
      jsSource,
      /searchResultsContainer\.appendChild\(createServiceCard\(service\)\)/,
      "Search results must use shared createServiceCard"
    );
  });

  it("createBookingModal includes pricing display element", () => {
    assert.match(
      htmlSource,
      /id="createBookingPricing"/,
      "HTML modal must have createBookingPricing element"
    );
    assert.match(
      jsSource,
      /document\.getElementById\("createBookingPricing"\)/,
      "openCreateBooking must update createBookingPricing"
    );
  });

  it("formatServicePrice formats whole numbers, decimals, and null/undefined values correctly", () => {
    // Extract the function implementation from the source code and execute in test sandbox
    const funcMatch = jsSource.match(/function formatServicePrice\(value\) \{[\s\S]*?\n\}/);
    assert.ok(funcMatch, "formatServicePrice function must be extractable");

    const formatServicePrice = new Function(
      `return (${funcMatch[0]});`
    )();

    // Whole numbers (e.g. 150.00 or 150)
    assert.strictEqual(formatServicePrice(150), "150");
    assert.strictEqual(formatServicePrice("150.00"), "150");
    assert.strictEqual(formatServicePrice("2000"), "2,000");

    // Decimals (e.g. 150.50 or 120.75)
    assert.strictEqual(formatServicePrice(150.5), "150.50");
    assert.strictEqual(formatServicePrice("120.75"), "120.75");

    // Null, undefined, empty, NaN
    assert.strictEqual(formatServicePrice(null), null);
    assert.strictEqual(formatServicePrice(undefined), null);
    assert.strictEqual(formatServicePrice(""), null);
    assert.strictEqual(formatServicePrice("invalid-number"), null);
  });

  it("createServiceCard generates valid DOM element with price tags and no undefined/NaN", () => {
    // Create a mock DOM environment
    class MockElement {
      constructor(tag) {
        this.tagName = tag;
        this.dataset = {};
        this.classList = {
          add: () => {},
          remove: () => {},
          contains: () => false
        };
        this.innerHTML = "";
        this.className = "";
      }
    }

    const documentMock = {
      createElement: (tag) => new MockElement(tag)
    };

    // Extract createServiceCard function body
    const cardFuncMatch = jsSource.match(/function createServiceCard\(service\) \{[\s\S]*?\n\}/);
    assert.ok(cardFuncMatch, "createServiceCard function must be extractable");

    const formatFuncMatch = jsSource.match(/function formatServicePrice\(value\) \{[\s\S]*?\n\}/);

    const testScope = new Function(
      "document", "escapeHtml", "getServiceIcon",
      `${formatFuncMatch[0]}
       return (${cardFuncMatch[0]});`
    );

    const createServiceCard = testScope(
      documentMock,
      (str) => str || "",
      () => "fa-wrench"
    );

    // Test with complete service data
    const service1 = {
      serviceId: "srv-001",
      serviceName: "Electrical Repair",
      categoryId: "cat-001",
      categoryName: "Electrician",
      basePricePerHour: 150.00,
      minimumServiceFee: 100.00
    };

    const card1 = createServiceCard(service1);
    assert.ok(card1.innerHTML.includes("Base Price:"), "Card must display Base Price label");
    assert.ok(card1.innerHTML.includes("₹150"), "Card must display formatted ₹150");
    assert.ok(card1.innerHTML.includes("/ hour"), "Card must display / hour unit");
    assert.ok(card1.innerHTML.includes("Minimum Price:"), "Card must display Minimum Price label");
    assert.ok(card1.innerHTML.includes("₹100"), "Card must display formatted ₹100");
    assert.strictEqual(card1.dataset.basePrice, "150");
    assert.strictEqual(card1.dataset.minPrice, "100");
    assert.ok(!card1.innerHTML.includes("undefined"), "Must not contain undefined");
    assert.ok(!card1.innerHTML.includes("NaN"), "Must not contain NaN");
    assert.ok(!card1.innerHTML.includes("null"), "Must not contain null");

    // Test with missing prices (graceful fallback)
    const service2 = {
      serviceId: "srv-002",
      serviceName: "Plumbing Inspection",
      categoryId: "cat-002",
      categoryName: "Plumber",
      basePricePerHour: null,
      minimumServiceFee: null
    };

    const card2 = createServiceCard(service2);
    assert.ok(card2.innerHTML.includes("Base Price:"), "Must still display Base Price label");
    assert.ok(card2.innerHTML.includes("—"), "Must display fallback em-dash when price is null");
    assert.ok(!card2.innerHTML.includes("undefined"), "Must not contain undefined on null price");
    assert.ok(!card2.innerHTML.includes("NaN"), "Must not contain NaN on null price");
    assert.ok(!card2.innerHTML.includes("null"), "Must not contain text 'null'");
  });
});
