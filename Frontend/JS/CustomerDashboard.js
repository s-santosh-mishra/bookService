/* Authentication Guard */
const accessToken = localStorage.getItem("servicehub_access_token");
const userId = localStorage.getItem("servicehub_user_id");
const userRole = localStorage.getItem("servicehub_user_role");

if (!accessToken || !userId || userRole !== "USER") {
  window.location.href = "login.html";
}

const API_BASE_URL = "http://localhost:8080/api/customer";

/* Dashboard Elements */

const welcomeMessage = document.getElementById("welcomeMessage");

const serviceSearch = document.getElementById("serviceSearch");

const categoryContainer = document.getElementById("categoryContainer");

const searchResultsSection = document.getElementById("searchResultsSection");

const searchResultsContainer = document.getElementById(
  "searchResultsContainer",
);

const categoryServicesSection = document.getElementById(
  "categoryServicesSection",
);

const categoryServicesContainer = document.getElementById(
  "categoryServicesContainer",
);

const categoryServicesHeading = document.getElementById(
  "categoryServicesHeading",
);

const categoryServicesDescription = document.getElementById(
  "categoryServicesDescription",
);

const popularServicesContainer = document.getElementById(
  "popularServicesContainer",
);

/* Active Booking Elements */

const activeBookingCard = document.getElementById("activeBookingCard");

const noActiveBooking = document.getElementById("noActiveBooking");

const activeBookingService = document.getElementById("activeBookingService");

const activeBookingStatus = document.getElementById("activeBookingStatus");

const activeBookingDescription = document.getElementById(
  "activeBookingDescription",
);

const activeBookingWorker = document.getElementById("activeBookingWorker");

const activeBookingDate = document.getElementById("activeBookingDate");

const activeBookingTime = document.getElementById("activeBookingTime");

const viewActiveBookingButton = document.getElementById(
  "viewActiveBookingButton",
);

/* Create Booking Modal */

const createBookingModal = document.getElementById("createBookingModal");

const createBookingService = document.getElementById("createBookingService");

const customerBookingNote = document.getElementById("customerBookingNote");

const confirmCreateBookingButton = document.getElementById(
  "confirmCreateBookingButton",
);

const closeCreateBookingButton = document.getElementById(
  "closeCreateBookingButton",
);

const cancelCreateBookingButton = document.getElementById(
  "cancelCreateBookingButton",
);

/* Booking Details Modal */

const bookingDetailsModal = document.getElementById("bookingDetailsModal");

const closeBookingDetailsButton = document.getElementById(
  "closeBookingDetailsButton",
);

const bookingDetailsService = document.getElementById("bookingDetailsService");

const bookingDetailsStatus = document.getElementById("bookingDetailsStatus");

const bookingDetailsDescription = document.getElementById(
  "bookingDetailsDescription",
);

const bookingDetailsWorker = document.getElementById("bookingDetailsWorker");

const bookingDetailsCreated = document.getElementById("bookingDetailsCreated");

const bookingDetailsTimeline = document.getElementById(
  "bookingDetailsTimeline",
);

const bookingDetailsNoteContainer = document.getElementById(
  "bookingDetailsNoteContainer",
);

const bookingDetailsNote = document.getElementById("bookingDetailsNote");

const customerCompletionContainer = document.getElementById(
  "customerCompletionContainer",
);

const customerCancelContainer = document.getElementById(
  "customerCancelContainer",
);

const confirmCustomerCompletionButton = document.getElementById(
  "confirmCustomerCompletionButton",
);

/* Dashboard Data */

let customerServices = [];

let selectedCategoryId = null;

let selectedBookingService = null;

let activeBooking = null;

let completionTimer = null;

/* Category Icons */

const categoryIconMap = {
  plumbing: "fa-faucet-drip",

  electrical: "fa-bolt",

  cleaning: "fa-broom",

  repair: "fa-screwdriver-wrench",

  "ac service": "fa-snowflake",

  "home services": "fa-house",
};

/* Service Icons */

