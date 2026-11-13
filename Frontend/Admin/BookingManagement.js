const API_ROOT = window.SERVICEHUB_CONFIG ? window.SERVICEHUB_CONFIG.API_BASE_URL : "http://localhost:8080";
const API_BASE_URL = `${API_ROOT}/api/admin/bookings`;
const SERVICE_API_URL = `${API_ROOT}/api/admin/services`;
const BOOKING_ACCESS_TOKEN_KEY = "servicehub_access_token";

const bookingTableBody = document.getElementById("bookingTableBody");
const bookingCount = document.getElementById("bookingCount");
const bookingSearch = document.getElementById("bookingSearch");
const bookingDetailsModal = document.getElementById("bookingDetailsModal");
const cancelBookingModal = document.getElementById("cancelBookingModal");

let bookings = [];
let currentBookingDetails = null;

const filterIds = [
  "statusFilter",
  "serviceFilter",
  "customerFilter",
  "workerFilter",
  "createdFromFilter",
  "createdToFilter",
  "paymentMethodFilter",
  "paymentStatusFilter",
  "sortFilter",
];

filterIds.forEach((id) => {
  document.getElementById(id).addEventListener("change", applyFilters);
});

bookingSearch.addEventListener("input", applyFilters);

document.getElementById("clearSearchButton").addEventListener("click", () => {
  bookingSearch.value = "";
  applyFilters();
});

document
  .getElementById("clearFiltersButton")
  .addEventListener("click", clearFilters);

bookingTableBody.addEventListener("click", (event) => {
  const button = event.target.closest(".view-booking-button");

  if (button) {
    viewBooking(button.dataset.bookingId);
  }
});

document
  .getElementById("closeBookingDetails")
  .addEventListener("click", closeBookingDetails);

document
  .getElementById("closeBookingDetailsFooter")
  .addEventListener("click", closeBookingDetails);

document
  .getElementById("bookingDetailsOverlay")
  .addEventListener("click", closeBookingDetails);

document
  .getElementById("cancelBookingButton")
  .addEventListener("click", openCancellationDialog);

document
  .getElementById("closeCancelBooking")
  .addEventListener("click", closeCancellationDialog);

document
  .getElementById("dismissCancelBooking")
  .addEventListener("click", closeCancellationDialog);

document
  .getElementById("cancelBookingOverlay")
  .addEventListener("click", closeCancellationDialog);

document
  .getElementById("cancelBookingForm")
  .addEventListener("submit", submitCancellation);

async function loadBookings(clearMessage = true) {
  if (clearMessage) {
    hideMessage();
  }

  bookingTableBody.innerHTML = `
    <tr>
      <td colspan="8" class="px-6 py-16 text-center text-gray-500">
        Loading bookings...
      </td>
    </tr>
  `;

  const token = sessionStorage.getItem(BOOKING_ACCESS_TOKEN_KEY);

  try {
    const [bookingResponse, serviceResponse] = await Promise.all([
      fetch(API_BASE_URL, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }),
      fetch(SERVICE_API_URL, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }).catch(() => null),
    ]);

    if (handleAuthorizationFailure(bookingResponse)) {
      return false;
    }

    if (!bookingResponse.ok) {
      throw new Error(
        await getResponseError(bookingResponse, "Failed to load bookings."),
      );
    }

    const bookingData = await bookingResponse.json();

    if (!Array.isArray(bookingData)) {
      throw new Error("The booking response was not a list.");
    }

    bookings = bookingData;

    let services = [];
    let serviceOptionsWarning = false;

    if (serviceResponse && handleAuthorizationFailure(serviceResponse)) {
      return false;
    }

    if (serviceResponse && serviceResponse.ok) {
      services = await serviceResponse.json();
    } else {
      serviceOptionsWarning = true;
    }

    populateFilters(services);
    applyFilters();

    if (serviceOptionsWarning) {
      showMessage(
        "Service options could not be loaded. Showing services with bookings.",
        "warning",
      );
    }

    return true;
  } catch (error) {
    console.error("Error loading admin bookings:", error);

    bookingTableBody.innerHTML = `
      <tr>
        <td colspan="8" class="px-6 py-12 text-center text-red-400">
          ${escapeHtml(error.message || "Unable to load bookings.")}
        </td>
      </tr>
    `;

    bookingCount.textContent = "Bookings unavailable";
    showMessage(error.message || "Unable to load bookings.", "error");
    return false;
  }
}

