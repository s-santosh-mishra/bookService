const API_ROOT = window.SERVICEHUB_CONFIG ? window.SERVICEHUB_CONFIG.API_BASE_URL : "http://localhost:8080";
const API_BASE_URL = `${API_ROOT}/api/admin/payments`;
const PAYMENT_ACCESS_TOKEN_KEY = "servicehub_access_token";

const paymentTableBody = document.getElementById("paymentTableBody");
const paymentCount = document.getElementById("paymentCount");
const paymentSearch = document.getElementById("paymentSearch");
const paymentDetailsModal = document.getElementById("paymentDetailsModal");
const paymentDetailsContent = document.getElementById("paymentDetailsContent");
const paymentMessage = document.getElementById("paymentMessage");

let payments = [];

const filterIds = [
  "statusFilter",
  "methodFilter",
  "createdFromFilter",
  "createdToFilter",
  "minimumAmountFilter",
  "maximumAmountFilter",
  "sortFilter",
];

filterIds.forEach((id) => {
  document.getElementById(id).addEventListener("change", applyFilters);
});

paymentSearch.addEventListener("input", applyFilters);

document.getElementById("clearSearchButton").addEventListener("click", () => {
  paymentSearch.value = "";
  applyFilters();
});

document.getElementById("clearFiltersButton").addEventListener("click", () => {
  filterIds.forEach((id) => {
    const filter = document.getElementById(id);
    filter.value = id === "sortFilter" ? "created-desc" : "";
  });
  applyFilters();
});

paymentTableBody.addEventListener("click", (event) => {
  const button = event.target.closest(".view-payment-button");
  if (button) {
    viewPayment(button.dataset.paymentId);
  }
});

document
  .getElementById("closePaymentDetails")
  .addEventListener("click", closePaymentDetails);
document
  .getElementById("closePaymentDetailsFooter")
  .addEventListener("click", closePaymentDetails);
document
  .getElementById("paymentDetailsOverlay")
  .addEventListener("click", closePaymentDetails);

