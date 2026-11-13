const API_ROOT = window.SERVICEHUB_CONFIG ? window.SERVICEHUB_CONFIG.API_BASE_URL : "http://localhost:8080";
const SETTLEMENT_API_URL = `${API_ROOT}/api/admin/settlements`;
const SETTLEMENT_ACCESS_TOKEN_KEY = "servicehub_access_token";

const batchTableBody = document.getElementById("settlementBatchTableBody");
const batchDetailsContent = document.getElementById("settlementBatchDetailsContent");
const historyTableBody = document.getElementById("settlementHistoryTableBody");
const selectionContent = document.getElementById("paymentSelectionContent");
const settlementDetailsContent = document.getElementById("settlementDetailsContent");
const settlementMessage = document.getElementById("settlementMessage");

let settlementBatches = [];
let settlementHistory = [];
let selectedWorker = null;
let selectedBusinessDate = null;
let eligiblePayments = [];
let selectedPaymentIds = new Set();
let activeBatch = null;
let activeSettlementId = null;
let activeStatusAction = null;

document.getElementById("workerSearch").addEventListener("input", renderBatches);
document.getElementById("historyWorkerFilter").addEventListener("change", () => loadHistory());
document.getElementById("historyStatusFilter").addEventListener("change", () => loadHistory());

document.getElementById("clearHistoryFilters").addEventListener("click", () => {
  document.getElementById("historyWorkerFilter").value = "";
  document.getElementById("historyStatusFilter").value = "";
  loadHistory();
});

batchTableBody.addEventListener("click", (event) => {
  const button = event.target.closest(".view-settlement-batch");
  if (button) {
    loadBatchDetails(button.dataset.businessDate);
  }
});

batchDetailsContent.addEventListener("click", (event) => {
  const button = event.target.closest(".select-batch-worker");
  if (button) {
    selectBatchWorker(button.dataset.workerId);
    return;
  }
  const settlementButton = event.target.closest(".view-attempt-settlement");
  if (settlementButton) {
    viewSettlementDetails(settlementButton.dataset.settlementId);
  }
});

historyTableBody.addEventListener("click", (event) => {
  const trigger = event.target.closest(".view-settlement-button, tr[data-settlement-id]");
  if (trigger && trigger.dataset.settlementId) {
    viewSettlementDetails(trigger.dataset.settlementId);
  }
});

selectionContent.addEventListener("change", (event) => {
  if (event.target.id === "selectAllPayments") {
    eligiblePayments.forEach((payment) => {
      if (payment.paymentId) {
        if (event.target.checked) {
          selectedPaymentIds.add(payment.paymentId);
        } else {
          selectedPaymentIds.delete(payment.paymentId);
        }
      }
    });
    renderPaymentSelection();
    return;
  }

  const checkbox = event.target.closest(".eligible-payment-checkbox");
  if (!checkbox) {
    return;
  }
  if (checkbox.checked) {
    selectedPaymentIds.add(checkbox.value);
  } else {
    selectedPaymentIds.delete(checkbox.value);
  }
  updateSelectionSummary();
  updateSelectAllCheckbox();
});

document.getElementById("reviewSettlementButton").addEventListener("click", () => {
  renderCreateConfirmation();
  openModal("createSettlementModal");
});

document.getElementById("confirmCreateSettlement").addEventListener("click", createSettlement);
document.getElementById("settlementStatusForm").addEventListener("submit", submitStatusUpdate);
document.getElementById("settlementReversalForm").addEventListener("submit", submitReversal);

document.querySelectorAll("[data-close-modal]").forEach((button) => {
  button.addEventListener("click", () => closeModal(button.dataset.closeModal));
});

document.querySelectorAll("[data-modal-overlay]").forEach((overlay) => {
  overlay.addEventListener("click", () => closeModal(overlay.dataset.modalOverlay));
});

loadPage();

async function loadPage() {
  try {
    await Promise.all([loadSummary(), loadBatches(), loadHistory()]);
  } catch (error) {
    showMessage(error.message || "Unable to load settlement management.", "error");
  }
}

async function loadSummary() {
  const summary = await settlementRequest("/summary");
  if (!summary) {
    throw new Error("The settlement summary response was empty.");
  }
  document.getElementById("summaryEligibleWorkers").textContent =
    formatCount(summary.workersWithEligiblePayments);
  document.getElementById("summaryEligibleAmount").textContent =
    formatMoney(summary.totalEligibleAmount);
  document.getElementById("summaryPending").textContent =
    formatCount(summary.pendingSettlements);
  document.getElementById("summaryProcessing").textContent =
    formatCount(summary.processingSettlements);
  document.getElementById("summarySettledAmount").textContent =
    formatMoney(summary.successfullySettledAmount);
  document.getElementById("summaryFailed").textContent =
    formatCount(summary.failedSettlements);
}

async function loadBatches() {
  batchTableBody.innerHTML = loadingRow(11, "Loading settlement batches...");
  try {
    const response = await settlementRequest("/batches");
    if (!response || !Array.isArray(response.batches)) {
      throw new Error("The settlement batch response was incomplete.");
    }
    settlementBatches = response.batches;
    renderBatches();
    renderUnclassifiedBatchNotice(response);
    updateHistoryWorkerOptions();
  } catch (error) {
    batchTableBody.innerHTML = emptyRow(
      11,
      error.message || "Unable to load settlement batches.",
      "text-red-400",
    );
    throw error;
  }
}

function renderBatches() {
  const search = document.getElementById("workerSearch").value.trim().toLowerCase();
  const batches = settlementBatches.filter((batch) => {
    const fields = [
      batch.businessDate,
      formatBusinessDate(batch.businessDate),
      ...batch.workers.map((worker) => worker.workerName),
      ...batch.workers.map((worker) => worker.workerId),
    ];
    return fields.some((field) =>
      String(field || "").toLowerCase().includes(search),
    );
  });

  if (!batches.length) {
    batchTableBody.innerHTML = emptyRow(
      11,
      settlementBatches.length
        ? "No batches or workers match your search."
        : "No dated online payment batches are available.",
    );
    return;
  }

  batchTableBody.innerHTML = batches
    .map(
      (batch) => `
        <tr class="border-b border-gray-800 last:border-0 transition-colors hover:bg-gray-800/40">
          <td class="whitespace-nowrap px-5 py-4 font-medium text-gray-200">${escapeHtml(formatBusinessDate(batch.businessDate))}</td>
          <td class="px-5 py-4">${getBatchStatusBadge(batch.status)}</td>
          <td class="px-5 py-4 text-sm text-gray-300">${escapeHtml(formatCount(batch.batchPaymentCount))} payments · ${escapeHtml(formatCount(batch.eligibleWorkerCount))} workers</td>
          <td class="whitespace-nowrap px-5 py-4 text-gray-200">${escapeHtml(formatMoney(batch.batchPaymentAmount))}</td>
          <td class="whitespace-nowrap px-5 py-4 text-gray-200">${escapeHtml(formatCount(batch.eligiblePaymentCount))} · ${escapeHtml(formatMoney(batch.eligibleAmount))}</td>
          <td class="px-5 py-4 text-gray-300">${escapeHtml(formatCount(batch.settlementsCreatedCount))}</td>
          <td class="whitespace-nowrap px-5 py-4 text-emerald-300">${escapeHtml(formatMoney(batch.successfullySettledAmount))}</td>
          <td class="whitespace-nowrap px-5 py-4 text-amber-300">${escapeHtml(formatMoney(batch.failedSettlementAmount))}</td>
          <td class="whitespace-nowrap px-5 py-4 text-sky-300">${escapeHtml(formatMoney(batch.processingAmount))}</td>
          <td class="whitespace-nowrap px-5 py-4 font-medium text-gray-200">${escapeHtml(formatMoney(batch.remainingEligibleAmount))}</td>
          <td class="px-5 py-4 text-right">
            <button type="button" class="view-settlement-batch whitespace-nowrap rounded-lg bg-violet-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-500" data-business-date="${escapeHtml(batch.businessDate || "")}">
              <i class="fa-solid fa-eye mr-1"></i>
              View Batch
            </button>
          </td>
        </tr>
      `,
    )
    .join("");
}