function populateFilters(services) {
  const serviceFilter = document.getElementById("serviceFilter");
  const customerFilter = document.getElementById("customerFilter");
  const workerFilter = document.getElementById("workerFilter");

  const selectedService = serviceFilter.value;
  const selectedCustomer = customerFilter.value;
  const selectedWorker = workerFilter.value;
  const serviceOptions = new Map();

  services.forEach((service) => {
    if (service.serviceId) {
      serviceOptions.set(service.serviceId, service.serviceName || "Unnamed service");
    }
  });

  if (serviceOptions.size === 0) {
    bookings.forEach((booking) => {
      if (booking.serviceId) {
        serviceOptions.set(
          booking.serviceId,
          booking.serviceName || "Unnamed service",
        );
      }
    });
  }

  serviceFilter.innerHTML = `
    <option value="">All Services</option>
    ${[...serviceOptions.entries()]
      .sort((left, right) => left[1].localeCompare(right[1]))
      .map(
        ([serviceId, serviceName]) => `
          <option value="${escapeHtml(serviceId)}">
            ${escapeHtml(serviceName)}
          </option>
        `,
      )
      .join("")}
  `;

  if (serviceOptions.has(selectedService)) {
    serviceFilter.value = selectedService;
  }

  const customers = new Map();
  const workers = new Map();

  bookings.forEach((booking) => {
    if (booking.customerId) {
      customers.set(
        booking.customerId,
        booking.customerName || "Unnamed customer",
      );
    }

    if (booking.workerId) {
      workers.set(booking.workerId, booking.workerName || "Unnamed worker");
    }
  });

  customerFilter.innerHTML = `
    <option value="">All Customers</option>
    ${[...customers.entries()]
      .sort((left, right) => left[1].localeCompare(right[1]))
      .map(
        ([customerId, customerName]) => `
          <option value="${escapeHtml(customerId)}">
            ${escapeHtml(customerName)}
          </option>
        `,
      )
      .join("")}
  `;

  if (customers.has(selectedCustomer)) {
    customerFilter.value = selectedCustomer;
  }

  workerFilter.innerHTML = `
    <option value="">All Workers</option>
    <option value="unassigned">Unassigned</option>
    ${[...workers.entries()]
      .sort((left, right) => left[1].localeCompare(right[1]))
      .map(
        ([workerId, workerName]) => `
          <option value="${escapeHtml(workerId)}">
            ${escapeHtml(workerName)}
          </option>
        `,
      )
      .join("")}
  `;

  if (
    selectedWorker === "unassigned" ||
    workers.has(selectedWorker)
  ) {
    workerFilter.value = selectedWorker;
  }
}