async function loadPayments() {
  hideMessage();
  paymentTableBody.innerHTML = `
    <tr>
      <td colspan="9" class="px-6 py-16 text-center text-gray-500">
        Loading payments...
      </td>
    </tr>
  `;

  const token = sessionStorage.getItem(PAYMENT_ACCESS_TOKEN_KEY);

  try {
    const response = await fetch(API_BASE_URL, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (handleAuthorizationFailure(response)) {
      return;
    }

    if (!response.ok) {
      throw new Error(
        await getResponseError(response, "Failed to load payments."),
      );
    }

    const dashboard = await response.json();
    if (!dashboard || !dashboard.summary || !Array.isArray(dashboard.payments)) {
      throw new Error("The payment response was incomplete.");
    }

    payments = dashboard.payments;
    renderSummary(dashboard.summary);
    applyFilters();
  } catch (error) {
    paymentTableBody.innerHTML = `
      <tr>
        <td colspan="9" class="px-6 py-12 text-center text-red-400">
          ${escapeHtml(error.message || "Unable to load payments.")}
        </td>
      </tr>
    `;
    paymentCount.textContent = "Payments unavailable";
    showMessage(error.message || "Unable to load payments.", "error");
  }
}

function renderSummary(summary) {
  document.getElementById("summarySuccessful").textContent =
    formatCount(summary.successfulCount);
  document.getElementById("summaryPending").textContent =
    formatCount(summary.pendingCount);
  document.getElementById("summaryFailed").textContent =
    formatCount(summary.failedCount);
  document.getElementById("summaryCancelled").textContent =
    formatCount(summary.cancelledCount);
  document.getElementById("summaryRefunded").textContent =
    formatCount(summary.refundedCount);
  document.getElementById("summaryCollected").textContent =
    formatMoney(summary.successfullyCollectedAmount);
}

function applyFilters() {
  const searchValue = paymentSearch.value.trim().toLowerCase();
  const status = document.getElementById("statusFilter").value;
  const method = document.getElementById("methodFilter").value;
  const createdFrom = document.getElementById("createdFromFilter").value;
  const createdTo = document.getElementById("createdToFilter").value;
  const minimumInput = document.getElementById("minimumAmountFilter").value.trim();
  const maximumInput = document.getElementById("maximumAmountFilter").value.trim();
  const sortBy = document.getElementById("sortFilter").value;

  const minimumAmount = minimumInput ? Number(minimumInput) : null;
  const maximumAmount = maximumInput ? Number(maximumInput) : null;

  if (
    (minimumInput && (!Number.isFinite(minimumAmount) || minimumAmount < 0)) ||
    (maximumInput && (!Number.isFinite(maximumAmount) || maximumAmount < 0))
  ) {
    showInvalidFilters("Amounts must be valid non-negative numbers.");
    return;
  }

  if (
    (createdFrom && createdTo && createdFrom > createdTo) ||
    (minimumAmount !== null &&
      maximumAmount !== null &&
      minimumAmount > maximumAmount)
  ) {
    showInvalidFilters("The filter range start must not exceed its end.");
    return;
  }

  hideMessage();

  const filteredPayments = payments.filter((payment) => {
    if (status && payment.status !== status) {
      return false;
    }

    if (method && payment.paymentMethod !== method) {
      return false;
    }

    const createdDate = getLocalDateValue(payment.createdAt);
    if (createdFrom && (!createdDate || createdDate < createdFrom)) {
      return false;
    }
    if (createdTo && (!createdDate || createdDate > createdTo)) {
      return false;
    }

    const amount = Number(payment.amount);
    if (minimumAmount !== null && (!Number.isFinite(amount) || amount < minimumAmount)) {
      return false;
    }
    if (maximumAmount !== null && (!Number.isFinite(amount) || amount > maximumAmount)) {
      return false;
    }

    if (searchValue) {
      const searchableFields = [
        payment.paymentId,
        payment.bookingId,
        payment.customerName,
        payment.customerPhone,
        payment.workerName,
        payment.workerPhone,
      ];

      if (
        !searchableFields.some((value) =>
          String(value || "")
            .toLowerCase()
            .includes(searchValue),
        )
      ) {
        return false;
      }
    }

    return true;
  });

  sortPayments(filteredPayments, sortBy);
  renderPayments(filteredPayments);
}

function sortPayments(paymentList, sortBy) {
  paymentList.sort((left, right) => {
    if (sortBy === "created-asc" || sortBy === "created-desc") {
      const direction = sortBy === "created-asc" ? 1 : -1;
      return direction * (getTimestamp(left.createdAt) - getTimestamp(right.createdAt));
    }

    if (sortBy === "amount-asc" || sortBy === "amount-desc") {
      const direction = sortBy === "amount-asc" ? 1 : -1;
      const leftAmount = Number(left.amount);
      const rightAmount = Number(right.amount);
      const leftMissing = !Number.isFinite(leftAmount);
      const rightMissing = !Number.isFinite(rightAmount);

      if (leftMissing !== rightMissing) {
        return leftMissing ? 1 : -1;
      }

      return direction * (leftAmount - rightAmount);
    }

    return 0;
  });
}

function renderPayments(paymentList) {
  paymentCount.textContent = `${paymentList.length} ${
    paymentList.length === 1 ? "payment attempt" : "payment attempts"
  }`;

  if (paymentList.length === 0) {
    paymentTableBody.innerHTML = `
      <tr>
        <td colspan="9" class="px-6 py-16 text-center">
          <div class="flex flex-col items-center">
            <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-gray-800">
              <i class="fa-solid fa-receipt text-lg text-gray-600"></i>
            </div>
            <p class="text-gray-300">No payments match these filters.</p>
            <p class="mt-1 text-sm text-gray-500">
              Try changing your search or clearing the filters.
            </p>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  paymentTableBody.innerHTML = paymentList
    .map(
      (payment) => `
        <tr class="border-b border-gray-800 last:border-0 transition-colors hover:bg-gray-800/40">
          <td class="max-w-[180px] break-all px-5 py-4 text-xs text-gray-400">
            ${escapeHtml(payment.paymentId || "—")}
          </td>
          <td class="max-w-[180px] break-all px-5 py-4 text-xs text-gray-400">
            ${escapeHtml(payment.bookingId || "—")}
          </td>
          <td class="px-5 py-4">
            <p class="font-medium text-white">
              ${escapeHtml(payment.customerName || "—")}
            </p>
            <p class="mt-1 text-xs text-gray-500">
              ${escapeHtml(payment.customerPhone || "—")}
            </p>
          </td>
          <td class="px-5 py-4">
            <p class="font-medium text-gray-300">
              ${escapeHtml(payment.workerName || "—")}
            </p>
            <p class="mt-1 text-xs text-gray-500">
              ${escapeHtml(payment.workerPhone || "—")}
            </p>
          </td>
          <td class="px-5 py-4 text-gray-300">
            ${escapeHtml(formatPaymentMethod(payment.paymentMethod))}
          </td>
          <td class="whitespace-nowrap px-5 py-4 text-gray-300">
            ${escapeHtml(formatMoney(payment.amount))}
          </td>
          <td class="px-5 py-4">
            ${getPaymentStatusBadge(payment.status)}
          </td>
          <td class="whitespace-nowrap px-5 py-4 text-sm text-gray-400">
            ${escapeHtml(formatDateTime(payment.createdAt))}
          </td>
          <td class="px-5 py-4 text-right">
            <button
              type="button"
              class="view-payment-button whitespace-nowrap rounded-lg bg-gray-800 px-3 py-2 text-sm text-gray-300 transition-colors hover:bg-gray-700 hover:text-white"
              data-payment-id="${escapeHtml(payment.paymentId || "")}"
              ${payment.paymentId ? "" : "disabled"}
            >
              <i class="fa-solid fa-eye mr-1"></i>
              View Details
            </button>
          </td>
        </tr>
      `,
    )
    .join("");
}

async function viewPayment(paymentId) {
  paymentDetailsModal.classList.remove("hidden");
  document.body.classList.add("overflow-hidden");
  document.getElementById("paymentDetailsTitle").textContent =
    "Payment Details";
  document.getElementById("paymentDetailsStatus").innerHTML = "";
  paymentDetailsContent.innerHTML = `
    <p class="py-12 text-center text-gray-400">Loading payment details...</p>
  `;

  const token = sessionStorage.getItem(PAYMENT_ACCESS_TOKEN_KEY);

  try {
    const response = await fetch(
      `${API_BASE_URL}/${encodeURIComponent(paymentId)}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      },
    );

    if (handleAuthorizationFailure(response)) {
      closePaymentDetails();
      return;
    }

    if (!response.ok) {
      throw new Error(
        await getResponseError(response, "Failed to load payment details."),
      );
    }

    renderPaymentDetails(await response.json());
  } catch (error) {
    paymentDetailsContent.innerHTML = `
      <p class="py-12 text-center text-red-400">
        ${escapeHtml(error.message || "Unable to load payment details.")}
      </p>
    `;
  }
}

