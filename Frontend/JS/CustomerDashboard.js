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

const bookingPartsContainer = document.getElementById("bookingPartsContainer");

const customerPartsList = document.getElementById("customerPartsList");

const bookingDetailsNoteContainer = document.getElementById(
  "bookingDetailsNoteContainer",
);

const bookingDetailsNote = document.getElementById("bookingDetailsNote");

const bookingOtpContainer = document.getElementById("bookingOtpContainer");

/* Dashboard Data */

let customerServices = [];

let selectedCategoryId = null;

let selectedBookingService = null;

let activeBooking = null;

let bookingRefreshTimer = null;

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

async function loadActiveBooking() {
  try {
    const bookings = (await apiRequest(`${API_BASE_URL}/bookings`)) || [];

    const active = bookings
      .filter((b) => ["PENDING", "ACCEPTED", "IN_PROGRESS"].includes(b.status))
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    if (active.length > 0) {
      startActiveBookingPolling();
    } else {
      stopActiveBookingPolling();
    }

    renderActiveBookings(active);

  } catch (error) {
    console.error("Booking loading error:", error);
    renderActiveBookings([]);
  }
}

//ploong timer

function startActiveBookingPolling() {
  if (bookingRefreshTimer) {
    return;
  }

  bookingRefreshTimer = setInterval(async () => {
    try {
      await loadActiveBooking();
    } catch (error) {
      console.error("Active booking refresh failed:", error);
    }
  }, 3000);
}

function stopActiveBookingPolling() {
  if (bookingRefreshTimer) {
    clearInterval(bookingRefreshTimer);
    bookingRefreshTimer = null;
  }
}

/* Render Active Booking */