const serviceIconMap = {
  pipe: "fa-faucet",

  plumbing: "fa-faucet-drip",

  fan: "fa-fan",

  ac: "fa-snowflake",

  electrical: "fa-bolt",

  cleaning: "fa-broom",

  repair: "fa-screwdriver-wrench",
};

/* Get Category Icon */

function getCategoryIcon(name) {
  return categoryIconMap[(name || "").toLowerCase().trim()] || "fa-house";
}

/* Get Service Icon */

function getServiceIcon(name) {
  const value = (name || "").toLowerCase();

  for (const keyword in serviceIconMap) {
    if (value.includes(keyword)) {
      return serviceIconMap[keyword];
    }
  }

  return "fa-screwdriver-wrench";
}

/* HTML Escape */

function escapeHtml(value) {
  const div = document.createElement("div");

  div.textContent = value ?? "";

  return div.innerHTML;
}

/* Load Customer Name */

async function loadCustomerName() {
  try {
    const profile = await apiRequest(`${API_BASE_URL}/profile`);

    const name = profile?.fullName || "Customer";

    localStorage.setItem("servicehub_user_name", name);

    welcomeMessage.textContent = `Welcome back, ${name}!`;
  } catch (error) {
    console.error("Failed to load customer profile:", error);

    const name = localStorage.getItem("servicehub_user_name");

    welcomeMessage.textContent = name
      ? `Welcome back, ${name}!`
      : "Welcome back!";
  }
}

/* API Request */

async function apiRequest(url, options = {}) {
  const response = await fetch(url, {
    ...options,

    headers: {
      Authorization: `Bearer ${accessToken}`,

      ...(options.headers || {}),
    },
  });

  if (response.status === 401) {
    handleUnauthorized();

    throw new Error("Unauthorized");
  }

  let data = null;

  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    data = await response.json();
  }

  if (!response.ok) {
    throw new Error(data?.message || data?.error || "Request failed.");
  }

  return data;
}

/* Load Categories */

async function loadCustomerCategories() {
  try {
    const categories = await apiRequest(`${API_BASE_URL}/categories`);

    renderCategories(categories || []);
  } catch (error) {
    console.error("Category loading error:", error);
  }
}

/* Render Categories */

function renderCategories(categories) {
  categoryContainer.innerHTML = "";

  if (categories.length === 0) {
    categoryContainer.innerHTML = `
            <p class="col-span-full
                      text-center
                      text-gray-500
                      py-8">

                No services are currently available.

            </p>
        `;

    return;
  }

  categories.forEach((category) => {
    const button = document.createElement("button");

    button.className =
      "category-card bg-gray-900 " +
      "border border-gray-800 " +
      "rounded-2xl p-5 text-center " +
      "hover:border-violet-500/50 " +
      "hover:bg-gray-900/80 transition";

    button.dataset.categoryId = category.categoryId;

    button.innerHTML = `

                <i class="fa-solid
                          ${getCategoryIcon(category.categoryName)}
                          text-2xl
                          text-violet-400
                          mb-3"></i>

                <p class="font-medium">

                    ${escapeHtml(category.categoryName)}

                </p>

            `;

    button.addEventListener("click", () => selectCategory(category, button));

    categoryContainer.appendChild(button);
  });
}

/* Select Category */

function selectCategory(category, button) {
  if (selectedCategoryId === category.categoryId) {
    selectedCategoryId = null;

    document.querySelectorAll(".category-card").forEach((item) => {
      item.classList.remove("border-violet-500", "bg-violet-900/20");
    });

    hideCategoryResults();

    return;
  }

  selectedCategoryId = category.categoryId;

  document.querySelectorAll(".category-card").forEach((item) => {
    item.classList.remove("border-violet-500", "bg-violet-900/20");
  });

  button.classList.add("border-violet-500", "bg-violet-900/20");

  renderCategoryResults(category);
}

/* Load Services */