function renderPaymentDetails(details) {
  if (!details || !details.paymentId) {
    throw new Error("Payment details are incomplete.");
  }

  document.getElementById("paymentDetailsTitle").textContent =
    `Payment ${details.paymentId}`;
  document.getElementById("paymentDetailsStatus").innerHTML =
    getPaymentStatusBadge(details.status);

  const paymentInformation = [
    ["Payment ID", details.paymentId],
    ["Booking ID", details.bookingId],
    ["Method", formatPaymentMethod(details.paymentMethod)],
    ["Amount", formatMoney(details.amount)],
    ["Status", formatStatus(details.status)],
    ["Created", formatDateTime(details.createdAt)],
    ["Paid", formatIstDateTime(details.paidAt)],
  ];

  const razorpayInformation =
    details.paymentMethod === "RAZORPAY"
      ? createDetailsSection("Razorpay Information", [
          ["Razorpay Order ID", details.razorpayOrderId],
          ["Razorpay Payment ID", details.razorpayPaymentId],
        ])
      : "";

  const customer = details.customer;
  const worker = details.worker;
  const booking = details.booking;

  const attempts = Array.isArray(details.relatedAttempts)
    ? details.relatedAttempts
    : [];

  paymentDetailsContent.innerHTML = `
    <div class="space-y-7">
      ${createDetailsSection("Payment Information", paymentInformation)}
      ${razorpayInformation}
      ${createDetailsSection("Customer Information", [
        ["Name", customer?.name],
        ["Phone", customer?.phone],
        ["Email", customer?.email],
      ])}
      ${createDetailsSection("Worker Information", [
        ["Name", worker?.name || "Unassigned"],
        ["Phone", worker?.phone],
        ["Email", worker?.email],
      ])}
      ${createDetailsSection("Booking Information", [
        ["Status", formatStatus(booking?.status)],
        ["Service", booking?.serviceName],
        ["Category", booking?.categoryName],
        ["Booking Created", formatDateTime(booking?.createdAt)],
      ])}
      <section>
        <h3 class="mb-3 text-lg font-semibold text-white">
          Related Payment Attempts
        </h3>
        ${
          attempts.length
            ? `<div class="space-y-3">${attempts
                .map((attempt) => renderRelatedAttempt(attempt, details.paymentId))
                .join("")}</div>`
            : `<p class="rounded-xl border border-gray-800 bg-gray-950/60 p-4 text-sm text-gray-500">
                No related payment attempts are available.
              </p>`
        }
      </section>
    </div>
  `;
}

function renderRelatedAttempt(attempt, selectedPaymentId) {
  const isSelected = attempt.paymentId === selectedPaymentId;
  const cardClass = isSelected
    ? "border-violet-500/40 bg-violet-500/5"
    : "border-gray-800 bg-gray-950/60";

  return `
    <article class="rounded-xl border ${cardClass} p-4">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
        <p class="break-all text-sm font-medium text-gray-200">
          ${escapeHtml(attempt.paymentId || "—")}
        </p>
        ${
          isSelected
            ? `<span class="rounded-full bg-violet-500/10 px-3 py-1 text-xs text-violet-300">
                Selected payment
              </span>`
            : ""
        }
      </div>
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        ${createDetailItem("Method", formatPaymentMethod(attempt.paymentMethod))}
        ${createDetailItem("Amount", formatMoney(attempt.amount))}
        ${createDetailItem("Status", formatStatus(attempt.status))}
        ${createDetailItem("Created", formatDateTime(attempt.createdAt))}
        ${createDetailItem("Paid", formatIstDateTime(attempt.paidAt))}
      </div>
    </article>
  `;
}

