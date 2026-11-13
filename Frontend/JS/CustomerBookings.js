const API_ROOT = window.SERVICEHUB_CONFIG ? window.SERVICEHUB_CONFIG.API_BASE_URL : "http://localhost:8080";
const API_BASE_URL = `${API_ROOT}/api/customer`;

const accessToken = localStorage.getItem("servicehub_access_token");

const userRole = localStorage.getItem("servicehub_user_role");

/* AUTH GUARD */

if (!accessToken || userRole !== "USER") {
  window.location.href = "/HTML/login.html";
}

/* ELEMENTS*/

const bookingsLoading = document.getElementById("bookings-loading");

const bookingsError = document.getElementById("bookings-error");

const bookingsErrorMessage = document.getElementById("bookings-error-message");

const retryBookingsButton = document.getElementById("retry-bookings-button");

const noBookings = document.getElementById("no-bookings");

const bookingsContainer = document.getElementById("bookings-container");

/* STATUS HELPERS */

function getStatusLabel(status) {
  const labels = {
    PENDING: "Waiting for Worker",

    ACCEPTED: "Accepted",

    IN_PROGRESS: "In Progress",

    WAITING_FOR_PAYMENT: "Waiting for Payment",

    AUTO_COMPLETED: "Auto Completed",

    COMPLETED: "Completed",

    CANCELLED: "Cancelled",

    NO_WORKER: "No Worker Found",

    FAILED: "Failed",

    WORKER_CANCELLED: "Worker Cancelled",

    WORKER_CANNOT_COMPLETE: "Worker Could Not Complete",
  };

  return labels[status] || status;
}

function getStatusClasses(status) {
  const classes = {
    PENDING: "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",

    ACCEPTED: "bg-blue-500/10 text-blue-400 border-blue-500/30",

    IN_PROGRESS: "bg-purple-500/10 text-purple-400 border-purple-500/30",

    WAITING_FOR_PAYMENT:
      "bg-orange-500/10 text-orange-400 border-orange-500/30",

    COMPLETED: "bg-green-500/10 text-green-400 border-green-500/30",

    REVIEWED: "bg-emerald-500/10 text-emerald-400 border-emerald-500/30",

    CANCELLED: "bg-gray-500/10 text-gray-400 border-gray-500/30",

    NO_WORKER: "bg-orange-500/10 text-orange-400 border-orange-500/30",

    FAILED: "bg-red-500/10 text-red-400 border-red-500/30",

    WORKER_CANCELLED:
      "bg-gray-500/10 text-gray-400 border-gray-500/30",

    WORKER_CANNOT_COMPLETE:
      "bg-red-500/10 text-red-400 border-red-500/30"
  };

  return classes[status] || "bg-gray-500/10 text-gray-400 border-gray-500/30";
}


function getStatusDescription(status) {
  const descriptions = {
    PENDING: "Your request is waiting for a worker to accept it.",

    ACCEPTED: "A worker has accepted your booking.",

    IN_PROGRESS: "Your service is currently in progress.",

    WAITING_FOR_PAYMENT:
      "Your service is complete. Please pay the final billed amount.",

    COMPLETED: "Your service has been successfully completed.",

    REVIEWED: "Your booking is complete. Thank you for sharing your feedback!",

    CANCELLED: "This booking was cancelled.",

    NO_WORKER:
      "No worker accepted your request within 30 minutes. You can try booking this service again.",

    FAILED: "This booking could not be completed.",

    WORKER_CANCELLED:
      "The worker cancelled the booking before starting the service. You can request this service again.",

    WORKER_CANNOT_COMPLETE:
      "The worker could not complete the service after starting it. Please contact ServiceHub if you need further assistance."
  };

  return descriptions[status] || "Booking status information is unavailable.";
}

/* PAYMENT HELPERS */