async function loadCustomerServices() {
  try {
    customerServices = (await apiRequest(`${API_BASE_URL}/services`)) || [];

    /*
            Popular Services is independent.
            It is rendered once and never
            filtered by search/category.
        */

    renderPopularServices();

    /*
            Results sections start hidden.
        */

    hideCategoryResults();

    hideSearchResults();
  } catch (error) {
    console.error("Service loading error:", error);
  }
}

/* Random Popular Services */

function getRandomServices(services) {
  const shuffled = [...services];

  for (let i = shuffled.length - 1; i > 0; i--) {
    const randomIndex = Math.floor(Math.random() * (i + 1));

    [shuffled[i], shuffled[randomIndex]] = [shuffled[randomIndex], shuffled[i]];
  }

  return shuffled.slice(0, 6);
}

/* Create Service Card */

function createServiceCard(service) {
  const card = document.createElement("div");

  card.className =
    "service-card bg-gray-900 " +
    "border border-gray-800 " +
    "rounded-2xl p-6 " +
    "hover:border-violet-500/50 " +
    "transition";

  card.dataset.serviceName = service.serviceName || "";

  card.dataset.categoryId = service.categoryId || "";

  card.innerHTML = `

        <div class="w-12 h-12
                    rounded-xl
                    bg-violet-900/30
                    flex items-center
                    justify-center
                    mb-5">

            <i class="fa-solid
                      ${getServiceIcon(service.serviceName)}
                      text-violet-400">

            </i>

        </div>


        <p class="text-xs
                  text-violet-400
                  mb-2">

            ${escapeHtml(service.categoryName)}

        </p>


        <h4 class="text-lg
                   font-semibold
                   mb-2">

            ${escapeHtml(service.serviceName)}

        </h4>


        <p class="text-sm
                  text-gray-400
                  mb-5">

            Professional service from
            trusted workers.

        </p>


        <button
            class="book-service-button
                   text-violet-400
                   text-sm
                   font-medium
                   hover:text-violet-300
                   transition"
            data-service-id="${service.serviceId}">

            Book Service

            <i class="fa-solid
                      fa-arrow-right
                      ml-2"></i>

        </button>

    `;

  return card;
}

/* Popular Services */

function renderPopularServices() {
  popularServicesContainer.innerHTML = "";

  const services = getRandomServices(customerServices);

  if (services.length === 0) {
    popularServicesContainer.innerHTML = `
            <p class="col-span-full
                      text-center
                      text-gray-500
                      py-8">

                No popular services are
                currently available.

            </p>
        `;

    return;
  }

  services.forEach((service) => {
    popularServicesContainer.appendChild(createServiceCard(service));
  });
}

/*
    CATEGORY RESULTS

    Controlled ONLY by category selection.
*/

function renderCategoryResults(category) {
  categoryServicesContainer.innerHTML = "";

  categoryServicesSection.classList.remove("category-services-enter");

  categoryServicesSection.classList.remove("hidden");

  void categoryServicesSection.offsetWidth;

  categoryServicesSection.classList.add("category-services-enter");

  categoryServicesHeading.textContent = `Services under ${category.categoryName}`;

  categoryServicesDescription.textContent =
    "Choose a service to request a worker.";

  const services = customerServices.filter(
    (service) => String(service.categoryId) === String(category.categoryId),
  );

  if (services.length === 0) {
    categoryServicesContainer.innerHTML = `
            <p class="col-span-full
                      text-center
                      text-gray-500
                      py-8">

                No services are currently available
                in this category.

            </p>
        `;

    return;
  }

  services.forEach((service) => {
    categoryServicesContainer.appendChild(createServiceCard(service));
  });
}

/* Hide Category Results */

function hideCategoryResults() {
  categoryServicesSection.classList.add("hidden");

  categoryServicesContainer.innerHTML = "";
}

/*
    SEARCH RESULTS

    Controlled ONLY by search.
*/