function createDetailsSection(title, items) {
  return `
    <section>
      <h3 class="mb-3 text-lg font-semibold text-white">${escapeHtml(title)}</h3>
      <div class="grid gap-4 rounded-xl border border-gray-800 bg-gray-950/60 p-4 sm:grid-cols-2 lg:grid-cols-3">
        ${items
          .map(([label, value]) => createDetailItem(label, value))
          .join("")}
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
      <p class="mt-1 break-words text-sm text-gray-200">
        ${escapeHtml(displayValue)}
      </p>
    </div>
  `;
}

function closePaymentDetails() {
  paymentDetailsModal.classList.add("hidden");
  document.body.classList.remove("overflow-hidden");
}

function getPaymentStatusBadge(status) {
  const classes = {
    CREATED: "bg-slate-500/10 text-slate-300 border-slate-500/30",
    PENDING: "bg-amber-500/10 text-amber-400 border-amber-500/30",
    SUCCESS: "bg-emerald-500/10 text-emerald-400 border-emerald-500/30",
    FAILED: "bg-red-500/10 text-red-400 border-red-500/30",
    CANCELLED: "bg-gray-500/10 text-gray-400 border-gray-500/30",
    REFUNDED: "bg-sky-500/10 text-sky-400 border-sky-500/30",
  };
  const label = formatStatus(status);
  const statusClass = classes[status] || classes.CREATED;

  return `
    <span class="inline-flex rounded-full border px-3 py-1 text-xs font-medium ${statusClass}">
      ${escapeHtml(label)}
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

function formatPaymentMethod(method) {
  if (method === "RAZORPAY") {
    return "Razorpay";
  }
  if (method === "CASH") {
    return "Cash";
  }
  return method || "—";
}

function formatCount(value) {
  const count = Number(value);
  return Number.isFinite(count) ? count.toLocaleString("en-IN") : "0";
}

function formatMoney(value) {
  if (value === null || value === undefined || value === "") {
    return "—";
  }

  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return "—";
  }

  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
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

function formatIstDateTime(value) {
  if (!value) {
    return "—";
  }
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatIstDateTime(value, { fallback: "—" });
  }
  let str = String(value).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "+05:30";
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

function getTimestamp(value) {
  if (!value) {
    return 0;
  }
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.getTimestamp(value);
  }
  let str = String(value).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "Z";
  }
  const timestamp = new Date(str).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function getLocalDateValue(value) {
  if (!value) {
    return "";
  }
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.getLocalDateValue(value);
  }

  let str = String(value).trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(str)) {
    return str;
  }
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "Z";
  }
  const date = new Date(str);
  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const y = parts.find((p) => p.type === "year")?.value;
  const m = parts.find((p) => p.type === "month")?.value;
  const d = parts.find((p) => p.type === "day")?.value;
  return `${y}-${m}-${d}`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function showInvalidFilters(message) {
  paymentTableBody.innerHTML = `
    <tr>
      <td colspan="9" class="px-6 py-12 text-center text-yellow-400">
        ${escapeHtml(message)}
      </td>
    </tr>
  `;
  paymentCount.textContent = "Invalid filters";
  showMessage(message, "error");
}

function showMessage(message, type) {
  paymentMessage.textContent = message;
  paymentMessage.className =
    "mb-5 rounded-lg border px-4 py-3 text-sm";

  if (type === "warning") {
    paymentMessage.classList.add(
      "border-amber-500/30",
      "bg-amber-500/10",
      "text-amber-300",
    );
  } else {
    paymentMessage.classList.add(
      "border-red-500/30",
      "bg-red-500/10",
      "text-red-300",
    );
  }
}

function hideMessage() {
  paymentMessage.classList.add("hidden");
}

function handleAuthorizationFailure(response) {
  if (response.status !== 401 && response.status !== 403) {
    return false;
  }

  sessionStorage.removeItem("servicehub_access_token");
  sessionStorage.removeItem("servicehub_user_id");
  sessionStorage.removeItem("servicehub_user_email");
  sessionStorage.removeItem("servicehub_user_role");
  window.location.href = "/Admin/adminLogin.html";
  return true;
}

async function getResponseError(response, fallback) {
  try {
    const data = await response.json();
    return data.error || data.message || fallback;
  } catch {
    return fallback;
  }
}

loadPayments();