function renderUnclassifiedBatchNotice(response) {
  const notice = document.getElementById("unclassifiedBatchNotice");
  const paymentCount = Number(response.unclassifiedSuccessfulPaymentCount || 0);
  const settlementCount = Number(response.unclassifiedSettlementCount || 0);
  if (paymentCount === 0 && settlementCount === 0) {
    notice.classList.add("hidden");
    notice.textContent = "";
    return;
  }
  notice.classList.remove("hidden");
  notice.textContent =
    `${formatCount(paymentCount)} successful online payments have no paidAt timestamp (${formatMoney(response.unclassifiedSuccessfulPaymentAmount)}); ${formatCount(settlementCount)} historical settlements have no safely resolved business date. These records are not assigned a guessed date.`;
}

async function loadBatchDetails(businessDate) {
  const summary = settlementBatches.find(
    (batch) => batch.businessDate === businessDate,
  );
  document.getElementById("settlementBatchDetailsTitle").textContent =
    `Batch ${formatBusinessDate(businessDate)}`;
  document.getElementById("settlementBatchDetailsStatus").innerHTML =
    summary ? getBatchStatusBadge(summary.status) : "";
  batchDetailsContent.innerHTML =
    '<p class="py-12 text-center text-gray-400">Loading batch payments...</p>';
  openModal("settlementBatchDetailsModal");
  try {
    activeBatch = await settlementRequest(
      `/batches/${encodeURIComponent(businessDate)}/payments`,
    );
    if (!activeBatch || !Array.isArray(activeBatch.payments)) {
      throw new Error("The batch payment response was incomplete.");
    }
    renderBatchDetails(activeBatch, summary);
  } catch (error) {
    batchDetailsContent.innerHTML = `
      <p class="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
        ${escapeHtml(error.message || "Unable to load batch payments.")}
      </p>
    `;
    showMessage(error.message || "Unable to load batch payments.", "error");
  }
}

function renderBatchDetails(batch, summary) {
  const workers = summary?.workers || [];
  const eligibleByWorker = new Map();
  batch.payments
    .filter((payment) => payment.eligibleForSettlement)
    .forEach((payment) => {
      eligibleByWorker.set(
        payment.workerId,
        (eligibleByWorker.get(payment.workerId) || 0) + 1,
      );
    });
  const workerActions = workers.length
    ? workers
        .map((worker) => {
          const eligibleCount = eligibleByWorker.get(worker.workerId) || 0;
          return `
            <article class="rounded-xl border border-gray-800 bg-gray-950/60 p-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p class="font-medium text-white">${escapeHtml(worker.workerName || "Worker")}</p>
                  <p class="mt-1 text-sm text-gray-400">${escapeHtml(formatCount(worker.eligiblePaymentCount))} eligible · ${escapeHtml(formatMoney(worker.eligibleAmount))}</p>
                  <p class="mt-1 text-xs text-gray-500">${escapeHtml(worker.bankName || "—")} · ${escapeHtml(worker.maskedBankAccountNumber || "—")}</p>
                </div>
                <button type="button" class="select-batch-worker shrink-0 rounded-lg bg-violet-600 px-3 py-2 text-sm font-medium text-white hover:bg-violet-500 disabled:cursor-not-allowed disabled:opacity-50" data-worker-id="${escapeHtml(worker.workerId)}" ${!batch.closedForSettlement || eligibleCount === 0 ? "disabled" : ""}>
                  Select Payments
                </button>
              </div>
            </article>
          `;
        })
        .join("")
    : '<p class="rounded-xl border border-gray-800 bg-gray-950/60 p-4 text-sm text-gray-400">No payments are currently eligible for a new settlement in this batch.</p>';

  const rows = batch.payments.length
    ? batch.payments.map(renderBatchPaymentRow).join("")
    : '<tr><td colspan="7" class="px-5 py-10 text-center text-gray-500">No successful online payments belong to this business date.</td></tr>';
  batchDetailsContent.innerHTML = `
    <div class="space-y-7">
      <div class="rounded-xl border ${batch.closedForSettlement ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-100" : "border-amber-500/30 bg-amber-500/10 text-amber-100"} p-4 text-sm">
        ${batch.closedForSettlement
          ? "This batch is closed. Eligible payments can be selected for separate worker settlements."
          : "This batch is accumulating and cannot be settled until its business date reaches 8:00 PM IST."}
      </div>
      <section>
        <h3 class="mb-3 text-lg font-semibold">Workers in this batch</h3>
        <div class="grid gap-3 lg:grid-cols-2">${workerActions}</div>
      </section>
      <section>
        <div class="mb-3 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h3 class="text-lg font-semibold">Payment and Settlement Attempt History</h3>
            <p class="mt-1 text-sm text-gray-500">Payment records remain unchanged; each retry creates a separate settlement attempt.</p>
          </div>
          <span class="text-sm text-gray-400">${escapeHtml(formatCount(batch.payments.length))} payments</span>
        </div>
        <div class="overflow-x-auto rounded-xl border border-gray-800">
          <table class="w-full" style="min-width: 1180px">
            <thead><tr class="border-b border-gray-800 bg-gray-950/60 text-left text-xs uppercase tracking-wider text-gray-500">
              <th class="px-4 py-3">Worker</th><th class="px-4 py-3">Payment / Booking</th><th class="px-4 py-3">Paid At (IST)</th><th class="px-4 py-3">Amount / Method</th><th class="px-4 py-3">Booking</th><th class="px-4 py-3">Current Eligibility</th><th class="px-4 py-3">Settlement Attempts</th>
            </tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
      </section>
    </div>
  `;
}