function applyServiceSearch() {
  const value = serviceSearch.value.trim().toLowerCase();

  if (!value) {
    hideSearchResults();

    return;
  }

  const results = customerServices.filter(
    (service) =>
      (service.serviceName || "").toLowerCase().includes(value) ||
      (service.categoryName || "").toLowerCase().includes(value),
  );

  searchResultsContainer.innerHTML = "";

  searchResultsSection.classList.remove("hidden");

  const description = document.getElementById("searchResultsDescription");

  if (description) {
    description.textContent = `${results.length} service${
      results.length === 1 ? "" : "s"
    } found`;
  }

  if (results.length === 0) {
    searchResultsContainer.innerHTML = `
            <p class="col-span-full
                      text-center
                      text-gray-500
                      py-8">

                No services match your search.

            </p>
        `;

    return;
  }

  results.forEach((service) => {
    searchResultsContainer.appendChild(createServiceCard(service));
  });
}

/* Hide Search Results */

function hideSearchResults() {
  searchResultsSection.classList.add("hidden");

  searchResultsContainer.innerHTML = "";
}

if (serviceSearch) {
  serviceSearch.addEventListener("input", applyServiceSearch);
}

/* Active Booking */

function startCompletionCountdown() {
  if (completionTimer) {
    clearInterval(completionTimer);
    completionTimer = null;
  }

  if (
    !activeBooking ||
    activeBooking.status !== "IN_PROGRESS" ||
    !activeBooking.workerConfirmedCompletion ||
    !activeBooking.workerCompletedAt
  ) {
    return;
  }

  const countdownElement = document.getElementById("completion-countdown");

  if (!countdownElement) {
    return;
  }

  function updateCountdown() {
    const completedAt = new Date(activeBooking.workerCompletedAt).getTime();

    const expiryTime = completedAt + 30 * 60 * 1000;

    const remaining = expiryTime - Date.now();

    if (remaining <= 0) {
      countdownElement.textContent = "Confirmation window expired";

      clearInterval(completionTimer);
      completionTimer = null;

      setTimeout(() => {
        loadActiveBooking();
      }, 2000);

      return;
    }

    const minutes = Math.floor(remaining / 60000);

    const seconds = Math.floor((remaining % 60000) / 1000);

    countdownElement.textContent = `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }

  updateCountdown();

  completionTimer = setInterval(updateCountdown, 1000);
}

async function loadActiveBooking() {
  try {
    const bookings = (await apiRequest(`${API_BASE_URL}/bookings`)) || [];

    const active = bookings
      .filter((b) => ["PENDING", "ACCEPTED", "IN_PROGRESS"].includes(b.status))
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    if (active.length > 0) {
      renderActiveBooking(active[0]);
    } else {
      renderActiveBooking(null);
    }
  } catch (error) {
    console.error("Booking loading error:", error);
    renderActiveBooking(null);
  }
}

/* Render Active Booking */

function renderActiveBooking(booking) {
  if (completionTimer) {
    clearInterval(completionTimer);
    completionTimer = null;
  }

  activeBooking = booking;

  if (!booking) {
    activeBookingCard.classList.add("hidden");

    noActiveBooking.classList.remove("hidden");

    return;
  }

  noActiveBooking.classList.add("hidden");

  activeBookingCard.classList.remove("hidden");

  activeBookingService.textContent = booking.serviceName || "Service request";

  activeBookingStatus.textContent = booking.status || "UNKNOWN";

  activeBookingWorker.textContent = booking.workerName || "Worker not assigned";

  switch (booking.status) {
    case "PENDING":
      activeBookingDescription.textContent =
        "Your request is waiting for a worker to accept it.";

      break;

    case "ACCEPTED":
      activeBookingDescription.textContent =
        "A worker has accepted your booking.";

      break;

    case "IN_PROGRESS":
      if (booking.workerConfirmedCompletion) {
        activeBookingDescription.innerHTML = `
                    <span>
                        The worker has marked the service as finished.
                        Please confirm the work.
                    </span>

                    <span class="block mt-3">
                        <span class="block text-sm">
                            Confirmation window remaining:
                        </span>

                        <span id="completion-countdown"
                              class="block text-2xl font-semibold">
                            30:00
                        </span>
                    </span>
                `;
      } else {
        activeBookingDescription.textContent =
          "Your service is currently in progress.";
      }

      break;

    case "NO_WORKER":
      activeBookingDescription.textContent =
        "No worker accepted your request within 30 minutes. You can try booking this service again.";

      break;

    default:
      activeBookingDescription.textContent =
        "Your service request status is currently being processed.";
  }

  const createdAt = new Date(booking.createdAt);

  activeBookingDate.textContent = Number.isNaN(createdAt.getTime())
    ? "-"
    : createdAt.toLocaleDateString();

  activeBookingTime.textContent = Number.isNaN(createdAt.getTime())
    ? "-"
    : createdAt.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      });

  startCompletionCountdown();
}

/* Booking Status Description */

function getBookingStatusDescription(status) {
  switch (status) {
    case "PENDING":
      return "Your request is waiting for a worker to accept it.";

    case "ACCEPTED":
      return "A worker has accepted your booking and is assigned to the service.";

    case "IN_PROGRESS":
      return "Your service is currently in progress.";

    case "COMPLETED":
      return "The service has been completed by both parties.";

    case "AUTO_COMPLETED":
      return "The service was automatically completed because the 30-minute confirmation window expired.";

    case "CANCELLED":
      return "This booking was cancelled.";

    case "NO_WORKER":
      return "No worker accepted your request within 30 minutes.";

    case "FAILED":
      return "This booking could not be completed.";

    default:
      return "Your booking status is currently being processed.";
  }
}

/* Format Booking Date */

function formatBookingDateTime(value) {
  if (!value) {
    return null;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  return date.toLocaleString([], {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

/* Booking Timeline */

function renderBookingTimeline(booking) {
  const timeline = [
    ["Request created", booking.createdAt],

    ["Worker accepted", booking.acceptedAt],

    ["Service started", booking.startedAt],

    ["Service completed", booking.completedAt],

    ["Booking cancelled", booking.cancelledAt],

    ["Booking failed", booking.failedAt],
  ];

  bookingDetailsTimeline.innerHTML = "";

  timeline
    .filter((item) => item[1])
    .forEach((item) => {
      const row = document.createElement("div");

      row.className =
        "flex items-center justify-between gap-4 " +
        "bg-gray-950 border border-gray-800 " +
        "rounded-xl px-4 py-3";

      row.innerHTML = `

                    <span class="text-sm text-gray-300">

                        ${escapeHtml(item[0])}

                    </span>

                    <span class="text-sm text-gray-500 text-right">

                        ${escapeHtml(formatBookingDateTime(item[1]) || "-")}

                    </span>

                `;

      bookingDetailsTimeline.appendChild(row);
    });
}

/* Open Booking Details */

function openBookingDetails(booking = activeBooking) {
  if (!booking) {
    return;
  }

  activeBooking = booking;

  bookingDetailsService.textContent = booking.serviceName || "Service request";

  bookingDetailsStatus.textContent = booking.status || "UNKNOWN";

  bookingDetailsDescription.textContent = getBookingStatusDescription(
    booking.status,
  );

  bookingDetailsWorker.textContent =
    booking.workerName || "Worker not assigned";

  bookingDetailsCreated.textContent =
    formatBookingDateTime(booking.createdAt) || "-";

  renderBookingTimeline(booking);

  if (booking.customerNote) {
    bookingDetailsNote.textContent = booking.customerNote;

    bookingDetailsNoteContainer.classList.remove("hidden");
  } else {
    bookingDetailsNote.textContent = "";

    bookingDetailsNoteContainer.classList.add("hidden");
  }

  /*
        Customer can confirm completion
        only when the booking is IN_PROGRESS
        and the customer has not already confirmed.
    */

  if (
    booking.status === "IN_PROGRESS" &&
    !booking.customerConfirmedCompletion
  ) {
    customerCompletionContainer.classList.remove("hidden");

    confirmCustomerCompletionButton.disabled = false;
  } else {
    customerCompletionContainer.classList.add("hidden");
  }

  /* Customer Booking Action */

  customerCancelContainer.innerHTML = "";

  if (booking.status === "PENDING") {
    customerCancelContainer.innerHTML = `
            <button
                id="cancelCustomerBookingButton"
                type="button"
                class="w-full inline-flex items-center
                       justify-center gap-2
                       px-5 py-3
                       rounded-xl
                       border border-red-500/30
                       bg-red-500/10
                       text-red-400
                       hover:bg-red-500/20
                       transition
                       font-medium"
            >

                <i class="fa-solid fa-xmark"></i>

                Cancel Booking

            </button>
        `;

    customerCancelContainer.classList.remove("hidden");

    document
      .getElementById("cancelCustomerBookingButton")
      .addEventListener("click", cancelCustomerBooking);
  } else if (booking.status === "ACCEPTED") {
    customerCancelContainer.innerHTML = `
            <p class="text-sm text-gray-400 text-center">

                Need to cancel this booking?

                <a
                    href="ContactFaq.html"
                    class="text-violet-400
                           hover:text-violet-300
                           transition"
                >
                    Contact Support
                </a>

            </p>
        `;

    customerCancelContainer.classList.remove("hidden");
  } else {
    customerCancelContainer.classList.add("hidden");
  }

  bookingDetailsModal.classList.remove("hidden");

  document.body.classList.add("overflow-hidden");
}

/* Close Booking Details */

function closeBookingDetails() {
  bookingDetailsModal.classList.add("hidden");

  document.body.classList.remove("overflow-hidden");
}

/* Open Create Booking */

function openCreateBooking(serviceId) {
  selectedBookingService = customerServices.find(
    (service) => String(service.serviceId) === String(serviceId),
  );

  if (!selectedBookingService) {
    return;
  }

  createBookingService.textContent = selectedBookingService.serviceName;

  customerBookingNote.value = "";

  createBookingModal.classList.remove("hidden");

  document.body.classList.add("overflow-hidden");
}

/* Close Create Booking */

function closeCreateBooking() {
  createBookingModal.classList.add("hidden");

  document.body.classList.remove("overflow-hidden");

  selectedBookingService = null;
}

// get the current location

function getCurrentLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("Location services are not supported by this browser."));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        });
      },
      (error) => {
        switch (error.code) {
          case error.PERMISSION_DENIED:
            reject(new Error("Location permission is required to book a service."));
            break;

          case error.POSITION_UNAVAILABLE:
            reject(new Error("Unable to determine your current location."));
            break;

          case error.TIMEOUT:
            reject(new Error("Location request timed out. Please try again."));
            break;

          default:
            reject(new Error("Unable to get your current location."));
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    );
  });
}

/* Submit Booking */

async function submitBooking() {
  if (!selectedBookingService) return;

  confirmCreateBookingButton.disabled = true;
  confirmCreateBookingButton.textContent = "Getting location...";

  try {
    const location = await getCurrentLocation();

    confirmCreateBookingButton.textContent = "Requesting...";

    await apiRequest(`${API_BASE_URL}/bookings`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        serviceId: selectedBookingService.serviceId,
        customerNote: customerBookingNote.value.trim() || null,
        latitude: location.latitude,
        longitude: location.longitude
      })
    });

    closeCreateBooking();
    await loadActiveBooking();

    alert("Service request created successfully.");

  } catch (error) {
    console.error("Booking creation error:", error);
    alert(error.message);

  } finally {
    confirmCreateBookingButton.disabled = false;
    confirmCreateBookingButton.textContent = "Request Service";
  }
}

/*
    CUSTOMER COMPLETION

    This is the important final step.

    Customer confirms that the work is
    actually finished.

    Backend will only mark COMPLETED
    when the worker has also confirmed.
*/

async function confirmCustomerCompletion() {
  if (!activeBooking || activeBooking.status !== "IN_PROGRESS") {
    return;
  }

  if (!activeBooking.workerConfirmedCompletion) {
    alert("The worker has not marked the service as complete yet.");

    return;
  }

  const confirmed = window.confirm(
    "Confirm that the service work has actually been completed?",
  );

  if (!confirmed) {
    return;
  }

  confirmCustomerCompletionButton.disabled = true;

  confirmCustomerCompletionButton.textContent = "Confirming...";

  try {
    const updatedBooking = await apiRequest(
      `${API_BASE_URL}/bookings/${activeBooking.bookingId}/complete`,
      {
        method: "POST",
      },
    );

    activeBooking = updatedBooking;

    closeBookingDetails();

    await loadActiveBooking();

    openBookingDetails(updatedBooking);
  } catch (error) {
    console.error("Customer completion error:", error);

    alert(error.message);
  } finally {
    confirmCustomerCompletionButton.disabled = false;

    confirmCustomerCompletionButton.innerHTML = `
            <i class="fa-solid fa-check"></i>
            Confirm Work Completed
        `;
  }
}

async function cancelCustomerBooking() {
  if (!activeBooking || activeBooking.status !== "PENDING") {
    return;
  }

  const confirmed = window.confirm(
    "Are you sure you want to cancel this booking?",
  );

  if (!confirmed) {
    return;
  }

  const button = document.getElementById("cancelCustomerBookingButton");

  if (button) {
    button.disabled = true;

    button.textContent = "Cancelling...";
  }

  try {
    const updatedBooking = await apiRequest(
      `${API_BASE_URL}/bookings/${activeBooking.bookingId}/cancel`,
      {
        method: "POST",
      },
    );

    activeBooking = updatedBooking;

    closeBookingDetails();

    await loadActiveBooking();

    openBookingDetails(updatedBooking);
  } catch (error) {
    console.error("Booking cancellation error:", error);

    alert(error.message);

    if (button) {
      button.disabled = false;

      button.innerHTML = `
                <i class="fa-solid fa-xmark"></i>
                Cancel Booking
            `;
    }
  }
}

/* Event Delegation */

document.addEventListener("click", (event) => {
  const bookButton = event.target.closest(".book-service-button");

  if (!bookButton) {
    return;
  }

  openCreateBooking(bookButton.dataset.serviceId);
});

/* Buttons */

if (viewActiveBookingButton) {
  viewActiveBookingButton.addEventListener("click", () => openBookingDetails());
}

if (closeBookingDetailsButton) {
  closeBookingDetailsButton.addEventListener("click", closeBookingDetails);
}

if (closeCreateBookingButton) {
  closeCreateBookingButton.addEventListener("click", closeCreateBooking);
}

if (cancelCreateBookingButton) {
  cancelCreateBookingButton.addEventListener("click", closeCreateBooking);
}

if (confirmCreateBookingButton) {
  confirmCreateBookingButton.addEventListener("click", submitBooking);
}

if (confirmCustomerCompletionButton) {
  confirmCustomerCompletionButton.addEventListener(
    "click",
    confirmCustomerCompletion,
  );
}

/* Close Booking Details Outside */

if (bookingDetailsModal) {
  bookingDetailsModal.addEventListener("click", (event) => {
    if (event.target === bookingDetailsModal) {
      closeBookingDetails();
    }
  });
}

/* Close Create Booking Outside */

if (createBookingModal) {
  createBookingModal.addEventListener("click", (event) => {
    if (event.target === createBookingModal) {
      closeCreateBooking();
    }
  });
}

/* Escape */

document.addEventListener("keydown", (event) => {
  if (event.key !== "Escape") {
    return;
  }

  closeBookingDetails();

  closeCreateBooking();
});

/* Authentication Error */

function handleUnauthorized() {
  [
    "servicehub_access_token",
    "servicehub_user_id",
    "servicehub_user_name",
    "servicehub_user_email",
    "servicehub_user_role",
  ].forEach((key) => localStorage.removeItem(key));

  window.location.href = "login.html";
}

/* Initialize Dashboard */

async function initializeDashboard() {
  await loadCustomerName();

  await Promise.all([loadCustomerCategories(), loadCustomerServices()]);

  await loadActiveBooking();
}

window.addEventListener("beforeunload", () => {
  if (completionTimer) {
    clearInterval(completionTimer);
  }
});

initializeDashboard();
