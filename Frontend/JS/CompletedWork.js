const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";

const API_ROOT = window.SERVICEHUB_CONFIG ? window.SERVICEHUB_CONFIG.API_BASE_URL : "http://localhost:8080";
const API_BASE_URL = `${API_ROOT}/api/worker`;

// Authentication

function checkWorkerLogin() {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  const role = localStorage.getItem(ROLE_KEY);

  if (!token || role !== "WORKER") {
    window.location.href = "/HTML/login.html";

    return false;
  }

  return true;
}

// API Helper

async function workerApiRequest(url, options = {}) {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  const response = await fetch(url, {
    ...options,

    headers: {
      "Content-Type": "application/json",

      Authorization: `Bearer ${token}`,

      ...(options.headers || {}),
    },
  });

  /*
   * Only clear authentication when the token
   * itself is invalid or expired.
   */

  if (response.status === 401) {
    localStorage.removeItem(ACCESS_TOKEN_KEY);

    localStorage.removeItem(ROLE_KEY);

    window.location.href = "/HTML/login.html";

    throw new Error("Unauthorized");
  }

  if (!response.ok) {
    let message = "Something went wrong.";

    try {
      const data = await response.json();

      if (data.message) {
        message = data.message;
      }
    } catch (error) {
      // Ignore parsing errors
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

// Daily Settlement Summary

function formatCurrency(val) {
  const num = Number(val || 0);
  return `₹${num.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

async function loadDailySettlementSummary() {
  try {
    const summary = await workerApiRequest(
      `${API_BASE_URL}/settlements/today-summary`,
    );
    renderDailySettlementSummary(summary);
  } catch (error) {
    console.error("Failed to load daily settlement summary:", error);
    renderDailySettlementSummaryError(error);
  }
}

function renderDailySettlementSummary(summary) {
  if (!summary) return;

  const dateEl = document.getElementById("summaryBusinessDateText");
  const totalEarningsEl = document.getElementById("summaryTotalEarnings");
  const jobCountEl = document.getElementById("summaryJobCount");
  const cashEarningsEl = document.getElementById("summaryCashEarnings");
  const statusBadgeEl = document.getElementById("summaryStatusBadge");
  const statusMessageEl = document.getElementById("summaryStatusMessage");
  const statusIconContainer = document.getElementById(
    "summaryStatusIconContainer",
  );
  const statusIcon = document.getElementById("summaryStatusIcon");
  const settledAmountEl = document.getElementById("summarySettledAmount");
  const pendingAmountEl = document.getElementById("summaryPendingAmount");
  const unsettledBreakdownEl = document.getElementById(
    "summaryUnsettledBreakdown",
  );

  if (dateEl) {
    let formattedDate = summary.businessDate || "Today";
    if (window.SERVICEHUB_DATE && window.SERVICEHUB_DATE.formatDateOnly) {
      formattedDate = window.SERVICEHUB_DATE.formatDateOnly(
        summary.businessDate,
        { fallback: summary.businessDate },
      );
    } else if (summary.businessDate) {
      try {
        const [year, month, day] = summary.businessDate
          .split("-")
          .map(Number);
        const d = new Date(year, month - 1, day);
        formattedDate = d.toLocaleDateString("en-IN", {
          day: "numeric",
          month: "short",
          year: "numeric",
        });
      } catch (e) {
        // preserve original string
      }
    }
    const batchTag = summary.batchClosed ? "Batch Closed" : "Active Batch";
    dateEl.textContent = `Business Date: ${formattedDate} (${batchTag})`;
  }

  if (totalEarningsEl) {
    totalEarningsEl.textContent = formatCurrency(summary.totalEarnings);
  }

  if (jobCountEl) {
    const count = summary.completedJobsCount || 0;
    jobCountEl.textContent = `${count} ${count === 1 ? "job" : "jobs"}`;
  }

  if (cashEarningsEl) {
    cashEarningsEl.textContent = `Cash: ${formatCurrency(summary.cashEarnings)}`;
  }

  if (settledAmountEl) {
    settledAmountEl.textContent = formatCurrency(summary.settledAmount);
  }

  if (pendingAmountEl) {
    pendingAmountEl.textContent = formatCurrency(
      summary.pendingSettlementAmount,
    );
  }

  if (unsettledBreakdownEl) {
    const unsettled = Number(summary.unsettledAmount || 0);
    const online = Number(summary.onlineEarnings || 0);
    if (unsettled > 0 && !summary.batchClosed) {
      unsettledBreakdownEl.textContent = `Online: ${formatCurrency(online)} (accumulating)`;
    } else if (unsettled > 0) {
      unsettledBreakdownEl.textContent = `Online: ${formatCurrency(online)} (ready)`;
    } else {
      unsettledBreakdownEl.textContent = `Online: ${formatCurrency(online)}`;
    }
  }

  // Status Badge and Message
  if (statusBadgeEl && statusMessageEl) {
    statusMessageEl.textContent = summary.settlementStatusMessage || "";

    const statusConfig = {
      NO_JOBS: {
        text: "No Jobs Yet",
        badgeClass: "bg-gray-800 text-gray-400 border-gray-700",
        iconClass: "fa-solid fa-moon text-gray-400 text-sm",
        iconBoxClass: "bg-gray-800/60 border-gray-700",
      },
      CASH_COLLECTED: {
        text: "Direct Cash",
        badgeClass: "bg-teal-900/30 text-teal-400 border-teal-700/40",
        iconClass: "fa-solid fa-money-bill-wave text-teal-400 text-sm",
        iconBoxClass: "bg-teal-900/30 border-teal-700/40",
      },
      SETTLED: {
        text: "Settled",
        badgeClass:
          "bg-emerald-900/30 text-emerald-400 border-emerald-700/40",
        iconClass: "fa-solid fa-circle-check text-emerald-400 text-sm",
        iconBoxClass: "bg-emerald-900/30 border-emerald-700/40",
      },
      PARTIALLY_SETTLED: {
        text: "Partially Settled",
        badgeClass: "bg-amber-900/30 text-amber-400 border-amber-700/40",
        iconClass: "fa-solid fa-hourglass-half text-amber-400 text-sm",
        iconBoxClass: "bg-amber-900/30 border-amber-700/40",
      },
      PROCESSING: {
        text: "Processing",
        badgeClass: "bg-amber-900/30 text-amber-400 border-amber-700/40",
        iconClass: "fa-solid fa-spinner fa-spin text-amber-400 text-sm",
        iconBoxClass: "bg-amber-900/30 border-amber-700/40",
      },
      OPEN_ACCUMULATING: {
        text: "Batch Open",
        badgeClass: "bg-blue-900/30 text-blue-400 border-blue-700/40",
        iconClass: "fa-solid fa-clock-rotate-left text-blue-400 text-sm",
        iconBoxClass: "bg-blue-900/30 border-blue-700/40",
      },
      READY_FOR_SETTLEMENT: {
        text: "Batch Closed",
        badgeClass:
          "bg-violet-900/30 text-violet-400 border-violet-700/40",
        iconClass: "fa-solid fa-check-double text-violet-400 text-sm",
        iconBoxClass: "bg-violet-900/30 border-violet-700/40",
      },
      REVERSED_RETRY_AVAILABLE: {
        text: "Reversed — Retrying",
        badgeClass: "bg-rose-900/30 text-rose-400 border-rose-700/40",
        iconClass: "fa-solid fa-rotate-left text-rose-400 text-sm",
        iconBoxClass: "bg-rose-900/30 border-rose-700/40",
      },
      FAILED_RETRY_AVAILABLE: {
        text: "Transfer Failed",
        badgeClass: "bg-rose-900/30 text-rose-400 border-rose-700/40",
        iconClass:
          "fa-solid fa-triangle-exclamation text-rose-400 text-sm",
        iconBoxClass: "bg-rose-900/30 border-rose-700/40",
      },
    };

    const cfg = statusConfig[summary.settlementStatus] || {
      text: summary.settlementStatus || "Pending",
      badgeClass: "bg-gray-800 text-gray-300 border-gray-700",
      iconClass: "fa-solid fa-hourglass-half text-gray-400 text-sm",
      iconBoxClass: "bg-gray-800/60 border-gray-700",
    };

    statusBadgeEl.className = `px-2.5 py-0.5 rounded-full text-xs font-medium border ${cfg.badgeClass}`;
    statusBadgeEl.textContent = cfg.text;

    if (statusIcon) {
      statusIcon.className = cfg.iconClass;
    }
    if (statusIconContainer) {
      statusIconContainer.className = `w-9 h-9 rounded-xl border flex items-center justify-center shrink-0 ${cfg.iconBoxClass}`;
    }
  }
}

function renderDailySettlementSummaryError(error) {
  const dateEl = document.getElementById("summaryBusinessDateText");
  const statusBadgeEl = document.getElementById("summaryStatusBadge");
  const statusMessageEl = document.getElementById("summaryStatusMessage");

  if (dateEl) {
    dateEl.textContent = "Unable to load business date";
  }
  if (statusBadgeEl) {
    statusBadgeEl.className =
      "px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-900/30 text-red-400 border border-red-700/40";
    statusBadgeEl.textContent = "Summary Unavailable";
  }
  if (statusMessageEl) {
    statusMessageEl.textContent =
      error && error.message
        ? error.message
        : "Unable to retrieve today's settlement summary.";
  }
}

// Load Completed Work

async function loadCompletedWork() {
  const container = document.getElementById("completedWorkContainer");

  if (!container) {
    return;
  }

  // Load daily settlement summary in parallel
  loadDailySettlementSummary();

  try {
    const bookings = await workerApiRequest(`${API_BASE_URL}/bookings`);

    const completedBookings = (bookings || [])
      .filter(
        (booking) =>
          booking.status === "COMPLETED" || booking.status === "REVIEWED",
      )
      .sort((a, b) => {
        if (window.SERVICEHUB_DATE) {
          const tA =
            window.SERVICEHUB_DATE.getTimestamp(a.completedAt) ||
            window.SERVICEHUB_DATE.getTimestamp(a.updatedAt);
          const tB =
            window.SERVICEHUB_DATE.getTimestamp(b.completedAt) ||
            window.SERVICEHUB_DATE.getTimestamp(b.updatedAt);
          return tB - tA;
        }
        return (
          new Date(b.completedAt || b.updatedAt) -
          new Date(a.completedAt || a.updatedAt)
        );
      });

    renderCompletedWork(completedBookings);
  } catch (error) {
    console.error("Failed to load completed work:", error);

    container.innerHTML = `

            <div class="bg-gray-900
                        border border-gray-800
                        rounded-2xl
                        p-10
                        text-center">

                <div class="w-16 h-16
                            mx-auto mb-5
                            rounded-full
                            bg-red-900/20
                            flex items-center
                            justify-center">

                    <i class="fa-solid
                              fa-triangle-exclamation
                              text-red-400
                              text-2xl"></i>

                </div>


                <h4 class="text-lg
                           font-semibold
                           mb-2">

                    Unable to load completed work

                </h4>


                <p class="text-gray-400
                          text-sm
                          mb-5">

                    ${escapeHtml(error.message)}

                </p>


                <button
                    id="retryCompletedWork"
                    class="px-5 py-2.5
                           rounded-xl
                           bg-violet-600
                           hover:bg-violet-500
                           text-white
                           text-sm
                           font-medium
                           transition">

                    Retry

                </button>

            </div>

        `;

    const retryButton = document.getElementById("retryCompletedWork");

    if (retryButton) {
      retryButton.addEventListener("click", loadCompletedWork);
    }
  }
}

// Render Completed Work

function renderCompletedWork(bookings) {
  const container = document.getElementById("completedWorkContainer");

  if (!container) {
    return;
  }

  if (!bookings || bookings.length === 0) {
    container.innerHTML = `

            <div class="bg-gray-900
                        border border-gray-800
                        rounded-2xl
                        p-10
                        text-center">

                <div class="w-16 h-16
                            mx-auto mb-5
                            rounded-full
                            bg-gray-800
                            flex items-center
                            justify-center">

                    <i class="fa-solid
                              fa-clipboard-check
                              text-gray-500
                              text-2xl"></i>

                </div>


                <h4 class="text-lg
                           font-semibold
                           mb-2">

                    No completed work

                </h4>


                <p class="text-gray-400 text-sm">

                    Your completed services
                    will appear here.

                </p>

            </div>

        `;

    return;
  }

  container.innerHTML = bookings
    .map((booking) => createCompletedBookingCard(booking))
    .join("");
}

// Completed Booking Card

// Completed Booking Card

function createCompletedBookingCard(booking) {
  const customerName = escapeHtml(booking.customerName || "Customer");

  const serviceName = escapeHtml(booking.serviceName || "Service");

  const requestedDateTime = formatDateTime(booking.createdAt);

  const completedDateTime = formatDateTime(booking.completedAt);

  const finalBilledPrice =
    booking.finalBilledPrice !== null &&
    booking.finalBilledPrice !== undefined &&
    Number.isFinite(Number(booking.finalBilledPrice))
      ? `₹${Number(booking.finalBilledPrice).toFixed(2)}`
      : "Amount unavailable";

  const paymentMethod =
    booking.paymentMethod === "RAZORPAY"
      ? "Razorpay"
      : booking.paymentMethod === "CASH"
        ? "Cash"
        : "Payment method unavailable";

  return `

        <article
            class="border border-gray-800 bg-gray-900/40 rounded-2xl p-6"
        >

            <div class="flex flex-col gap-5">


                <!-- Top -->

                <div
                    class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4"
                >

                    <div>

                        <p
                            class="text-sm text-gray-500 mb-1"
                        >
                            Service
                        </p>

                        <h2
                            class="text-xl font-semibold text-white"
                        >
                            ${serviceName}
                        </h2>

                    </div>


                    <span
                        class="inline-flex w-fit items-center px-3 py-1.5 rounded-full border text-sm font-medium bg-green-500/10 text-green-400 border-green-500/30"
                    >

                        <i
                            class="fa-solid fa-check mr-1.5"
                        ></i>

                        Completed

                    </span>

                </div>



                <!-- Description -->

                <div>

                    <p
                        class="text-gray-400 text-sm"
                    >
                        This service has been completed.
                    </p>

                </div>



                <!-- Details -->

                <div
                    class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 pt-4 border-t border-gray-800"
                >


                    <!-- Customer -->

                    <div
                        class="flex items-center gap-3"
                    >

                        <div
                            class="w-10 h-10 rounded-lg bg-gray-800 flex items-center justify-center shrink-0"
                        >

                            <i
                                class="fa-solid fa-user text-gray-400"
                            ></i>

                        </div>

                        <div>

                            <p
                                class="text-xs text-gray-500 mb-1"
                            >
                                Customer
                            </p>

                            <p
                                class="text-sm text-gray-200"
                            >
                                ${customerName}
                            </p>

                        </div>

                    </div>



                    <!-- Requested -->

                    <div
                        class="flex items-center gap-3"
                    >

                        <div
                            class="w-10 h-10 rounded-lg bg-gray-800 flex items-center justify-center shrink-0"
                        >

                            <i
                                class="fa-solid fa-calendar text-gray-400"
                            ></i>

                        </div>

                        <div>

                            <p
                                class="text-xs text-gray-500 mb-1"
                            >
                                Requested
                            </p>

                            <p
                                class="text-sm text-gray-200"
                            >
                                ${requestedDateTime}
                            </p>

                        </div>

                    </div>



                    <!-- Completed -->

                    <div
                        class="flex items-center gap-3"
                    >

                        <div
                            class="w-10 h-10 rounded-lg bg-gray-800 flex items-center justify-center shrink-0"
                        >

                            <i
                                class="fa-solid fa-check text-green-400"
                            ></i>

                        </div>

                        <div>

                            <p
                                class="text-xs text-gray-500 mb-1"
                            >
                                Completed
                            </p>

                            <p
                                class="text-sm text-gray-200"
                            >
                                ${completedDateTime}
                            </p>

                        </div>

                    </div>


                </div>



                <!-- Customer Note -->

                ${
                  booking.customerNote
                    ? `

                            <div
                                class="pt-4 border-t border-gray-800"
                            >

                                <p
                                    class="text-xs text-gray-500 mb-1"
                                >
                                    Customer Note
                                </p>

                                <p
                                    class="text-sm text-gray-300 leading-relaxed"
                                >
                                    ${escapeHtml(booking.customerNote)}
                                </p>

                            </div>

                        `
                    : ""
                }



                <!-- Payment -->

                <div
                    class="pt-4 border-t border-gray-800"
                >

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

                        <div class="flex items-center gap-2">

                            <i
                                class="fa-solid fa-credit-card text-gray-500"
                            ></i>

                            <span
                                class="text-sm text-gray-400"
                            >
                                Payment
                            </span>

                            <span class="text-sm text-gray-200">
                                ${escapeHtml(paymentMethod)}
                            </span>

                        </div>

                        <div class="flex items-center justify-end gap-2">
                            <span class="text-sm text-gray-400">
                                Final amount
                            </span>
                            <span class="text-sm font-semibold text-gray-200">
                                ${escapeHtml(finalBilledPrice)}
                            </span>
                        </div>

                    </div>

                </div>


            </div>

        </article>

    `;
}

// Customer Address

function buildCustomerAddress(booking) {
  const addressParts = [
    booking.customerAddressLine1,

    booking.customerAddressLine2,

    booking.customerLandmark,

    booking.customerCity,

    booking.customerState,

    booking.customerPinCode,
  ].filter(Boolean);

  if (addressParts.length === 0) {
    return "Address not provided";
  }

  return addressParts.map((part) => escapeHtml(part)).join(", ");
}

// Date Formatting

function formatDateTime(dateString) {
  if (!dateString) {
    return "—";
  }

  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatDateTime(dateString, { fallback: "—" });
  }

  let str = String(dateString).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "Z";
  }
  const date = new Date(str);

  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

// HTML Escape

function escapeHtml(value) {
  if (value === null || value === undefined) {
    return "";
  }

  return String(value)
    .replace(/&/g, "&amp;")

    .replace(/</g, "&lt;")

    .replace(/>/g, "&gt;")

    .replace(/"/g, "&quot;")

    .replace(/'/g, "&#039;");
}

// Initialisation

if (checkWorkerLogin()) {
  loadCompletedWork();
}