function renderBatchPaymentRow(payment) {
  const attempts = payment.settlementAttempts || [];
  const attemptsText = attempts.length
    ? attempts.map((attempt) => `
        <div class="mb-2 last:mb-0 rounded-lg border border-gray-800 bg-gray-950/60 p-2">
          <p class="font-medium text-gray-200">${escapeHtml(formatStatus(attempt.status))} · ${escapeHtml(formatMoney(attempt.amount))}</p>
          <p class="mt-1 break-all text-xs text-gray-500">Settlement ${escapeHtml(attempt.settlementId || "—")} · ${escapeHtml(formatDateTime(attempt.createdAt))}</p>
          ${attempt.paymentReference ? `<p class="mt-1 break-all text-xs text-gray-400">Reference: ${escapeHtml(attempt.paymentReference)}</p>` : ""}
          ${attempt.failureReason ? `<p class="mt-1 text-xs text-red-300">Failure: ${escapeHtml(attempt.failureReason)}</p>` : ""}
          ${attempt.reversalReference ? `<p class="mt-1 break-all text-xs text-emerald-300">Reversed: ${escapeHtml(attempt.reversalReference)} · ${escapeHtml(formatDateTime(attempt.reversalVerifiedAt))} · ${escapeHtml(attempt.reversalVerifiedByAdmin || "Admin")}</p>` : ""}
          <button type="button" class="view-attempt-settlement mt-2 text-xs text-violet-300 hover:text-violet-200" data-settlement-id="${escapeHtml(attempt.settlementId || "")}">View full settlement details</button>
        </div>
      `).join("")
    : '<span class="text-xs text-gray-500">No settlement attempts</span>';
  return `
    <tr class="border-b border-gray-800 last:border-0 align-top">
      <td class="px-4 py-3 text-sm text-gray-200">${escapeHtml(payment.workerName || "—")}</td>
      <td class="px-4 py-3"><p class="break-all text-xs text-gray-300">${escapeHtml(payment.paymentId || "—")}</p><p class="mt-1 break-all text-xs text-gray-500">Booking ${escapeHtml(payment.bookingId || "—")}</p></td>
      <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-400">${escapeHtml(formatDateTimeIst(payment.paidAt))}</td>
      <td class="whitespace-nowrap px-4 py-3"><p class="text-sm text-gray-200">${escapeHtml(formatMoney(payment.amount))}</p><p class="mt-1 text-xs text-gray-500">${escapeHtml(formatStatus(payment.paymentMethod))} · ${escapeHtml(formatStatus(payment.paymentStatus))}</p></td>
      <td class="px-4 py-3 text-sm text-gray-300">${escapeHtml(formatStatus(payment.bookingStatus))}</td>
      <td class="px-4 py-3 text-sm ${payment.eligibleForSettlement && activeBatch?.closedForSettlement ? "text-emerald-300" : "text-amber-300"}">${payment.eligibleForSettlement ? activeBatch?.closedForSettlement ? "Eligible now" : "Eligible after batch closes" : payment.blockingSettlementId ? `Blocked by ${escapeHtml(payment.blockingSettlementId)}` : "Not eligible"}</td>
      <td class="max-w-lg px-4 py-3">${attemptsText}</td>
    </tr>
  `;
}

function selectBatchWorker(workerId) {
  if (!activeBatch?.closedForSettlement) {
    showMessage("This batch is not closed for settlement yet.", "error");
    return;
  }
  const summary = settlementBatches.find(
    (batch) => batch.businessDate === activeBatch.businessDate,
  );
  selectedWorker = summary?.workers.find(
    (worker) => worker.workerId === workerId,
  );
  if (!selectedWorker) {
    showMessage("Worker details are no longer available. Refresh the batches and try again.", "error");
    return;
  }
  selectedBusinessDate = activeBatch.businessDate;
  selectedPaymentIds = new Set();
  eligiblePayments = activeBatch.payments
    .filter(
      (payment) =>
        payment.workerId === workerId && payment.eligibleForSettlement,
    )
    .map((payment) => ({
      paymentId: payment.paymentId,
      bookingId: payment.bookingId,
      paymentAmount: payment.amount,
      paymentDate: payment.paidAt,
      paymentCreatedAt: payment.paymentCreatedAt,
      paymentMethod: payment.paymentMethod,
      paymentStatus: payment.paymentStatus,
      bookingStatus: payment.bookingStatus,
    }));
  document.getElementById("paymentSelectionWorkerName").textContent =
    `${selectedWorker.workerName || "Worker"} · ${formatBusinessDate(selectedBusinessDate)}`;
  document.getElementById("reviewSettlementButton").disabled = true;
  updateSelectionSummary();
  renderPaymentSelection();
  closeModal("settlementBatchDetailsModal");
  openModal("paymentSelectionModal");
}

function renderPaymentSelection() {
  const worker = selectedWorker;
  const bankDetails = `
    <section class="mb-6">
      <h3 class="mb-3 text-sm font-semibold text-gray-300">Transfer Destination (Worker Bank Details)</h3>
      <div class="grid gap-3 rounded-xl border border-gray-800 bg-gray-950/60 p-4 sm:grid-cols-2 lg:grid-cols-4">
        ${createDetailItem("Account holder", worker?.bankAccountHolderName)}
        ${createDetailItem("Account number", worker?.maskedBankAccountNumber)}
        ${createDetailItem("IFSC", worker?.bankIfscCode)}
        ${createDetailItem("Bank", worker?.bankName)}
      </div>
      <p class="mt-3 text-xs text-gray-500">The account number is masked. Verify the destination in your banking application before transferring.</p>
    </section>
  `;

  if (eligiblePayments.length === 0) {
    selectionContent.innerHTML = `
      ${bankDetails}
      <div class="rounded-xl border border-gray-800 bg-gray-950/60 p-8 text-center text-gray-400">
        No eligible payments are currently available for this worker.
      </div>
    `;
    updateSelectionSummary();
    return;
  }

  selectionContent.innerHTML = `
    ${bankDetails}
    <section>
      <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="font-semibold text-white">Eligible Payments for ${escapeHtml(formatBusinessDate(selectedBusinessDate))}</h3>
          <p class="mt-1 text-sm text-gray-500">Only this worker and business date can be included in this settlement.</p>
        </div>
        <label class="inline-flex items-center gap-2 text-sm text-gray-300">
          <input id="selectAllPayments" type="checkbox" class="h-4 w-4 rounded border-gray-700 bg-gray-950 text-violet-500 focus:ring-violet-500" />
          Select all
        </label>
      </div>
      <div class="overflow-x-auto rounded-xl border border-gray-800">
        <table class="w-full" style="min-width: 860px">
          <thead>
            <tr class="border-b border-gray-800 bg-gray-950/60 text-left text-xs uppercase tracking-wider text-gray-500">
              <th class="px-4 py-3"><span class="sr-only">Select</span></th>
              <th class="px-4 py-3">Payment / Booking</th>
              <th class="px-4 py-3">Booking Status</th>
              <th class="px-4 py-3">Payment Created</th>
              <th class="px-4 py-3">Paid At</th>
              <th class="px-4 py-3">Amount</th>
            </tr>
          </thead>
          <tbody>
            ${eligiblePayments.map(renderEligiblePaymentRow).join("")}
          </tbody>
        </table>
      </div>
    </section>
  `;
  eligiblePayments.forEach((payment) => {
    const checkbox = selectionContent.querySelector(
      `.eligible-payment-checkbox[value="${cssEscape(payment.paymentId || "")}"]`,
    );
    if (checkbox) {
      checkbox.checked = selectedPaymentIds.has(payment.paymentId);
    }
  });
  updateSelectionSummary();
  updateSelectAllCheckbox();
}