function formatCurrency(val) {
  const num = Number(val || 0);
  return `₹${num.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function getPaymentDisplayDetails(booking) {
  // 1. Confirmed successful payment
  if (booking.paymentStatus === "SUCCESS") {
    const isCash = booking.paymentMethod === "CASH";
    const amountStr =
      booking.paidAmount != null ? formatCurrency(booking.paidAmount) : "Paid";
    const paidAtParts = booking.paidAt ? formatDateTime(booking.paidAt) : null;
    const paidAtText =
      paidAtParts && paidAtParts.date !== "Not available"
        ? `on ${paidAtParts.date} · ${paidAtParts.time}`
        : "";

    if (isCash) {
      return {
        badgeText: "Paid in Cash",
        badgeClasses: "bg-teal-900/30 text-teal-400 border-teal-700/40",
        icon: "fa-solid fa-money-bill-wave text-teal-400 text-sm",
        methodTitle: "Cash Payment",
        amountText: amountStr,
        amountSubtext: "Paid to worker",
        subtext: `Cash receipt confirmed by service professional ${paidAtText}`.trim(),
        isPaid: true,
      };
    } else {
      return {
        badgeText: "Paid Online",
        badgeClasses: "bg-emerald-900/30 text-emerald-400 border-emerald-700/40",
        icon: "fa-solid fa-credit-card text-emerald-400 text-sm",
        methodTitle: "Online Payment (Razorpay)",
        amountText: amountStr,
        amountSubtext: "Paid online",
        subtext: `Online payment verified successfully ${paidAtText}`.trim(),
        isPaid: true,
      };
    }
  }

  // 2. Pending payment attempt
  if (
    booking.paymentStatus === "PENDING" ||
    booking.paymentStatus === "CREATED"
  ) {
    const isCash = booking.paymentMethod === "CASH";
    const dueAmount =
      booking.paymentAmount != null
        ? formatCurrency(booking.paymentAmount)
        : booking.finalBilledPrice != null
          ? formatCurrency(booking.finalBilledPrice)
          : "Pending";

    if (isCash) {
      return {
        badgeText: "Cash Due on Completion",
        badgeClasses: "bg-amber-900/30 text-amber-400 border-amber-700/40",
        icon: "fa-solid fa-hand-holding-dollar text-amber-400 text-sm",
        methodTitle: "Cash on Delivery",
        amountText: dueAmount,
        amountSubtext: "Cash amount due",
        subtext:
          "Pay in cash directly to your service professional upon completion.",
        isPaid: false,
      };
    } else {
      return {
        badgeText: "Payment Pending",
        badgeClasses: "bg-amber-900/30 text-amber-400 border-amber-700/40",
        icon: "fa-solid fa-clock-rotate-left text-amber-400 text-sm",
        methodTitle: "Online Payment",
        amountText: dueAmount,
        amountSubtext: "Awaiting confirmation",
        subtext: "Online payment attempt is processing or pending verification.",
        isPaid: false,
      };
    }
  }

  // 3. Failed payment attempt
  if (booking.paymentStatus === "FAILED") {
    return {
      badgeText: "Payment Failed",
      badgeClasses: "bg-rose-900/30 text-rose-400 border-rose-700/40",
      icon: "fa-solid fa-triangle-exclamation text-rose-400 text-sm",
      methodTitle:
        booking.paymentMethod === "CASH" ? "Cash Payment" : "Online Payment",
      amountText:
        booking.paymentAmount != null
          ? formatCurrency(booking.paymentAmount)
          : "Failed",
      amountSubtext: "Attempt failed",
      subtext: "Payment was unsuccessful. Please retry payment or contact support.",
      isPaid: false,
    };
  }

  // 4. Refunded payment
  if (booking.paymentStatus === "REFUNDED") {
    return {
      badgeText: "Refunded",
      badgeClasses: "bg-purple-900/30 text-purple-400 border-purple-700/40",
      icon: "fa-solid fa-rotate-left text-purple-400 text-sm",
      methodTitle:
        booking.paymentMethod === "CASH" ? "Cash Refund" : "Online Refund",
      amountText:
        booking.paymentAmount != null
          ? formatCurrency(booking.paymentAmount)
          : booking.paidAmount != null
            ? formatCurrency(booking.paidAmount)
            : "Refunded",
      amountSubtext: "Refund processed",
      subtext: "Payment has been refunded to your original payment source.",
      isPaid: false,
    };
  }

  // 5. Cancelled payment attempt
  if (booking.paymentStatus === "CANCELLED") {
    return {
      badgeText: "Payment Cancelled",
      badgeClasses: "bg-gray-800 text-gray-400 border-gray-700",
      icon: "fa-solid fa-ban text-gray-400 text-sm",
      methodTitle: "Cancelled",
      amountText:
        booking.paymentAmount != null
          ? formatCurrency(booking.paymentAmount)
          : "—",
      amountSubtext: "Cancelled attempt",
      subtext: "This payment attempt was cancelled.",
      isPaid: false,
    };
  }

  // 6. No payment attempt recorded yet
  if (booking.status === "WAITING_FOR_PAYMENT") {
    const billedAmount =
      booking.finalBilledPrice != null
        ? formatCurrency(booking.finalBilledPrice)
        : "Pending bill";
    return {
      badgeText: "Awaiting Payment",
      badgeClasses: "bg-orange-900/30 text-orange-400 border-orange-700/40",
      icon: "fa-solid fa-receipt text-orange-400 text-sm",
      methodTitle: "Payment Required",
      amountText: billedAmount,
      amountSubtext: "Billed price",
      subtext: "Service is complete. Please pay the billed amount on your dashboard.",
      isPaid: false,
    };
  }

  if (booking.status === "COMPLETED" || booking.status === "REVIEWED") {
    return {
      badgeText: "Payment Recorded",
      badgeClasses: "bg-gray-800 text-gray-400 border-gray-700",
      icon: "fa-solid fa-circle-check text-gray-400 text-sm",
      methodTitle: "Completed Booking",
      amountText:
        booking.finalBilledPrice != null
          ? formatCurrency(booking.finalBilledPrice)
          : "—",
      amountSubtext: "Final price",
      subtext: "Service completed.",
      isPaid: false,
    };
  }

  // Ongoing / Pending / Cancelled before billing
  return {
    badgeText: "Not Due Yet",
    badgeClasses: "bg-gray-800/60 text-gray-400 border-gray-700/60",
    icon: "fa-solid fa-clock text-gray-500 text-sm",
    methodTitle: "Payment",
    amountText: "—",
    amountSubtext: "Upon completion",
    subtext: "Final billing is calculated after the service is completed.",
    isPaid: false,
  };
}


/* DATE /  TIME*/

function formatDateTime(value) {
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatDateTimeParts(value);
  }

  if (!value) {
    return {
      date: "Not available",
      time: "Not available",
    };
  }

  let str = String(value).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "Z";
  }
  const date = new Date(str);

  if (Number.isNaN(date.getTime())) {
    return {
      date: "Not available",
      time: "Not available",
    };
  }

  return {
    date: new Intl.DateTimeFormat("en-IN", {
      timeZone: "Asia/Kolkata",
      day: "2-digit",
      month: "short",
      year: "numeric",
    }).format(date),

    time: new Intl.DateTimeFormat("en-IN", {
      timeZone: "Asia/Kolkata",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    }).format(date),
  };
}

/* LOAD BOOKINGS */

async function loadBookings() {
  showLoading();

  try {
    const response = await fetch(`${API_BASE_URL}/bookings`, {
      method: "GET",

      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (response.status === 401) {
      handleUnauthorized();
      return;
    }

    if (!response.ok) {
      throw new Error("Failed to load bookings.");
    }

    const bookings = await response.json();

    hideLoading();

    if (!Array.isArray(bookings) || bookings.length === 0) {
      showEmptyState();
      return;
    }

    /*
            Newest bookings first.
        */

    bookings.sort((a, b) =>
      window.SERVICEHUB_DATE
        ? window.SERVICEHUB_DATE.getTimestamp(b.createdAt) -
          window.SERVICEHUB_DATE.getTimestamp(a.createdAt)
        : new Date(b.createdAt) - new Date(a.createdAt),
    );

    renderBookings(bookings);
  } catch (error) {
    console.error("Booking loading error:", error);

    hideLoading();

    showError("Something went wrong while loading your bookings.");
  }
}

/* RENDER BOOKINGS */

function renderBookings(bookings) {
  bookingsContainer.innerHTML = "";

  bookings.forEach((booking) => {
    const card = createBookingCard(booking);

    bookingsContainer.appendChild(card);
  });

  bookingsContainer.classList.remove("hidden");

  noBookings.classList.add("hidden");
}

/* CREATE BOOKING  CARD*/

function createBookingCard(booking) {
  const card = document.createElement("article");

  card.className = "border border-gray-800 bg-gray-900/40 rounded-2xl p-6";

  const dateTime = formatDateTime(booking.createdAt);

  const statusClasses = getStatusClasses(booking.status);

  const statusLabel = getStatusLabel(booking.status);

  const description = getStatusDescription(booking.status);

  const serviceName = booking.serviceName || "Service request";

  const workerText = booking.workerName || "Worker not assigned";

  const paymentInfo = getPaymentDisplayDetails(booking);

  card.innerHTML = `

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
                    class="inline-flex w-fit items-center px-3 py-1.5 rounded-full border text-sm font-medium ${statusClasses}"
                >

                    ${statusLabel}

                </span>

            </div>


            <!-- Description -->

            <div>

                <p
                    class="text-gray-400 text-sm"
                >
                    ${description}
                </p>

            </div>


            <!-- Details -->

            <div
                class="grid grid-cols-1 md:grid-cols-3 gap-4 pt-4 border-t border-gray-800"
            >


                <!-- Worker -->

                <div
                    class="bg-gray-950/60 border border-gray-800 rounded-xl p-4"
                >

                    <div class="flex items-center gap-3">

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
                                Worker
                            </p>

                            <p
                                class="text-sm font-medium text-gray-200"
                            >
                                ${workerText}
                            </p>

                        </div>

                    </div>

                </div>



                <!-- Requested -->

                <div
                    class="bg-gray-950/60 border border-gray-800 rounded-xl p-4"
                >

                    <div class="flex items-center gap-3">

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
                                ${dateTime.date}
                                ·
                                ${dateTime.time}
                            </p>

                        </div>

                    </div>

                </div>



                <!-- Completed -->

                ${
                  booking.completedAt
                    ? `

                            <div
                                class="bg-green-950/10 border border-green-900/30 rounded-xl p-4"
                            >

                                <div class="flex items-center gap-3">

                                    <div
                                        class="w-10 h-10 rounded-lg bg-green-900/20 flex items-center justify-center shrink-0"
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
                                            ${formatDateTime(booking.completedAt).date}
                                            ·
                                            ${formatDateTime(booking.completedAt).time}
                                        </p>

                                    </div>

                                </div>

                            </div>

                        `
                    : ""
                }


            </div>


            <!-- Payment Information -->

            <div class="pt-4 border-t border-gray-800">

                <div class="bg-gray-950/70 border border-gray-800/80 rounded-xl p-4">

                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">

                        <div class="flex items-center gap-3">

                            <div class="w-10 h-10 rounded-lg bg-gray-900 border border-gray-800 flex items-center justify-center shrink-0">
                                <i class="${paymentInfo.icon}"></i>
                            </div>

                            <div>
                                <div class="flex items-center gap-2 flex-wrap">
                                    <p class="text-xs text-gray-400 uppercase tracking-wider font-medium">Payment Information</p>
                                    <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border ${paymentInfo.badgeClasses}">
                                        ${paymentInfo.badgeText}
                                    </span>
                                </div>

                                <p class="text-sm font-medium text-gray-200 mt-0.5">
                                    ${paymentInfo.methodTitle}
                                </p>
                            </div>

                        </div>

                        <div class="sm:text-right pl-13 sm:pl-0">
                            <p class="text-xs text-gray-500 mb-0.5">${paymentInfo.amountSubtext}</p>
                            <p class="text-base font-bold ${paymentInfo.isPaid ? "text-emerald-400" : "text-white"}">
                                ${paymentInfo.amountText}
                            </p>
                        </div>

                    </div>

                    ${
                      paymentInfo.subtext
                        ? `
                        <p class="text-xs text-gray-400 mt-2.5 pt-2.5 border-t border-gray-900 flex items-center gap-1.5">
                            <i class="fa-solid fa-circle-info text-gray-500 text-[10px]"></i>
                            <span>${paymentInfo.subtext}</span>
                        </p>
                    `
                        : ""
                    }

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
                            Your Note
                        </p>

                        <p
                            class="text-sm text-gray-300"
                        >
                            ${booking.customerNote}
                        </p>

                    </div>

                    `
                : ""
            }


        </div>

    `;

  return card;
}

/* UI STATES */

function showLoading() {
  bookingsLoading.classList.remove("hidden");

  bookingsError.classList.add("hidden");

  noBookings.classList.add("hidden");

  bookingsContainer.classList.add("hidden");
}

function hideLoading() {
  bookingsLoading.classList.add("hidden");
}

function showEmptyState() {
  bookingsError.classList.add("hidden");

  noBookings.classList.remove("hidden");

  bookingsContainer.classList.add("hidden");
}

function showError(message) {
  bookingsErrorMessage.textContent = message;

  bookingsError.classList.remove("hidden");

  noBookings.classList.add("hidden");

  bookingsContainer.classList.add("hidden");
}

/* UNAUTHORIZED*/

function handleUnauthorized() {
  localStorage.removeItem("servicehub_access_token");

  localStorage.removeItem("servicehub_user_id");

  localStorage.removeItem("servicehub_user_email");

  localStorage.removeItem("servicehub_user_role");

  window.location.href = "/HTML/login.html";
}

/* RETRY*/

retryBookingsButton.addEventListener("click", loadBookings);

/* INITIALIZE*/

loadBookings();
