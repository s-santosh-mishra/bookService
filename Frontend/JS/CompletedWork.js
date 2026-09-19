const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";

const API_BASE_URL = "http://localhost:8080/api/worker";

// Authentication

function checkWorkerLogin() {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  const role = localStorage.getItem(ROLE_KEY);

  if (!token || role !== "WORKER") {
    window.location.href = "../HTML/login.html";

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

    window.location.href = "../HTML/login.html";

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

// Load Completed Work

async function loadCompletedWork() {
  const container = document.getElementById("completedWorkContainer");

  if (!container) {
    return;
  }

  try {
    const bookings = await workerApiRequest(`${API_BASE_URL}/bookings`);

    const completedBookings = (bookings || [])
      .filter((booking) => booking.status === "COMPLETED")
      .sort(
        (a, b) =>
          new Date(b.completedAt || b.updatedAt) -
          new Date(a.completedAt || a.updatedAt),
      );

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

                    <div
                        class="flex items-center justify-between gap-4"
                    >

                        <div
                            class="flex items-center gap-2"
                        >

                            <i
                                class="fa-solid fa-credit-card text-gray-500"
                            ></i>

                            <span
                                class="text-sm text-gray-400"
                            >
                                Payment
                            </span>

                        </div>


                        <span
                            class="text-sm text-gray-500"
                        >
                            Not available yet
                        </span>

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

  const date = new Date(dateString);

  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return date.toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
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