function renderEligiblePaymentRow(payment) {
  const id = payment.paymentId || "";
  return `
    <tr class="border-b border-gray-800 last:border-0 hover:bg-gray-800/30">
      <td class="px-4 py-3">
        <input type="checkbox" class="eligible-payment-checkbox h-4 w-4 rounded border-gray-700 bg-gray-950 text-violet-500 focus:ring-violet-500" value="${escapeHtml(id)}" aria-label="Select payment ${escapeHtml(id || "without ID")}" ${id ? "" : "disabled"} />
      </td>
      <td class="px-4 py-3">
        <p class="break-all text-xs text-gray-300">${escapeHtml(id || "Payment ID unavailable")}</p>
        <p class="mt-1 break-all text-xs text-gray-500">Booking ${escapeHtml(payment.bookingId || "—")}</p>
      </td>
      <td class="px-4 py-3 text-sm text-gray-300">${escapeHtml(formatStatus(payment.bookingStatus))}</td>
      <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-400">${escapeHtml(formatDateTime(payment.paymentCreatedAt))}</td>
      <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-400">${escapeHtml(formatDateTimeIst(payment.paymentDate))}</td>
      <td class="whitespace-nowrap px-4 py-3 font-medium text-gray-200">${escapeHtml(formatMoney(payment.paymentAmount))}</td>
    </tr>
  `;
}

function updateSelectionSummary() {
  const selectedPayments = eligiblePayments.filter((payment) =>
    selectedPaymentIds.has(payment.paymentId),
  );
  const selectedCents = selectedPayments.reduce(
    (total, payment) => total + amountToCents(payment.paymentAmount),
    0n,
  );
  document.getElementById("selectionTotal").textContent =
    `${formatCount(selectedPayments.length)} ${selectedPayments.length === 1 ? "payment" : "payments"} selected · ${formatCents(selectedCents)}`;
  document.getElementById("reviewSettlementButton").disabled =
    selectedPayments.length === 0;
}

function updateSelectAllCheckbox() {
  const selectAll = document.getElementById("selectAllPayments");
  if (!selectAll) {
    return;
  }
  const availableIds = eligiblePayments
    .map((payment) => payment.paymentId)
    .filter(Boolean);
  selectAll.checked =
    availableIds.length > 0 &&
    availableIds.every((paymentId) => selectedPaymentIds.has(paymentId));
  selectAll.indeterminate =
    !selectAll.checked &&
    availableIds.some((paymentId) => selectedPaymentIds.has(paymentId));
}

function renderCreateConfirmation() {
  const selected = eligiblePayments.filter((payment) =>
    selectedPaymentIds.has(payment.paymentId),
  );
  const amountCents = selected.reduce(
    (total, payment) => total + amountToCents(payment.paymentAmount),
    0n,
  );
  const worker = selectedWorker || {};
  document.getElementById("createSettlementContent").innerHTML = `
    <div class="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4">
      <p class="font-medium text-amber-200">Manual bank transfer only</p>
      <p class="mt-1 text-sm text-amber-100/80">ServiceHub will not send money. Create this pending record, transfer the amount separately using your banking application, then update the settlement status here.</p>
    </div>
    <div class="grid gap-3 rounded-xl border border-gray-800 bg-gray-950/60 p-4 sm:grid-cols-2">
      ${createDetailItem("Business date (IST)", formatBusinessDate(selectedBusinessDate))}
      ${createDetailItem("Worker", worker.workerName)}
      ${createDetailItem("Selected payments", formatCount(selected.length))}
      ${createDetailItem("Settlement amount", formatCents(amountCents))}
      ${createDetailItem("Bank account", worker.maskedBankAccountNumber)}
      ${createDetailItem("Account holder", worker.bankAccountHolderName)}
      ${createDetailItem("Bank / IFSC", [worker.bankName, worker.bankIfscCode].filter(Boolean).join(" · "))}
    </div>
    <p class="text-sm text-gray-400">The selected payment records will be snapshotted in a new pending settlement for this business date. Existing or failed settlement history will not be changed.</p>
  `;
}

async function createSettlement() {
  if (
    !selectedWorker
    || !selectedBusinessDate
    || !selectedPaymentIds.size
    || !activeBatch?.closedForSettlement
  ) {
    showMessage("Select eligible payments from a closed business-date batch before creating a settlement.", "error");
    return;
  }
  const button = document.getElementById("confirmCreateSettlement");
  const restore = button && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(button, "Creating...")
    : null;
  if (button && window.SERVICEHUB_BUTTON && !restore) return;
  if (!restore && button) {
    button.disabled = true;
    button.textContent = "Creating...";
  }

  try {
    await settlementRequest("", {
      method: "POST",
      body: JSON.stringify({
        workerId: selectedWorker.workerId,
        businessDate: selectedBusinessDate,
        paymentIds: [...selectedPaymentIds],
      }),
    });
  } catch (error) {
    const errorMsg = error.message || "Unable to create the settlement.";
    const existingAlert = document.getElementById("createSettlementError");
    if (existingAlert) {
      existingAlert.innerHTML = `<p class="font-medium text-red-200">Settlement creation failed</p><p class="mt-1">${escapeHtml(errorMsg)}</p>`;
    } else {
      const content = document.getElementById("createSettlementContent");
      if (content) {
        const alertDiv = document.createElement("div");
        alertDiv.id = "createSettlementError";
        alertDiv.className = "rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300";
        alertDiv.innerHTML = `<p class="font-medium text-red-200">Settlement creation failed</p><p class="mt-1">${escapeHtml(errorMsg)}</p>`;
        content.prepend(alertDiv);
      }
    }
    showMessage(errorMsg, "error");
    return;
  } finally {
    if (restore) {
      restore();
    } else if (button) {
      button.disabled = false;
      button.textContent = "Create Pending Settlement";
    }
  }
  closeModal("createSettlementModal");
  closeModal("paymentSelectionModal");
  selectedPaymentIds.clear();
  showMessage("Pending settlement created. Complete the bank transfer outside ServiceHub, then record its result.", "success");
  try {
    await refreshDashboard();
  } catch (error) {
    showMessage(`Settlement created, but dashboard refresh failed: ${error.message}`, "error");
  }
}

