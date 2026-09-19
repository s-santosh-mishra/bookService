const API_BASE_URL = "http://localhost:8080/api/customer";

const accessToken = localStorage.getItem("servicehub_access_token");

const userRole = localStorage.getItem("servicehub_user_role");

/* AUTH GUARD */

if (!accessToken || userRole !== "USER") {
  window.location.href = "../HTML/login.html";
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

    COMPLETED: "bg-green-500/10 text-green-400 border-green-500/30",

    AUTO_COMPLETED: "bg-green-500/10 text-green-400 border-green-500/30",

    CANCELLED: "bg-gray-500/10 text-gray-400 border-gray-500/30",

    NO_WORKER: "bg-orange-500/10 text-orange-400 border-orange-500/30",

    FAILED: "bg-red-500/10 text-red-400 border-red-500/30",
  };

  return classes[status] || "bg-gray-500/10 text-gray-400 border-gray-500/30";
}

function getStatusDescription(status) {
  const descriptions = {
    PENDING: "Your request is waiting for a worker to accept it.",

    ACCEPTED: "A worker has accepted your booking.",

    IN_PROGRESS: "Your service is currently in progress.",

    AUTO_COMPLETED:
      "The service was automatically completed because the 30-minute confirmation window expired.",

    COMPLETED: "This service has been completed.",

    CANCELLED: "This booking was cancelled.",

    NO_WORKER:
      "No worker accepted your request within 30 minutes. You can try booking this service again.",

    FAILED: "This booking could not be completed.",

    WORKER_CANCELLED:
      "The worker cancelled the booking before starting the service. You can request this service again.",

    WORKER_CANNOT_COMPLETE:
      "The worker could not complete the service after starting it. Please contact ServiceHub if you need further assistance.",
  };

  return descriptions[status] || "Booking status information is unavailable.";
}

/* DATE /  TIME*/

function formatDateTime(value) {
  if (!value) {
    return {
      date: "Not available",
      time: "Not available",
    };
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return {
      date: "Not available",
      time: "Not available",
    };
  }

  return {
    date: date.toLocaleDateString([], {
      day: "2-digit",
      month: "short",
      year: "numeric",
    }),

    time: date.toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    }),
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

    bookings.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

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

  window.location.href = "../HTML/login.html";
}

/* RETRY*/

retryBookingsButton.addEventListener("click", loadBookings);

/* INITIALIZE*/

loadBookings();