function renderActiveBookings(bookings) {
  activeBookingsContainer.innerHTML = "";

  if (!bookings || bookings.length === 0) {
    activeBookingsContainer.classList.add("hidden");
    noActiveBooking.classList.remove("hidden");
    activeBooking = null;
    return;
  }

  noActiveBooking.classList.add("hidden");
  activeBookingsContainer.classList.remove("hidden");

  bookings.forEach((booking) => {
    const card = document.createElement("div");

    card.className =
      "bg-gray-900 border border-gray-800 rounded-2xl p-6 " +
      "hover:border-violet-500/50 transition";

    const description =
      booking.status === "PENDING"
        ? "Your request is waiting for a worker to accept it."
        : booking.status === "ACCEPTED"
          ? "A worker has accepted your booking."
          : "Your service is currently in progress.";

    const statusClasses =
      booking.status === "IN_PROGRESS"
        ? "bg-blue-900/30 text-blue-400 border border-blue-700/30"
        : booking.status === "ACCEPTED"
          ? "bg-green-900/30 text-green-400 border border-green-700/30"
          : "bg-yellow-900/30 text-yellow-400 border border-yellow-700/30";

    const createdAt = new Date(booking.createdAt);

    const formattedDate = Number.isNaN(createdAt.getTime())
      ? "-"
      : createdAt.toLocaleDateString();

    const formattedTime = Number.isNaN(createdAt.getTime())
      ? "-"
      : createdAt.toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        });

    const otpSection =
      booking.status === "ACCEPTED" && booking.startOtp
        ? `
          <div class="mt-5 p-4 rounded-xl bg-violet-950/20 border border-violet-700/30">

            <div class="flex items-center gap-3 mb-3">

              <div
                class="w-9 h-9 rounded-full
                       bg-violet-500/10
                       flex items-center justify-center"
              >
                <i class="fa-solid fa-key text-violet-400"></i>
              </div>

              <div>
                <h4 class="text-white font-semibold text-sm">
                  Start Work OTP
                </h4>

                <p class="text-xs text-gray-400">
                  Give this OTP to the worker when they are ready to start.
                </p>
              </div>

            </div>

            <div class="text-center py-2">

              <p
                class="text-3xl font-bold
                       tracking-[0.35em]
                       text-violet-300"
              >
                ${escapeHtml(booking.startOtp)}
              </p>

            </div>

            <div
              class="mt-3 p-3 rounded-lg
                     bg-red-950/20
                     border border-red-700/30"
            >
              <p class="text-xs text-red-300 leading-relaxed">

                <i class="fa-solid fa-triangle-exclamation mr-1"></i>

                Never share this OTP over a phone call, text message, or with
                anyone other than the worker at your location.

              </p>
            </div>

          </div>
        `
        : booking.status === "IN_PROGRESS" &&
            booking.completionRequested &&
            booking.completionOtp
          ? `
            <div class="mt-5 p-4 rounded-xl bg-violet-950/20 border border-violet-700/30">

              <div class="flex items-center gap-3 mb-3">

                <div
                  class="w-9 h-9 rounded-full
                         bg-violet-500/10
                         flex items-center justify-center"
                >
                  <i class="fa-solid fa-lock text-violet-400"></i>
                </div>

                <div>
                  <h4 class="text-white font-semibold text-sm">
                    Completion OTP
                  </h4>

                  <p class="text-xs text-gray-400">
                    Give this OTP only after the service is fully complete.
                  </p>
                </div>

              </div>

              <div class="text-center py-2">

                <p
                  class="text-3xl font-bold
                         tracking-[0.35em]
                         text-violet-300"
                >
                  ${escapeHtml(booking.completionOtp)}
                </p>

              </div>

              <div
                class="mt-3 p-3 rounded-lg
                       bg-red-950/20
                       border border-red-700/30"
              >
                <p class="text-xs text-red-300 leading-relaxed">

                  <i class="fa-solid fa-triangle-exclamation mr-1"></i>

                  Do not give this OTP until the service has been completely
                  finished and you have checked the work. Never share it over
                  a phone call or message.

                </p>
              </div>

            </div>
          `
          : "";

    card.innerHTML = `
      <div
        class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6"
      >

        <div class="flex items-start gap-5">

          <div
            class="w-14 h-14 rounded-xl
                   bg-violet-900/30
                   border border-violet-700/40
                   flex items-center justify-center
                   shrink-0"
          >
            <i class="fa-solid fa-wrench text-violet-400 text-xl"></i>
          </div>

          <div class="w-full">

            <div class="flex flex-wrap items-center gap-3 mb-2">

              <h4 class="text-lg font-semibold">
                ${escapeHtml(booking.serviceName || "Service request")}
              </h4>

              <span
                class="px-3 py-1 rounded-full text-xs font-medium ${statusClasses}"
              >
                ${escapeHtml(booking.status || "UNKNOWN")}
              </span>

            </div>

            <p class="text-sm text-gray-400 mb-3">
              ${escapeHtml(description)}
            </p>

            <div
              class="flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-400"
            >

              <span>
                <i class="fa-solid fa-user mr-2 text-violet-400"></i>
                ${escapeHtml(booking.workerName || "Worker not assigned")}
              </span>

              <span>
                <i class="fa-solid fa-calendar-days mr-2 text-violet-400"></i>
                ${escapeHtml(formattedDate)}
              </span>

              <span>
                <i class="fa-solid fa-clock mr-2 text-violet-400"></i>
                ${escapeHtml(formattedTime)}
              </span>

            </div>

            ${otpSection}

          </div>

        </div>

        <div class="lg:text-right shrink-0">

          <button
            type="button"
            class="view-active-booking-button
                   inline-flex items-center
                   justify-center gap-2
                   px-5 py-3
                   rounded-xl
                   bg-violet-600
                   text-white
                   hover:bg-violet-700
                   transition
                   font-medium"
            data-booking-id="${booking.bookingId}"
          >
            <span>View Details</span>
            <i class="fa-solid fa-arrow-right"></i>
          </button>

        </div>

      </div>
    `;

    activeBookingsContainer.appendChild(card);
  });

  // Attach View Details handlers

  activeBookingsContainer
    .querySelectorAll(".view-active-booking-button")
    .forEach((button) => {
      button.addEventListener("click", () => {
        const booking = bookings.find(
          (item) => String(item.bookingId) === String(button.dataset.bookingId),
        );

        if (booking) {
          activeBooking = booking;
          openBookingDetails(booking);
        }
      });
    });

  // Keep the first active booking as the current booking.
  activeBooking = bookings[0];
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