async function loadHistory() {
  historyTableBody.innerHTML = loadingRow(8, "Loading settlement history...");
  const params = new URLSearchParams();
  const workerId = document.getElementById("historyWorkerFilter").value;
  const status = document.getElementById("historyStatusFilter").value;
  if (workerId) {
    params.set("workerId", workerId);
  }
  if (status) {
    params.set("status", status);
  }
  const suffix = params.toString() ? `?${params.toString()}` : "";

  try {
    const result = await settlementRequest(suffix);
    if (!Array.isArray(result)) {
      throw new Error("The settlement history response was incomplete.");
    }
    settlementHistory = result;
    renderHistory();
    updateHistoryWorkerOptions();
  } catch (error) {
    historyTableBody.innerHTML = emptyRow(
      8,
      error.message || "Unable to load settlement history.",
      "text-red-400",
    );
    document.getElementById("settlementCount").textContent =
      "History unavailable";
    showMessage(error.message || "Unable to load settlement history.", "error");
  }
}

function renderHistory() {
  document.getElementById("settlementCount").textContent =
    `${formatCount(settlementHistory.length)} ${settlementHistory.length === 1 ? "settlement" : "settlements"}`;

  if (!settlementHistory.length) {
    historyTableBody.innerHTML = emptyRow(8, "No settlements match these filters.");
    return;
  }

  historyTableBody.innerHTML = settlementHistory
    .map(
      (settlement) => `
        <tr class="border-b border-gray-800 last:border-0 transition-colors hover:bg-gray-800/40 cursor-pointer" data-settlement-id="${escapeHtml(settlement.settlementId || "")}">
          <td class="max-w-[190px] break-all px-5 py-4 text-xs text-gray-400">${escapeHtml(settlement.settlementId || "—")}</td>
          <td class="whitespace-nowrap px-5 py-4 text-sm text-gray-300">${escapeHtml(formatBusinessDate(settlement.businessDate))}</td>
          <td class="px-5 py-4">
            <p class="font-medium text-gray-200">${escapeHtml(settlement.workerName || "—")}</p>
            <p class="mt-1 break-all text-xs text-gray-500">${escapeHtml(settlement.workerId || "")}</p>
          </td>
          <td class="px-5 py-4 text-gray-300">${escapeHtml(formatCount(settlement.paymentCount))}</td>
          <td class="whitespace-nowrap px-5 py-4 text-gray-200">${escapeHtml(formatMoney(settlement.amount))}</td>
          <td class="px-5 py-4">${getSettlementStatusBadge(settlement.status)}</td>
          <td class="whitespace-nowrap px-5 py-4 text-sm text-gray-400">${escapeHtml(formatDateTime(settlement.createdAt))}</td>
          <td class="px-5 py-4 text-right">
            <button type="button" class="view-settlement-button whitespace-nowrap rounded-lg bg-gray-800 px-3 py-2 text-sm text-gray-300 transition-colors hover:bg-gray-700 hover:text-white" data-settlement-id="${escapeHtml(settlement.settlementId || "")}" ${settlement.settlementId ? "" : "disabled"}>
              <i class="fa-solid fa-eye mr-1"></i>
              View Details
            </button>
          </td>
        </tr>
      `,
    )
    .join("");
}

function updateHistoryWorkerOptions() {
  const select = document.getElementById("historyWorkerFilter");
  const selectedValue = select.value;
  const selectedLabel =
    select.options[select.selectedIndex]?.textContent || selectedValue;
  const workersById = new Map();
  settlementBatches.forEach((batch) => {
    batch.workers.forEach((worker) => {
      if (worker.workerId) {
        workersById.set(worker.workerId, worker.workerName || worker.workerId);
      }
    });
  });
  settlementHistory.forEach((settlement) => {
    if (settlement.workerId) {
      workersById.set(
        settlement.workerId,
        settlement.workerName || workersById.get(settlement.workerId) || settlement.workerId,
      );
    }
  });
  if (selectedValue && !workersById.has(selectedValue)) {
    workersById.set(selectedValue, selectedLabel);
  }
  select.innerHTML =
    '<option value="">All workers</option>' +
    [...workersById.entries()]
      .sort((left, right) => left[1].localeCompare(right[1]))
      .map(
        ([id, name]) =>
          `<option value="${escapeHtml(id)}">${escapeHtml(name)}</option>`,
      )
      .join("");
  select.value = workersById.has(selectedValue) ? selectedValue : "";
}

async function viewSettlementDetails(settlementId) {
  if (!settlementId) {
    showMessage("Please select a valid settlement record.", "error");
    return;
  }
  activeSettlementId = settlementId;
  document.getElementById("settlementDetailsTitle").textContent =
    "Settlement Details";
  document.getElementById("settlementDetailsStatus").innerHTML = "";
  settlementDetailsContent.innerHTML =
    '<p class="py-12 text-center text-gray-400">Loading settlement details...</p>';
  document.getElementById("settlementDetailsActions").innerHTML = "";
  openModal("settlementDetailsModal");

  try {
    const details = await settlementRequest(`/${encodeURIComponent(settlementId)}`);
    renderSettlementDetails(details);
  } catch (error) {
    settlementDetailsContent.innerHTML = `
      <div class="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
        <p class="font-medium text-red-200">Unable to load settlement details</p>
        <p class="mt-1">${escapeHtml(error.message || "Request failed.")}</p>
      </div>
    `;
    showMessage(error.message || "Unable to load settlement details.", "error");
  }
}