function applyFilters() {
  const searchValue = bookingSearch.value.trim().toLowerCase();
  const status = document.getElementById("statusFilter").value;
  const serviceId = document.getElementById("serviceFilter").value;
  const customerId = document.getElementById("customerFilter").value;
  const workerId = document.getElementById("workerFilter").value;
  const createdFrom = document.getElementById("createdFromFilter").value;
  const createdTo = document.getElementById("createdToFilter").value;
  const paymentMethod = document.getElementById("paymentMethodFilter").value;
  const paymentStatus = document.getElementById("paymentStatusFilter").value;
  const sortBy = document.getElementById("sortFilter").value;

  if (createdFrom && createdTo && createdFrom > createdTo) {
    bookingTableBody.innerHTML = `
      <tr>
        <td colspan="8" class="px-6 py-12 text-center text-yellow-400">
          The start date must be on or before the end date.
        </td>
      </tr>
    `;
    bookingCount.textContent = "Invalid date range";
    showMessage(
      "The created-from date must be on or before the created-to date.",
      "error",
    );
    return;
  }

  hideMessage();

  const filteredBookings = bookings.filter((booking) => {
    if (status && booking.status !== status) {
      return false;
    }

    if (serviceId && booking.serviceId !== serviceId) {
      return false;
    }

    if (customerId && booking.customerId !== customerId) {
      return false;
    }

    if (workerId === "unassigned" && booking.workerId) {
      return false;
    }

    if (workerId && workerId !== "unassigned" && booking.workerId !== workerId) {
      return false;
    }

    const createdDate = getLocalDateValue(booking.createdAt);

    if (createdFrom && (!createdDate || createdDate < createdFrom)) {
      return false;
    }

    if (createdTo && (!createdDate || createdDate > createdTo)) {
      return false;
    }

    if (
      paymentMethod === "none" &&
      (booking.paymentMethods || []).length > 0
    ) {
      return false;
    }

    if (
      paymentMethod &&
      paymentMethod !== "none" &&
      !(booking.paymentMethods || []).includes(paymentMethod)
    ) {
      return false;
    }

    if (
      paymentStatus === "none" &&
      (booking.paymentStatuses || []).length > 0
    ) {
      return false;
    }

    if (
      paymentStatus &&
      paymentStatus !== "none" &&
      !(booking.paymentStatuses || []).includes(paymentStatus)
    ) {
      return false;
    }

    if (searchValue) {
      const searchableFields = [
        booking.bookingId,
        booking.customerName,
        booking.customerPhone,
        booking.workerName,
        booking.workerPhone,
        booking.serviceName,
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

  sortBookings(filteredBookings, sortBy);
  renderBookings(filteredBookings);
}

function sortBookings(bookingList, sortBy) {
  bookingList.sort((left, right) => {
    if (sortBy === "created-asc" || sortBy === "created-desc") {
      const direction = sortBy === "created-asc" ? 1 : -1;
      return (
        direction * (getTimestamp(left.createdAt) - getTimestamp(right.createdAt))
      );
    }

    if (sortBy === "customer-asc" || sortBy === "customer-desc") {
      const direction = sortBy === "customer-asc" ? 1 : -1;
      return (
        direction *
        (left.customerName || "").localeCompare(right.customerName || "")
      );
    }

    if (sortBy === "amount-asc" || sortBy === "amount-desc") {
      const direction = sortBy === "amount-asc" ? 1 : -1;
      const leftAmount = Number(left.finalBilledPrice);
      const rightAmount = Number(right.finalBilledPrice);
      const leftMissing = left.finalBilledPrice === null;
      const rightMissing = right.finalBilledPrice === null;

      if (leftMissing !== rightMissing) {
        return leftMissing ? 1 : -1;
      }

      return direction * (leftAmount - rightAmount);
    }

    return 0;
  });
}

function renderBookings(bookingList) {
  bookingCount.textContent = `${bookingList.length} ${
    bookingList.length === 1 ? "booking" : "bookings"
  }`;

  if (bookingList.length === 0) {
    bookingTableBody.innerHTML = `
      <tr>
        <td colspan="8" class="px-6 py-16 text-center">
          <div class="flex flex-col items-center">
            <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-gray-800">
              <i class="fa-solid fa-calendar-xmark text-lg text-gray-600"></i>
            </div>
            <p class="text-gray-300">No bookings match these filters.</p>
            <p class="mt-1 text-sm text-gray-500">
              Try changing your search or clearing the filters.
            </p>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  bookingTableBody.innerHTML = bookingList
    .map(
      (booking) => `
        <tr class="border-b border-gray-800 last:border-0 transition-colors hover:bg-gray-800/40">
          <td class="max-w-[180px] break-all px-5 py-4 text-xs text-gray-400">
            ${escapeHtml(booking.bookingId)}
          </td>
          <td class="px-5 py-4">
            <p class="font-medium text-white">
              ${escapeHtml(booking.customerName || "N/A")}
            </p>
          </td>
          <td class="px-5 py-4 text-gray-300">
            ${escapeHtml(booking.workerName || "Unassigned")}
          </td>
          <td class="px-5 py-4 text-gray-300">
            ${escapeHtml(booking.serviceName || "N/A")}
          </td>
          <td class="px-5 py-4 text-sm text-gray-400">
            ${escapeHtml(formatDateTime(booking.createdAt))}
          </td>
          <td class="whitespace-nowrap px-5 py-4 text-gray-300">
            ${escapeHtml(formatMoney(booking.finalBilledPrice))}
          </td>
          <td class="px-5 py-4">
            ${getStatusBadge(booking.status)}
          </td>
          <td class="px-5 py-4 text-right">
            <button
              type="button"
              class="view-booking-button rounded-lg bg-gray-800 px-3 py-2 text-sm text-gray-300 transition-colors hover:bg-gray-700 hover:text-white"
              data-booking-id="${escapeHtml(booking.bookingId)}"
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

async function viewBooking(bookingId) {
  bookingDetailsModal.classList.remove("hidden");
  document.body.classList.add("overflow-hidden");
  document.getElementById("bookingDetailsTitle").textContent =
    "Booking Details";
  document.getElementById("bookingDetailsStatus").innerHTML = "";
  document.getElementById("bookingDetailsContent").innerHTML = `
    <p class="py-12 text-center text-gray-400">Loading booking details...</p>
  `;
  document.getElementById("cancelBookingButton").classList.add("hidden");

  const token = sessionStorage.getItem(BOOKING_ACCESS_TOKEN_KEY);

  try {
    const response = await fetch(`${API_BASE_URL}/${encodeURIComponent(bookingId)}`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (handleAuthorizationFailure(response)) {
      closeBookingDetails();
      return;
    }

    if (!response.ok) {
      throw new Error(
        await getResponseError(response, "Failed to load booking details."),
      );
    }

    currentBookingDetails = await response.json();
    renderBookingDetails(currentBookingDetails);
  } catch (error) {
    console.error("Error loading booking details:", error);
    document.getElementById("bookingDetailsContent").innerHTML = `
      <p class="py-12 text-center text-red-400">
        ${escapeHtml(error.message || "Unable to load booking details.")}
      </p>
    `;
  }
}

function renderBookingDetails(details) {
  const booking = details.booking;

  if (!booking) {
    throw new Error("Booking details are incomplete.");
  }

  document.getElementById("bookingDetailsTitle").textContent =
    `Booking ${booking.bookingId}`;
  document.getElementById("bookingDetailsStatus").innerHTML =
    getStatusBadge(booking.status);
  document.getElementById("cancelBookingButton").classList.toggle(
    "hidden",
    !isAdminCancellationAllowed(booking.status),
  );

  const address = details.customerAddress;
  const addressText = address
    ? [
        address.addressLine1,
        address.addressLine2,
        address.landmark,
        address.city,
        address.state,
        address.pinCode,
      ]
        .filter(Boolean)
        .join(", ")
    : "";

  const coordinates =
    details.serviceLatitude !== null &&
    details.serviceLatitude !== undefined &&
    details.serviceLongitude !== null &&
    details.serviceLongitude !== undefined
      ? `${details.serviceLatitude}, ${details.serviceLongitude}`
      : "";

  const lifecycleEvents = getLifecycleEvents(details.lifecycle);
  const billing = details.billing;
  const payments = details.payments || [];
  const cancellationInfo = getCancellationInfo(details);

  document.getElementById("bookingDetailsContent").innerHTML = `
    <div class="space-y-7">
      ${createDetailsSection("Booking Information", [
        ["Booking ID", booking.bookingId],
        ["Service", booking.serviceName],
        ["Category", booking.categoryName],
        ["Status", getStatusLabel(booking.status)],
        ["Booking Created Date", formatDateTime(booking.createdAt)],
        ["Last Updated", formatDateTime(details.lifecycle?.updatedAt)],
        ["Customer Note", details.customerNote],
      ])}

      ${createDetailsSection("Customer Information", [
        ["Name", booking.customerName],
        ["Phone", booking.customerPhone],
        ["Email", details.customerEmail],
        ["Customer Profile Address", addressText],
        ["Service Coordinates", coordinates],
      ])}

      ${createDetailsSection("Worker Information", [
        ["Worker", booking.workerName || "No worker assigned"],
        ["Phone", booking.workerPhone],
        ["Email", details.workerEmail],
        ["Assigned Service", booking.workerId ? booking.serviceName : "N/A"],
      ])}

      <section>
        <h3 class="mb-3 text-lg font-semibold text-white">Billing Information</h3>
        ${
          billing
            ? `<div class="grid gap-4 rounded-xl border border-gray-800 bg-gray-950/60 p-4 sm:grid-cols-2 lg:grid-cols-3">
                ${createDetailItem("Base Price / Hour", formatMoney(billing.basePricePerHour))}
                ${createDetailItem("Minimum Service Fee", formatMoney(billing.minimumServiceFee))}
                ${createDetailItem("Labour Charge", formatMoney(billing.labourCharge))}
                ${createDetailItem("Travel Charge", formatMoney(billing.travelCharge))}
                ${createDetailItem("Parts Cost", formatMoney(billing.partsCost))}
                ${createDetailItem("Additional Fees", formatMoney(billing.additionalFees))}
                ${createDetailItem("Billable Hours", billing.billableHours ?? "N/A")}
                ${createDetailItem("Final Bill Amount", formatMoney(billing.finalBilledPrice))}
              </div>`
            : `<p class="rounded-xl border border-gray-800 bg-gray-950/60 p-4 text-sm text-gray-500">
                No billing record is available for this booking.
              </p>`
        }
      </section>

      <section>
        <h3 class="mb-3 text-lg font-semibold text-white">Payment Information</h3>
        ${
          payments.length
            ? `<div class="space-y-3">
                ${payments.map(renderPayment).join("")}
              </div>`
            : `<p class="rounded-xl border border-gray-800 bg-gray-950/60 p-4 text-sm text-gray-500">
                No payment record is available for this booking.
              </p>`
        }
      </section>

      <section>
        <h3 class="mb-3 text-lg font-semibold text-white">Booking Lifecycle</h3>
        ${
          lifecycleEvents.length
            ? `<ol class="space-y-3 border-l border-gray-800 pl-5">
                ${lifecycleEvents
                  .map(
                    (event) => `
                      <li class="relative">
                        <span class="absolute -left-[25px] top-1.5 h-2.5 w-2.5 rounded-full bg-violet-400"></span>
                        <p class="text-sm font-medium text-gray-200">${escapeHtml(event.label)}</p>
                        <p class="mt-1 text-sm text-gray-500">${escapeHtml(formatDateTime(event.timestamp))}</p>
                      </li>
                    `,
                  )
                  .join("")}
              </ol>`
            : `<p class="text-sm text-gray-500">No lifecycle timestamps are available.</p>`
        }
      </section>

      ${
        cancellationInfo
          ? `<section>
              <h3 class="mb-3 text-lg font-semibold text-white">
                Cancellation or Failure Information
              </h3>
              <div class="rounded-xl border border-gray-800 bg-gray-950/60 p-4">
                <p class="text-sm text-gray-300">
                  ${escapeHtml(cancellationInfo)}
                </p>
              </div>
            </section>`
          : ""
      }
    </div>
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
  return `
    <div class="min-w-0">
      <p class="text-xs text-gray-500">${escapeHtml(label)}</p>
      <p class="mt-1 break-words text-sm text-gray-200">
        ${escapeHtml(value === null || value === undefined || value === "" ? "N/A" : value)}
      </p>
    </div>
  `;
}

function renderPayment(payment) {
  return `
    <div class="grid gap-4 rounded-xl border border-gray-800 bg-gray-950/60 p-4 sm:grid-cols-2 lg:grid-cols-4">
      ${createDetailItem("Payment ID", payment.paymentId)}
      ${createDetailItem("Method", payment.paymentMethod)}
      ${createDetailItem("Status", payment.status)}
      ${createDetailItem("Amount", formatMoney(payment.amount))}
      ${createDetailItem("Razorpay Order ID", payment.razorpayOrderId)}
      ${createDetailItem("Razorpay Payment ID", payment.razorpayPaymentId)}
      ${createDetailItem("Created", formatDateTime(payment.createdAt))}
      ${createDetailItem("Completed", formatIstDateTime(payment.paidAt))}
    </div>
  `;
}

function getLifecycleEvents(lifecycle) {
  if (!lifecycle) {
    return [];
  }

  return [
    ["createdAt", "Booking created"],
    ["acceptedAt", "Booking accepted"],
    ["startedAt", "Work started"],
    ["completionOtpRequestedAt", "Completion requested"],
    ["completedAt", "Booking completed"],
    ["workerCompletedAt", "Worker marked complete"],
    ["cancelledAt", "Booking cancelled"],
    ["failedAt", "Booking failed"],
    ["workerCancelledAt", "Worker cancelled"],
  ]
    .map(([key, label]) => ({
      label,
      timestamp: lifecycle[key],
    }))
    .filter((event) => event.timestamp)
    .sort(
      (left, right) =>
        getTimestamp(left.timestamp) - getTimestamp(right.timestamp),
    );
}

function getCancellationInfo(details) {
  const booking = details.booking;
  const reasons = [];

  if (details.adminCancellationReason) {
    reasons.push(`Admin cancellation reason: ${details.adminCancellationReason}`);
  }

  if (details.workerCancellationReason) {
    reasons.push(`Worker cancellation reason: ${details.workerCancellationReason}`);
  }

  if (details.workerCancellationMessage) {
    reasons.push(`Worker message: ${details.workerCancellationMessage}`);
  }

  if (
    (booking.status === "CANCELLED" ||
      booking.status === "WORKER_CANCELLED" ||
      booking.status === "FAILED" ||
      booking.status === "WORKER_CANNOT_COMPLETE") &&
    reasons.length === 0
  ) {
    reasons.push(
      booking.status === "FAILED" || booking.status === "WORKER_CANNOT_COMPLETE"
        ? "No failure reason is recorded."
        : "No cancellation reason is recorded.",
    );
  }

  return reasons.join(" ");
}

function isAdminCancellationAllowed(status) {
  return ["PENDING", "ACCEPTED", "IN_PROGRESS"].includes(status);
}

function openCancellationDialog() {
  const booking = currentBookingDetails?.booking;

  if (!booking || !isAdminCancellationAllowed(booking.status)) {
    return;
  }

  document.getElementById("cancelBookingId").textContent = booking.bookingId;
  document.getElementById("cancelBookingStatus").textContent =
    getStatusLabel(booking.status);
  document.getElementById("cancellationReason").value = "";
  document.getElementById("cancelBookingError").classList.add("hidden");
  cancelBookingModal.classList.remove("hidden");
  document.getElementById("cancellationReason").focus();
}

function closeCancellationDialog() {
  cancelBookingModal.classList.add("hidden");
}

async function submitCancellation(event) {
  event.preventDefault();

  const booking = currentBookingDetails?.booking;
  const reason = document.getElementById("cancellationReason").value.trim();
  const errorElement = document.getElementById("cancelBookingError");

  if (!booking || !isAdminCancellationAllowed(booking.status)) {
    return;
  }

  if (!reason) {
    showCancellationError("Enter a cancellation reason.");
    return;
  }

  if (reason.length > 1000) {
    showCancellationError("The reason cannot exceed 1000 characters.");
    return;
  }

  const submitButton = document.getElementById("confirmCancelBooking");
  const restore = submitButton && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(submitButton, "Cancelling...")
    : null;
  if (submitButton && window.SERVICEHUB_BUTTON && !restore) return;
  if (submitButton && !restore) {
    submitButton.disabled = true;
    submitButton.textContent = "Cancelling...";
  }
  errorElement.classList.add("hidden");

  const token = sessionStorage.getItem(BOOKING_ACCESS_TOKEN_KEY);

  try {
    const response = await fetch(
      `${API_BASE_URL}/${encodeURIComponent(booking.bookingId)}/cancel`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ reason }),
      },
    );

    if (handleAuthorizationFailure(response)) {
      return;
    }

    if (!response.ok) {
      throw new Error(
        await getResponseError(response, "Failed to cancel the booking."),
      );
    }

    currentBookingDetails = await response.json();
    closeCancellationDialog();
    renderBookingDetails(currentBookingDetails);

    if (await loadBookings(false)) {
      showMessage("Booking cancelled successfully.", "success");
    } else {
      showMessage(
        "Booking was cancelled, but the list could not be refreshed.",
        "warning",
      );
    }
  } catch (error) {
    console.error("Error cancelling booking:", error);
    showCancellationError(error.message || "Unable to cancel the booking.");
  } finally {
    if (restore) {
      restore();
    } else if (submitButton) {
      submitButton.disabled = false;
      submitButton.innerHTML = "Confirm Cancellation";
    }
  }
}

function showCancellationError(message) {
  const errorElement = document.getElementById("cancelBookingError");
  errorElement.textContent = message;
  errorElement.classList.remove("hidden");
}

function clearFilters() {
  filterIds.forEach((id) => {
    const element = document.getElementById(id);
    element.value = id === "sortFilter" ? "created-desc" : "";
  });

  applyFilters();
}

function closeBookingDetails() {
  bookingDetailsModal.classList.add("hidden");
  document.body.classList.remove("overflow-hidden");
  currentBookingDetails = null;
}

function getStatusBadge(status) {
  const statusStyles = {
    PENDING: "bg-yellow-500/10 text-yellow-400",
    ACCEPTED: "bg-blue-500/10 text-blue-400",
    IN_PROGRESS: "bg-blue-500/10 text-blue-400",
    WAITING_FOR_PAYMENT: "bg-orange-500/10 text-orange-400",
    COMPLETED: "bg-green-500/10 text-green-400",
    REVIEWED: "bg-green-500/10 text-green-400",
    CANCELLED: "bg-red-500/10 text-red-400",
    NO_WORKER: "bg-gray-700 text-gray-300",
    FAILED: "bg-red-500/10 text-red-400",
    WORKER_CANCELLED: "bg-red-500/10 text-red-400",
    WORKER_CANNOT_COMPLETE: "bg-red-500/10 text-red-400",
  };

  return `
    <span class="inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-medium ${
      statusStyles[status] || "bg-gray-700 text-gray-300"
    }">
      ${escapeHtml(getStatusLabel(status))}
    </span>
  `;
}

function getStatusLabel(status) {
  return String(status || "UNKNOWN")
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function getLocalDateValue(dateValue) {
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.getLocalDateValue(dateValue);
  }

  let str = String(dateValue || "").trim();
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

function getTimestamp(dateValue) {
  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.getTimestamp(dateValue);
  }
  let str = String(dateValue || "").trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "Z";
  }
  const timestamp = new Date(str).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatDateTime(dateValue) {
  if (!dateValue) {
    return "N/A";
  }

  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatDateTime(dateValue, { fallback: "N/A" });
  }

  let str = String(dateValue).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "Z";
  }
  const date = new Date(str);

  if (Number.isNaN(date.getTime())) {
    return "N/A";
  }

  return new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatIstDateTime(dateValue) {
  if (!dateValue) {
    return "N/A";
  }

  if (window.SERVICEHUB_DATE) {
    return window.SERVICEHUB_DATE.formatIstDateTime(dateValue, { fallback: "N/A" });
  }

  let str = String(dateValue).trim();
  const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
  if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
    str = str.replace(" ", "T") + "+05:30";
  }
  const date = new Date(str);

  if (Number.isNaN(date.getTime())) {
    return "N/A";
  }

  return new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatMoney(amount) {
  if (amount === null || amount === undefined || amount === "") {
    return "N/A";
  }

  const numericAmount = Number(amount);

  if (!Number.isFinite(numericAmount)) {
    return "N/A";
  }

  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
  }).format(numericAmount);
}

function handleAuthorizationFailure(response) {
  if (response.status !== 401 && response.status !== 403) {
    return false;
  }

  window.location.href = "/Admin/adminLogin.html";
  return true;
}

async function getResponseError(response, fallbackMessage) {
  const errorData = await response.json().catch(() => null);
  return errorData?.error || errorData?.message || fallbackMessage;
}

function showMessage(message, type) {
  const messageElement = document.getElementById("bookingMessage");
  const typeStyles = {
    error: "border-red-500/20 bg-red-500/10 text-red-400",
    success: "border-green-500/20 bg-green-500/10 text-green-400",
    warning: "border-yellow-500/20 bg-yellow-500/10 text-yellow-400",
  };

  messageElement.textContent = message;
  messageElement.className = `mb-5 rounded-lg border px-4 py-3 text-sm ${
    typeStyles[type] || typeStyles.error
  }`;
}

function hideMessage() {
  const messageElement = document.getElementById("bookingMessage");
  messageElement.classList.add("hidden");
}

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

loadBookings();