/* Load Customer Part Photo */

async function loadCustomerPartPhoto(partId) {
  const imageElement = document.getElementById(`customer-part-photo-${partId}`);

  if (!imageElement) {
    console.error("Part image element not found:", partId);
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE_URL}/bookings/parts/${partId}/photo`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    if (!response.ok) {
      console.error(
        "Part photo request failed:",
        response.status,
        response.statusText,
      );
      return;
    }

    const blob = await response.blob();

    if (!blob.type.startsWith("image/")) {
      console.error("Part photo response is not an image:", blob.type);
      return;
    }

    const imageUrl = URL.createObjectURL(blob);

    imageElement.src = imageUrl;

    imageElement.onclick = () => {
      openCustomerPartImageViewer(partId);
    };
  } catch (error) {
    console.error("Failed to load customer part photo:", error);
  }
}

/* Customer Part Image Viewer */

async function openCustomerPartImageViewer(partId) {
  try {
    const response = await fetch(
      `${API_BASE_URL}/bookings/parts/${partId}/photo`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    if (!response.ok) {
      console.error(
        "Viewer photo request failed:",
        response.status,
        response.statusText,
      );

      return;
    }

    const blob = await response.blob();

    if (!blob.type.startsWith("image/")) {
      console.error("Viewer response is not an image:", blob.type);

      return;
    }

    const imageUrl = URL.createObjectURL(blob);

    const oldViewer = document.getElementById("customerPartImageViewer");

    if (oldViewer) {
      oldViewer.remove();
    }

    const viewer = document.createElement("div");

    viewer.id = "customerPartImageViewer";

    viewer.className =
      "fixed inset-0 z-[200] " +
      "flex items-center justify-center " +
      "bg-black/80 backdrop-blur-sm " +
      "px-4 py-8 " +
      "opacity-0 transition-opacity duration-200";

    viewer.innerHTML = `
      <div
        id="customerPartImageCard"
        class="
          relative
          max-w-[78vw]
          max-h-[80vh]
          bg-gray-900
          border border-gray-700
          rounded-2xl
          shadow-2xl
          p-3
          scale-95
          opacity-0
          transition-all
          duration-200
        "
      >

        <button
          type="button"
          id="closeCustomerPartImageViewer"
          class="
            absolute
            top-3 right-3
            w-10 h-10
            rounded-full
            bg-gray-800/90
            border border-gray-700
            text-gray-300
            hover:text-white
            hover:bg-gray-700
            transition
            z-10
          "
        >
          <i class="fa-solid fa-xmark"></i>
        </button>

        <img
          src="${imageUrl}"
          alt="Part photo"
          class="
            block
            max-w-[74vw]
            max-h-[72vh]
            object-contain
            rounded-xl
          "
        >

      </div>
    `;

    document.body.appendChild(viewer);

    const imageCard = document.getElementById("customerPartImageCard");

    const closeViewer = () => {
      imageCard.classList.remove("scale-100", "opacity-100");

      imageCard.classList.add("scale-95", "opacity-0");

      viewer.classList.remove("opacity-100");

      viewer.classList.add("opacity-0");

      setTimeout(() => {
        URL.revokeObjectURL(imageUrl);
        viewer.remove();
      }, 200);
    };

    document
      .getElementById("closeCustomerPartImageViewer")
      .addEventListener("click", closeViewer);

    viewer.addEventListener("click", (event) => {
      if (event.target === viewer) {
        closeViewer();
      }
    });

    const handleEscape = (event) => {
      if (event.key === "Escape") {
        closeViewer();

        document.removeEventListener("keydown", handleEscape);
      }
    };

    document.addEventListener("keydown", handleEscape);

    requestAnimationFrame(() => {
      viewer.classList.add("opacity-100");

      imageCard.classList.remove("scale-95", "opacity-0");

      imageCard.classList.add("scale-100", "opacity-100");
    });
  } catch (error) {
    console.error("Failed to open customer part photo:", error);
  }
}

/* Approve Customer Part */

async function approveCustomerPart(bookingPartId) {
  const button = document.querySelector(
    `.customer-approve-part-button[data-part-id="${bookingPartId}"]`,
  );

  if (button) {
    button.disabled = true;

    button.innerHTML = `
      <i class="fa-solid fa-spinner fa-spin"></i>
      Approving...
    `;
  }

  try {
    await apiRequest(
      `${API_BASE_URL}/bookings/parts/${bookingPartId}/approve`,
      {
        method: "POST",
      },
    );

    await loadCustomerBookingParts(activeBooking.bookingId);
  } catch (error) {
    console.error("Failed to approve part:", error);

    alert(error.message || "Unable to approve part.");

    if (button) {
      button.disabled = false;

      button.innerHTML = `
        <i class="fa-solid fa-check"></i>
        Approve Part
      `;
    }
  }
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

  loadCustomerBookingParts(booking.bookingId);

  if (booking.customerNote) {
    bookingDetailsNote.textContent = booking.customerNote;

    bookingDetailsNoteContainer.classList.remove("hidden");
  } else {
    bookingDetailsNote.textContent = "";

    bookingDetailsNoteContainer.classList.add("hidden");
  }

  if (bookingOtpContainer) {
    bookingOtpContainer.classList.add("hidden");
    bookingOtpContainer.innerHTML = "";

    // Start OTP
    if (booking.status === "ACCEPTED" && booking.startOtp) {
      bookingOtpContainer.classList.remove("hidden");

      bookingOtpContainer.innerHTML = `
      <div class="flex items-center gap-3 mb-3">
        <div class="w-10 h-10 rounded-full bg-violet-500/10 flex items-center justify-center">
          <i class="fa-solid fa-key text-violet-400"></i>
        </div>

        <div>
          <h4 class="text-white font-semibold">Start Work OTP</h4>
          <p class="text-xs text-gray-400">
            Give this OTP to the worker when they are ready to start.
          </p>
        </div>
      </div>

      <div class="text-center py-3">
        <p class="text-3xl font-bold tracking-[0.35em] text-violet-300">
          ${booking.startOtp}
        </p>
      </div>

      <div class="mt-3 p-3 rounded-lg bg-red-950/20 border border-red-700/30">
        <p class="text-xs text-red-300 leading-relaxed">
          <i class="fa-solid fa-triangle-exclamation mr-1"></i>
          Never share this OTP over a phone call, text message, or with anyone
          other than the worker at your location.
        </p>
      </div>
    `;
    }

    // Completion OTP
    if (
      booking.status === "IN_PROGRESS" &&
      booking.completionRequested &&
      booking.completionOtp
    ) {
      bookingOtpContainer.classList.remove("hidden");

      bookingOtpContainer.innerHTML = `
      <div class="flex items-center gap-3 mb-3">
        <div class="w-10 h-10 rounded-full bg-violet-500/10 flex items-center justify-center">
          <i class="fa-solid fa-lock text-violet-400"></i>
        </div>

        <div>
          <h4 class="text-white font-semibold">Completion OTP</h4>
          <p class="text-xs text-gray-400">
            Give this OTP to the worker only after the service is fully complete.
          </p>
        </div>
      </div>

      <div class="text-center py-3">
        <p class="text-3xl font-bold tracking-[0.35em] text-violet-300">
          ${booking.completionOtp}
        </p>
      </div>

      <div class="mt-3 p-3 rounded-lg bg-red-950/20 border border-red-700/30">
        <p class="text-xs text-red-300 leading-relaxed">
          <i class="fa-solid fa-triangle-exclamation mr-1"></i>
          Do not give this OTP until the service has been completely finished
          and you have checked the work. Never share it over a phone call or
          message.
        </p>
      </div>
    `;
    }
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
          longitude: position.coords.longitude,
        });
      },
      (error) => {
        switch (error.code) {
          case error.PERMISSION_DENIED:
            reject(
              new Error("Location permission is required to book a service."),
            );
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
        maximumAge: 0,
      },
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
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        serviceId: selectedBookingService.serviceId,
        customerNote: customerBookingNote.value.trim() || null,
        latitude: location.latitude,
        longitude: location.longitude,
      }),
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

// parts

async function loadCustomerBookingParts(bookingId) {
  const container = document.getElementById("bookingPartsContainer");
  const list = document.getElementById("customerPartsList");

  if (!container || !list) return;

  try {
    list.innerHTML = `
      <div class="text-sm text-gray-400">
        Loading parts...
      </div>
    `;

    const parts = await apiRequest(
      `${API_BASE_URL}/bookings/${bookingId}/parts`,
    );

    if (!parts || parts.length === 0) {
      container.classList.add("hidden");
      return;
    }

    container.classList.remove("hidden");
    renderCustomerBookingParts(parts);
  } catch (error) {
    console.error("Failed to load booking parts:", error);

    container.classList.remove("hidden");

    list.innerHTML = `
      <div class="text-sm text-red-400">
        Unable to load parts.
      </div>
    `;
  }
}

function renderCustomerBookingParts(parts) {
  const list = document.getElementById("customerPartsList");

  if (!list) return;

  list.innerHTML = parts.map((part) => {
    const isPending = part.status === "PENDING";

    return `
      <div
        class="bg-gray-900/60 border border-gray-800 rounded-xl p-4"
        data-booking-part-id="${part.bookingPartId}"
      >
        <div class="flex gap-4">

          <!-- Part Photo -->
          <div class="w-20 h-20 flex-shrink-0">
            <img
              id="customer-part-photo-${part.bookingPartId}"
              src=""
              alt="${escapeHtml(part.partName)}"
              class="w-full h-full object-cover rounded-lg border border-gray-700"
            >
          </div>

          <!-- Part Details -->
          <div class="flex-1 min-w-0">

            <div class="flex items-start justify-between gap-3">
              <h4 class="font-semibold text-white">
                ${escapeHtml(part.partName)}
              </h4>

              <span class="text-xs px-2 py-1 rounded-full ${
                isPending
                  ? "bg-yellow-500/10 text-yellow-400 border border-yellow-500/20"
                  : "bg-green-500/10 text-green-400 border border-green-500/20"
              }">
                ${isPending ? "Pending Approval" : "Approved"}
              </span>
            </div>

            <div class="mt-2 text-sm text-gray-400 space-y-1">
              <p>
                Unit Price:
                <span class="text-gray-200">
                  ₹${Number(part.unitPrice).toFixed(2)}
                </span>
              </p>

              <p>
                Quantity:
                <span class="text-gray-200">
                  ${part.quantity}
                </span>
              </p>

              <p>
                Total:
                <span class="text-white font-medium">
                  ₹${Number(part.totalPrice).toFixed(2)}
                </span>
              </p>
            </div>

            ${
              isPending
                ? `
                  <button
                    type="button"
                    class="approve-booking-part-btn mt-3 inline-flex items-center px-4 py-2 rounded-lg bg-violet-600 hover:bg-violet-700 text-white text-sm font-medium transition"
                    data-booking-part-id="${part.bookingPartId}"
                  >
                    Approve Part
                  </button>
                `
                : `
                  <div class="mt-3 text-sm text-green-400">
                    ✓ Part approved
                  </div>
                `
            }

          </div>
        </div>
      </div>
    `;
  });

  parts.forEach((part) => {
    loadCustomerPartPhoto(part.bookingPartId);
  });
}

/* Event Delegation */

document.addEventListener("click", (event) => {
  const bookButton = event.target.closest(".book-service-button");

  if (bookButton) {
    openCreateBooking(bookButton.dataset.serviceId);

    return;
  }

  const approvePartButton = event.target.closest(".approve-booking-part-btn");

  if (approvePartButton) {
    approveCustomerPart(approvePartButton.dataset.bookingPartId);
    return;
  }
});

/* Buttons */

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
  stopActiveBookingPolling();
});

initializeDashboard();