function renderSettlementDetails(details) {
  if (!details || !details.settlementId) {
    throw new Error("Settlement details are incomplete.");
  }
  activeSettlementId = details.settlementId;
  document.getElementById("settlementDetailsTitle").textContent =
    `Settlement ${details.settlementId}`;
  document.getElementById("settlementDetailsStatus").innerHTML =
    getSettlementStatusBadge(details.status);

  const totalMatches = details.paymentItemsTotalMatchesAmount === true;
  const paymentItems = Array.isArray(details.paymentItems)
    ? details.paymentItems
    : [];
  const reversal = details.reversal || null;
  settlementDetailsContent.innerHTML = `
    <div class="space-y-7">
      ${createSettlementStatusNotice(details.status, reversal)}
      ${createDetailsSection("Settlement Information", [
        ["Settlement ID", details.settlementId],
        ["Business date (IST)", formatBusinessDate(details.businessDate)],
        ["Worker", details.workerName],
        ["Worker ID", details.workerId],
        ["Amount", formatMoney(details.amount)],
        ["Status", formatStatus(details.status)],
        ["Payments", formatCount(paymentItems.length)],
        ["Created", formatDateTime(details.createdAt)],
        ["Settled", formatDateTime(details.settledAt)],
        ["Payment reference", details.paymentReference],
        ["Created by admin", details.createdByAdminName],
        ["Admin ID", details.createdByAdminId],
        ["Failure reason", details.failureReason],
      ])}
      ${
        reversal
          ? createDetailsSection("Verified Bank Reversal Audit", [
              ["Bank reversal reference", reversal.bankReversalReference],
              ["Reason", reversal.reversalReason],
              ["Verified at", formatDateTime(reversal.verifiedAt)],
              ["Verified by admin", reversal.verifiedByAdminName],
              ["Verifying admin ID", reversal.verifiedByAdminId],
            ])
          : ""
      }
      ${createDetailsSection("Historical Bank Snapshot", [
        ["Account holder", details.bankAccountHolderName],
        ["Account number", details.maskedBankAccountNumber],
        ["IFSC", details.bankIfscCode],
        ["Bank", details.bankName],
      ])}
      <section>
        <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h3 class="text-lg font-semibold text-white">Included Payments</h3>
          <span class="rounded-full border px-3 py-1 text-xs ${totalMatches ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300" : "border-red-500/30 bg-red-500/10 text-red-300"}">
            Item total ${totalMatches ? "matches" : "does not match"} settlement amount
          </span>
        </div>
        <p class="mb-3 text-sm text-gray-400">Settlement amount: ${escapeHtml(formatMoney(details.amount))} · Payment item total: ${escapeHtml(formatMoney(details.paymentItemsTotal))}</p>
        ${
          paymentItems.length
            ? `<div class="overflow-x-auto rounded-xl border border-gray-800">
                <table class="w-full" style="min-width: 760px">
                  <thead><tr class="border-b border-gray-800 bg-gray-950/60 text-left text-xs uppercase tracking-wider text-gray-500">
                    <th class="px-4 py-3">Payment ID</th><th class="px-4 py-3">Booking ID</th><th class="px-4 py-3">Amount</th><th class="px-4 py-3">Payment Created</th><th class="px-4 py-3">Paid At</th><th class="px-4 py-3">Status</th>
                  </tr></thead>
                  <tbody>${paymentItems.map(renderSettlementItem).join("")}</tbody>
                </table>
              </div>`
            : '<p class="rounded-xl border border-gray-800 bg-gray-950/60 p-4 text-sm text-gray-500">No payment items are attached to this settlement.</p>'
        }
      </section>
      ${
        details.status === "FAILED"
          ? '<p class="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">This failed settlement is historical and cannot be reopened. Retry only by creating a new settlement with currently eligible payments.</p>'
          : ""
      }
    </div>
  `;

  renderSettlementActions(details.status, details.settlementId, reversal !== null);
}

function renderSettlementItem(item) {
  return `
    <tr class="border-b border-gray-800 last:border-0">
      <td class="max-w-[190px] break-all px-4 py-3 text-xs text-gray-400">${escapeHtml(item.paymentId || "—")}</td>
      <td class="max-w-[190px] break-all px-4 py-3 text-xs text-gray-400">${escapeHtml(item.bookingId || "—")}</td>
      <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-200">${escapeHtml(formatMoney(item.amount))}</td>
      <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-400">${escapeHtml(formatDateTime(item.paymentCreatedAt))}</td>
      <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-400">${escapeHtml(formatDateTimeIst(item.paymentDate))}</td>
      <td class="px-4 py-3 text-sm text-gray-300">${escapeHtml(formatStatus(item.paymentStatus))}</td>
    </tr>
  `;
}

function createSettlementStatusNotice(status, reversal) {
  const notices = {
    PENDING: {
      classes: "border-amber-500/30 bg-amber-500/10 text-amber-200",
      text: "Pending: the settlement record is created, but the bank transfer has not been initiated.",
    },
    PROCESSING: {
      classes: "border-sky-500/30 bg-sky-500/10 text-sky-200",
      text: "Processing: the external transfer was initiated or is awaiting bank confirmation. Keep it processing while the outcome is delayed; its payments remain unavailable for another settlement.",
    },
    SETTLED: reversal
      ? {
          classes: "border-emerald-500/30 bg-emerald-500/10 text-emerald-200",
          text: "Settled with a verified bank reversal recorded separately. The original settlement, transaction reference, items, and timestamps remain unchanged; its payments may be eligible for a new settlement unless another active or settled record includes them.",
        }
      : {
      classes: "border-emerald-500/30 bg-emerald-500/10 text-emerald-200",
      text: "Settled: an administrator recorded a confirmed successful transfer. If a worker reports non-receipt, verify the recorded reference with the bank; the report alone does not change this historical record.",
        },
    FAILED: {
      classes: "border-red-500/30 bg-red-500/10 text-red-200",
      text: "Failed: an administrator recorded a confirmed unsuccessful transfer. This record and its items remain in history; eligible payments can be selected for a separate new settlement.",
    },
  };
  const notice = notices[status];
  if (!notice) {
    return "";
  }
  return `
    <p class="rounded-xl border p-4 text-sm ${notice.classes}">
      ${escapeHtml(notice.text)}
    </p>
  `;
}

function renderSettlementActions(status, settlementId, reversalRecorded) {
  const actions = {
    PENDING: [
      actionButton(
        "processing",
        settlementId,
        "Mark Processing",
        "fa-arrows-rotate",
        "bg-sky-600 hover:bg-sky-500",
      ),
    ],
    PROCESSING: [
      actionButton(
        "settled",
        settlementId,
        "Record Transfer Success",
        "fa-circle-check",
        "bg-emerald-600 hover:bg-emerald-500",
      ),
      actionButton(
        "failed",
        settlementId,
        "Record Transfer Failure",
        "fa-circle-xmark",
        "bg-red-600 hover:bg-red-500",
      ),
    ],
  };
  const container = document.getElementById("settlementDetailsActions");
  const buttons = actions[status] || [];
  const reversalButton =
    status === "SETTLED" && !reversalRecorded
      ? `<button type="button" class="record-settlement-reversal rounded-lg bg-red-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-red-500" data-settlement-id="${escapeHtml(settlementId)}"><i class="fa-solid fa-rotate-left mr-1"></i>Record Verified Bank Reversal</button>`
      : "";
  container.innerHTML = `
    <button type="button" data-close-modal="settlementDetailsModal" class="rounded-lg bg-gray-800 px-4 py-2.5 text-sm text-gray-300 hover:bg-gray-700 hover:text-white">Close</button>
    ${buttons.join("")}
    ${reversalButton}
  `;
  container.querySelectorAll(".settlement-status-action").forEach((button) => {
    button.addEventListener("click", () =>
      openStatusModal(button.dataset.action, button.dataset.settlementId),
    );
  });
  container.querySelector('[data-close-modal="settlementDetailsModal"]')
    .addEventListener("click", () => closeModal("settlementDetailsModal"));
  const reversalAction = container.querySelector(".record-settlement-reversal");
  if (reversalAction) {
    reversalAction.addEventListener("click", () =>
      openReversalModal(reversalAction.dataset.settlementId),
    );
  }
}

function actionButton(action, settlementId, label, icon, className) {
  return `
    <button type="button" class="settlement-status-action rounded-lg px-4 py-2.5 text-sm font-medium text-white transition-colors ${className}" data-action="${action}" data-settlement-id="${escapeHtml(settlementId)}">
      <i class="fa-solid ${icon} mr-1"></i>${label}
    </button>
  `;
}

function openStatusModal(action, settlementId) {
  activeStatusAction = { action, settlementId };
  const referenceField = document.getElementById("paymentReferenceField");
  const failureField = document.getElementById("failureReasonField");
  const title = document.getElementById("settlementStatusTitle");
  const prompt = document.getElementById("settlementStatusPrompt");
  const submitButton = document.getElementById("submitSettlementStatus");
  document.getElementById("paymentReferenceInput").value = "";
  document.getElementById("failureReasonInput").value = "";
  referenceField.classList.add("hidden");
  failureField.classList.add("hidden");

  if (action === "processing") {
    title.textContent = "Start Manual Transfer";
    prompt.textContent =
      "Mark this pending settlement as processing when you initiate the bank transfer outside ServiceHub. Leave it processing while awaiting a delayed bank outcome.";
    submitButton.textContent = "Mark Processing";
  } else if (action === "settled") {
    title.textContent = "Record Successful Transfer";
    prompt.textContent =
      "Only record a confirmed successful external bank transfer. If the worker reports non-receipt, verify the reference with the bank before taking further action.";
    referenceField.classList.remove("hidden");
    submitButton.textContent = "Record as Settled";
  } else {
    title.textContent = "Record Failed Transfer";
    prompt.textContent =
      "Record failure only after the bank confirms the transfer was unsuccessful. A delayed transfer should remain processing; this failed record cannot be reopened.";
    failureField.classList.remove("hidden");
    submitButton.textContent = "Record as Failed";
  }
  openModal("settlementStatusModal");
}

async function submitStatusUpdate(event) {
  event.preventDefault();
  if (!activeStatusAction) {
    return;
  }

  const { action, settlementId } = activeStatusAction;
  let path = `/${encodeURIComponent(settlementId)}/${action}`;
  let body;
  if (action === "settled") {
    const paymentReference = document
      .getElementById("paymentReferenceInput")
      .value.trim();
    if (!paymentReference) {
      showMessage("Enter the external bank transaction reference.", "error");
      return;
    }
    if (paymentReference.length > 100) {
      showMessage("The payment reference cannot exceed 100 characters.", "error");
      return;
    }
    body = { paymentReference };
  } else if (action === "failed") {
    const failureReason = document
      .getElementById("failureReasonInput")
      .value.trim();
    if (!failureReason) {
      showMessage("Enter a reason for the failed transfer.", "error");
      return;
    }
    if (failureReason.length > 1000) {
      showMessage("The failure reason cannot exceed 1000 characters.", "error");
      return;
    }
    body = { failureReason };
  }

  const submitButton = document.getElementById("submitSettlementStatus");
  const restore = submitButton && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(submitButton, "Saving...")
    : null;
  if (submitButton && window.SERVICEHUB_BUTTON && !restore) return;
  if (!restore && submitButton) {
    submitButton.disabled = true;
    submitButton.textContent = "Saving...";
  }

  try {
    await settlementRequest(path, {
      method: "PATCH",
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
  } catch (error) {
    showMessage(error.message || "Unable to update settlement status.", "error");
    return;
  } finally {
    if (restore) {
      restore();
    } else if (submitButton) {
      submitButton.disabled = false;
      submitButton.textContent = statusSubmitLabel(action);
    }
  }

  closeModal("settlementStatusModal");
  showMessage("Settlement status updated.", "success");
  try {
    await refreshDashboard();
  } catch (error) {
    showMessage(`Settlement status updated, but dashboard refresh failed: ${error.message}`, "error");
  }
  if (activeSettlementId) {
    await viewSettlementDetails(activeSettlementId);
  }
}

function statusSubmitLabel(action) {
  if (action === "processing") {
    return "Mark Processing";
  }
  if (action === "settled") {
    return "Record as Settled";
  }
  return "Record as Failed";
}

function openReversalModal(settlementId) {
  activeSettlementId = settlementId;
  document.getElementById("bankReversalReferenceInput").value = "";
  document.getElementById("reversalReasonInput").value = "";
  document.getElementById("reversalVerifiedCheckbox").checked = false;
  openModal("settlementReversalModal");
}

async function submitReversal(event) {
  event.preventDefault();
  if (!activeSettlementId) {
    return;
  }

  const bankReversalReference = document
    .getElementById("bankReversalReferenceInput")
    .value.trim();
  const reversalReason = document
    .getElementById("reversalReasonInput")
    .value.trim();
  const verifiedWithBankRecords = document
    .getElementById("reversalVerifiedCheckbox")
    .checked;
  if (!bankReversalReference || !reversalReason || !verifiedWithBankRecords) {
    showMessage(
      "Enter the bank reversal reference and reason, and confirm external bank verification.",
      "error",
    );
    return;
  }

  const submitButton = document.getElementById("submitSettlementReversal");
  const restore = submitButton && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(submitButton, "Recording...")
    : null;
  if (submitButton && window.SERVICEHUB_BUTTON && !restore) return;
  if (!restore && submitButton) {
    submitButton.disabled = true;
    submitButton.textContent = "Recording...";
  }

  try {
    await settlementRequest(
      `/${encodeURIComponent(activeSettlementId)}/reversal`,
      {
        method: "POST",
        body: JSON.stringify({
          bankReversalReference,
          reversalReason,
          verifiedWithBankRecords,
        }),
      },
    );
  } catch (error) {
    showMessage(error.message || "Unable to record the bank reversal.", "error");
    return;
  } finally {
    if (restore) {
      restore();
    } else if (submitButton) {
      submitButton.disabled = false;
      submitButton.textContent = "Record Verified Reversal";
    }
  }

  closeModal("settlementReversalModal");
  showMessage(
    "Verified bank reversal recorded. The original settlement is unchanged; create a separate settlement only if another transfer is needed.",
    "success",
  );
  submitButton.disabled = false;
  submitButton.textContent = "Record Verified Reversal";
  try {
    await refreshDashboard();
  } catch (error) {
    showMessage(
      `Reversal recorded, but dashboard refresh failed: ${error.message}`,
      "error",
    );
  }
  await viewSettlementDetails(activeSettlementId);
}

async function refreshDashboard() {
  await Promise.all([loadSummary(), loadBatches(), loadHistory()]);
}

async function settlementRequest(path, options = {}) {
  const token = sessionStorage.getItem(SETTLEMENT_ACCESS_TOKEN_KEY);
  const headers = {
    Authorization: `Bearer ${token}`,
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...options.headers,
  };
  const response = await fetch(`${SETTLEMENT_API_URL}${path}`, {
    ...options,
    headers,
  });

  // Parse response body before deciding how to handle the status
  const responseText = await response.text();
  let data = null;
  if (responseText) {
    try {
      data = JSON.parse(responseText);
    } catch {
      data = responseText;
    }
  }

  if (response.status === 401) {
    // Genuine authentication failure — JWT expired or invalid
    sessionStorage.removeItem("servicehub_access_token");
    sessionStorage.removeItem("servicehub_user_id");
    sessionStorage.removeItem("servicehub_user_email");
    sessionStorage.removeItem("servicehub_user_role");
    window.location.href = "/Admin/adminLogin.html";
    throw new Error("Your administrator session has expired.");
  }

  if (!response.ok) {
    const message =
      (data &&
        typeof data === "object" &&
        (data.message || data.error || data.detail)) ||
      (typeof data === "string" && data) ||
      (response.status === 403
        ? "Access forbidden. Your account does not have permission to perform this settlement action."
        : `Request failed with status ${response.status}.`);
    throw new Error(message);
  }
  return data;
}

function openModal(id) {
  document.getElementById(id).classList.remove("hidden");
  document.body.classList.add("overflow-hidden");
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.classList.add("hidden");
  }
  if (!document.querySelector("[data-modal]:not(.hidden)")) {
    document.body.classList.remove("overflow-hidden");
  }
}

function createDetailsSection(title, items) {
  return `
    <section>
      <h3 class="mb-3 text-lg font-semibold text-white">${escapeHtml(title)}</h3>
      <div class="grid gap-4 rounded-xl border border-gray-800 bg-gray-950/60 p-4 sm:grid-cols-2 lg:grid-cols-3">
        ${items.map(([label, value]) => createDetailItem(label, value)).join("")}
      </div>
    </section>
  `;
}

function createDetailItem(label, value) {
  const displayValue =
    value === null || value === undefined || value === "" ? "—" : value;
  return `
    <div class="min-w-0">
      <p class="text-xs text-gray-500">${escapeHtml(label)}</p>
      <p class="mt-1 break-words text-sm text-gray-200">${escapeHtml(displayValue)}</p>
    </div>
  `;
}

function getSettlementStatusBadge(status) {
  const classes = {
    PENDING: "border-amber-500/30 bg-amber-500/10 text-amber-300",
    PROCESSING: "border-sky-500/30 bg-sky-500/10 text-sky-300",
    SETTLED: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
    FAILED: "border-red-500/30 bg-red-500/10 text-red-300",
  };
  return `
    <span class="inline-flex rounded-full border px-3 py-1 text-xs font-medium ${classes[status] || "border-gray-500/30 bg-gray-500/10 text-gray-300"}">
      ${escapeHtml(formatStatus(status))}
    </span>
  `;
}

function getBatchStatusBadge(status) {
  const classes = {
    OPEN_ACCUMULATING: "border-amber-500/30 bg-amber-500/10 text-amber-300",
    CLOSED_READY_FOR_SETTLEMENT: "border-violet-500/30 bg-violet-500/10 text-violet-300",
    PARTIALLY_PROCESSED: "border-sky-500/30 bg-sky-500/10 text-sky-300",
    FULLY_SETTLED: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
    FAILED_RETRY_AVAILABLE: "border-red-500/30 bg-red-500/10 text-red-300",
  };
  const labels = {
    OPEN_ACCUMULATING: "OPEN / ACCUMULATING",
    CLOSED_READY_FOR_SETTLEMENT: "CLOSED / READY FOR SETTLEMENT",
    PARTIALLY_PROCESSED: "PARTIALLY PROCESSED",
    FULLY_SETTLED: "FULLY SETTLED",
    FAILED_RETRY_AVAILABLE: "FAILED / RETRY AVAILABLE",
  };
  return `
    <span class="inline-flex whitespace-nowrap rounded-full border px-3 py-1 text-xs font-medium ${classes[status] || "border-gray-500/30 bg-gray-500/10 text-gray-300"}">
      ${escapeHtml(labels[status] || formatStatus(status))}
    </span>
  `;
}

function formatStatus(status) {
  if (!status) {
    return "—";
  }
  return String(status)
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatCount(value) {
  const count = Number(value);
  return Number.isFinite(count) ? count.toLocaleString("en-IN") : "0";
}

function amountToCents(value) {
  const text = String(value ?? "").trim();
  const match = /^(-?)(\d+)(?:\.(\d+))?$/.exec(text);
  if (!match) {
    throw new Error("A payment amount is not a valid decimal currency value.");
  }
  const fraction = (match[3] || "").replace(/0+$/, "");
  if (fraction.length > 2) {
    throw new Error("A payment amount has more than two decimal places.");
  }
  const whole = BigInt(match[2]);
  const cents = BigInt((fraction + "00").slice(0, 2));
  const total = whole * 100n + cents;
  return match[1] === "-" ? -total : total;
}

function formatCents(cents) {
  const isNegative = cents < 0n;
  const absolute = isNegative ? -cents : cents;
  const rupees = (absolute / 100n).toLocaleString("en-IN");
  const paisa = String(absolute % 100n).padStart(2, "0");
  return `${isNegative ? "-" : ""}₹${rupees}.${paisa}`;
}

function formatMoney(value) {
  if (value === null || value === undefined || value === "") {
    return "—";
  }
  try {
    return formatCents(amountToCents(value));
  } catch {
    return "—";
  }
}

function formatDateTime(value) {
  if (!value) {
    return "—";
  }
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatDateTime(value, { fallback: "—" });
  }
  let str = String(value).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "Z";
  }
  const date = new Date(str);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("en-IN", {
        timeZone: "Asia/Kolkata",
        dateStyle: "medium",
        timeStyle: "short",
      }).format(date);
}

function formatDateTimeIst(value) {
  if (!value) {
    return "—";
  }
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatIstDateTime(value, {
      fallback: "—",
      dateStyle: "medium",
      timeStyle: "medium",
    });
  }
  const text = String(value).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(text);
  const date = new Date(hasOffset ? text : `${text}+05:30`);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("en-IN", {
        timeZone: "Asia/Kolkata",
        dateStyle: "medium",
        timeStyle: "medium",
      }).format(date);
}

function formatBusinessDate(value) {
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatCalendarDate(value, { fallback: "Unclassified" });
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || ""));
  if (!match) {
    return "Unclassified";
  }
  const date = new Date(
    Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])),
  );
  return new Intl.DateTimeFormat("en-IN", {
    timeZone: "UTC",
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(date);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function cssEscape(value) {
  if (window.CSS && typeof window.CSS.escape === "function") {
    return window.CSS.escape(value);
  }
  return String(value).replace(/["\\]/g, "\\$&");
}

function loadingRow(columnCount, message) {
  return `<tr><td colspan="${columnCount}" class="px-6 py-12 text-center text-gray-500">${escapeHtml(message)}</td></tr>`;
}

function emptyRow(columnCount, message, textClass = "text-gray-500") {
  return `<tr><td colspan="${columnCount}" class="px-6 py-12 text-center ${textClass}">${escapeHtml(message)}</td></tr>`;
}

function showMessage(message, type) {
  settlementMessage.textContent = message;
  settlementMessage.className = "mb-5 rounded-lg border px-4 py-3 text-sm";
  const classes =
    type === "success"
      ? ["border-emerald-500/30", "bg-emerald-500/10", "text-emerald-300"]
      : type === "warning"
        ? ["border-amber-500/30", "bg-amber-500/10", "text-amber-300"]
        : ["border-red-500/30", "bg-red-500/10", "text-red-300"];
  settlementMessage.classList.add(...classes);
}
