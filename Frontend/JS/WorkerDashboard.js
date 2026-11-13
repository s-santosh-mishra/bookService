const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";
const USER_NAME_KEY = "servicehub_user_name";

const API_ROOT = window.SERVICEHUB_CONFIG ? window.SERVICEHUB_CONFIG.API_BASE_URL : "http://localhost:8080";
const API_BASE_URL = `${API_ROOT}/api/worker`;
let workerBookingRefreshTimer = null;
let lastWorkerBookingsSnapshot = null;
let currentWorkerStatus = null;
let isAvailabilityUpdating = false;
let workerLocationWatchId = null;
let workerLocationUpdateTimer = null;
let workerLocationStartAttempted = false;
let workerLocationUpdateInProgress = false;
let workerLocationAcquisitionInProgress = false;
let lastWorkerLocation = null;
let lastWorkerLocationUpdateAt = 0;
const WORKER_LOCATION_UPDATE_INTERVAL_MS = 90_000;
const WORKER_LOCATION_MOVEMENT_THRESHOLD_METERS = 50;
const WORKER_LOCATION_MIN_UPDATE_INTERVAL_MS = 15_000;
const razorpayConfirmationCountdowns = new Map();
const dismissedRazorpayBookingIds = new Set();

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

// Worker Information

async function loadWorkerInformation() {
  try {
    const profile = await workerApiRequest(`${API_BASE_URL}/profile`);

    const workerName = profile?.fullName || "Worker";

    localStorage.setItem(USER_NAME_KEY, workerName);

    const welcomeMessage = document.getElementById("welcomeMessage");

    if (welcomeMessage) {
      welcomeMessage.textContent = `Welcome back, ${workerName}!`;
    }
  } catch (error) {
    console.error("Failed to load worker profile:", error);

    const workerName = localStorage.getItem(USER_NAME_KEY) || "Worker";

    const welcomeMessage = document.getElementById("welcomeMessage");

    if (welcomeMessage) {
      welcomeMessage.textContent = `Welcome back, ${workerName}!`;
    }
  }
}

// Worker Status

async function loadWorkerStatus() {
  try {
    const status = await workerApiRequest(`${API_BASE_URL}/status`);

    renderWorkerStatus(status);
    if (status?.availabilityStatus === "AVAILABLE") {
      if (!isAvailabilityUpdating) {
        ensureWorkerLocationTracking();
      }
    } else {
      stopWorkerLocationTracking();
    }
  } catch (error) {
    console.error("Failed to load worker status:", error);
    currentWorkerStatus = null;
    stopWorkerLocationTracking();
    updateAvailabilityToggle();

    const statusElement = document.getElementById("availabilityStatus");

    const badge = document.getElementById("availabilityBadge");

    if (statusElement) {
      statusElement.textContent = "Unable to load";
    }

    if (badge) {
      badge.textContent = "Error";

      badge.className =
        "px-3 py-1 rounded-full " +
        "text-xs font-medium " +
        "bg-red-900/30 " +
        "text-red-400 " +
        "border border-red-700/30";
    }
  }
}

function renderWorkerStatus(status) {
  currentWorkerStatus = status;
  updateAvailabilityToggle();

  const verificationStatusElement =
    document.getElementById("verificationStatus");

  const verificationBadge = document.getElementById("verificationBadge");

  const statusElement = document.getElementById("availabilityStatus");

  const badge = document.getElementById("availabilityBadge");

  const iconContainer = document.getElementById("availabilityIcon");

  if (!statusElement || !badge || !iconContainer) {
    return;
  }

  if (status.verificationStatus) {
    const verificationLabel = status.verificationStatus
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/^\w/, (character) => character.toUpperCase());

    if (verificationStatusElement) {
      verificationStatusElement.textContent = verificationLabel;
    }

    if (verificationBadge) {
      verificationBadge.textContent = verificationLabel;
    }
  }

  /*
   * Availability only applies to verified workers.
   * A worker who is not verified cannot be considered available.
   */

  if (status.verificationStatus !== "VERIFIED") {
    statusElement.textContent = "Not Available";

    badge.textContent = "Not Available";

    badge.className =
      "px-3 py-1 rounded-full " +
      "text-xs font-medium " +
      "bg-gray-800 " +
      "text-gray-400 " +
      "border border-gray-700";

    iconContainer.className =
      "w-12 h-12 rounded-xl " +
      "bg-gray-800 " +
      "border border-gray-700 " +
      "flex items-center justify-center";

    iconContainer.innerHTML = `
            <i class="fa-solid fa-lock
                      text-gray-400 text-lg"></i>
        `;

    return;
  }

  const availability = status.availabilityStatus;

  // AVAILABLE

  if (availability === "AVAILABLE") {
    statusElement.textContent = "Available";

    badge.textContent = "Available";

    badge.className =
      "px-3 py-1 rounded-full " +
      "text-xs font-medium " +
      "bg-green-900/30 " +
      "text-green-400 " +
      "border border-green-700/30";

    iconContainer.className =
      "w-12 h-12 rounded-xl " +
      "bg-green-900/30 " +
      "border border-green-700/40 " +
      "flex items-center justify-center";

    iconContainer.innerHTML = `
            <i class="fa-solid fa-circle-check
                      text-green-400 text-lg"></i>
        `;

    return;
  }

  // UNAVAILABLE

  if (availability === "UNAVAILABLE") {
    statusElement.textContent = "Unavailable";

    badge.textContent = "Unavailable";

    badge.className =
      "px-3 py-1 rounded-full " +
      "text-xs font-medium " +
      "bg-gray-800 " +
      "text-gray-400 " +
      "border border-gray-700";

    iconContainer.className =
      "w-12 h-12 rounded-xl " +
      "bg-gray-800 " +
      "border border-gray-700 " +
      "flex items-center justify-center";

    iconContainer.innerHTML = `
            <i class="fa-solid fa-circle-xmark
                      text-gray-400 text-lg"></i>
        `;

    return;
  }

  // BUSY

  if (availability === "BUSY") {
    statusElement.textContent = "Busy";

    badge.textContent = "Busy";

    badge.className =
      "px-3 py-1 rounded-full " +
      "text-xs font-medium " +
      "bg-yellow-900/30 " +
      "text-yellow-400 " +
      "border border-yellow-700/30";

    iconContainer.className =
      "w-12 h-12 rounded-xl " +
      "bg-yellow-900/30 " +
      "border border-yellow-700/40 " +
      "flex items-center justify-center";

    iconContainer.innerHTML = `
            <i class="fa-solid fa-briefcase
                      text-yellow-400 text-lg"></i>
        `;

    return;
  }

  // Unknown status

  statusElement.textContent = availability || "Unknown";

  badge.textContent = "Unknown";
}

function updateAvailabilityToggle() {
  const button = document.getElementById("availabilityToggleButton");
  const label = document.getElementById("availabilityToggleLabel");

  if (!button || !label) {
    return;
  }

  const availability = currentWorkerStatus?.availabilityStatus;
  const isVerified = currentWorkerStatus?.verificationStatus === "VERIFIED";
  const isBusy = availability === "BUSY";
  const canChange = isVerified && ["AVAILABLE", "UNAVAILABLE"].includes(availability);

  button.disabled = isAvailabilityUpdating || !canChange;
  label.textContent = isAvailabilityUpdating
    ? "Updating..."
    : isBusy
      ? "Busy"
      : !currentWorkerStatus
        ? "Status unavailable"
        : !isVerified
          ? "Verification required"
          : availability === "AVAILABLE"
            ? "Go offline"
            : "Go online";

  const icon = button.querySelector("i");
  if (icon) {
    icon.className = isAvailabilityUpdating
      ? "fa-solid fa-spinner fa-spin"
      : availability === "AVAILABLE"
        ? "fa-solid fa-toggle-on"
        : "fa-solid fa-toggle-off";
  }
}

function showAvailabilityFeedback(message, isError = false) {
  const feedback = document.getElementById("availabilityFeedback");

  if (!feedback) {
    return;
  }

  feedback.textContent = message;
  feedback.classList.toggle("text-red-400", isError);
  feedback.classList.toggle("text-green-400", !isError && Boolean(message));
  feedback.classList.toggle("text-gray-400", !message);
}

async function toggleWorkerAvailability() {
  if (isAvailabilityUpdating || !currentWorkerStatus) {
    return;
  }

  const isOnline = currentWorkerStatus.availabilityStatus === "AVAILABLE";
  const endpoint = isOnline ? "offline" : "online";
  isAvailabilityUpdating = true;
  updateAvailabilityToggle();

  try {
    if (isOnline) {
      showAvailabilityFeedback("Going offline...");
      const updatedStatus = await workerApiRequest(
        `${API_BASE_URL}/status/${endpoint}`,
        { method: "POST" },
      );
      renderWorkerStatus(updatedStatus);
      stopWorkerLocationTracking();
      showAvailabilityFeedback("You are now offline.");
    } else {
      showAvailabilityFeedback("Getting your current location...");
      const position = await getCurrentBrowserPosition();

      showAvailabilityFeedback("Going online and verifying your location...");
      const updatedStatus = await workerApiRequest(
        `${API_BASE_URL}/status/${endpoint}`,
        { method: "POST" },
      );

      if (!updatedStatus || updatedStatus.availabilityStatus !== "AVAILABLE") {
        throw new Error("Unable to change availability to online.");
      }

      renderWorkerStatus(updatedStatus);
      try {
        await updateWorkerLocation(position.coords);
      } catch (locationError) {
        try {
          const offlineStatus = await workerApiRequest(
            `${API_BASE_URL}/status/offline`,
            { method: "POST" },
          );
          renderWorkerStatus(offlineStatus);
          throw locationError;
        } catch (rollbackError) {
          if (rollbackError === locationError) {
            throw rollbackError;
          }

          console.error("Failed to roll back worker availability:", rollbackError);
          await loadWorkerStatus();
          throw new Error(
            `${locationError.message} Your online status could not be rolled back; check your availability and location before continuing.`,
          );
        }
      }

      workerLocationStartAttempted = false;
      startWorkerLocationTracking(position.coords);
      showAvailabilityFeedback(
        "You are online and your location was updated.",
      );
    }

    await loadBookingRequests();
  } catch (error) {
    console.error("Failed to update worker availability:", error);
    showAvailabilityFeedback(
      error.message || "Unable to update availability.",
      true,
    );
  } finally {
    isAvailabilityUpdating = false;
    updateAvailabilityToggle();
    if (currentWorkerStatus?.availabilityStatus === "AVAILABLE"
        && workerLocationWatchId === null
        && workerLocationUpdateTimer === null) {
      ensureWorkerLocationTracking();
    }
  }
}

function getCurrentBrowserPosition() {
  if (!navigator.geolocation) {
    return Promise.reject(
      new Error("This browser does not support location access."),
    );
  }

  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      resolve,
      (error) => {
        if (error.code === error.PERMISSION_DENIED) {
          reject(
            new Error("Location permission was denied. Allow location access to go online."),
          );
        } else if (error.code === error.POSITION_UNAVAILABLE) {
          reject(new Error("Your current location is unavailable."));
        } else {
          reject(new Error("Location request timed out. Please try again."));
        }
      },
      {
        enableHighAccuracy: true,
        maximumAge: 0,
        timeout: 15_000,
      },
    );
  });
}

async function updateWorkerLocation(coords) {
  const updatedStatus = await workerApiRequest(
    `${API_BASE_URL}/status/location`,
    {
      method: "POST",
      body: JSON.stringify({
        latitude: coords.latitude,
        longitude: coords.longitude,
      }),
    },
  );

  if (!updatedStatus || updatedStatus.availabilityStatus !== "AVAILABLE") {
    throw new Error("The server did not confirm the location update.");
  }

  lastWorkerLocation = {
    latitude: coords.latitude,
    longitude: coords.longitude,
  };
  lastWorkerLocationUpdateAt = Date.now();
}

function ensureWorkerLocationTracking() {
  if (workerLocationStartAttempted
      || workerLocationWatchId !== null
      || workerLocationUpdateTimer !== null
      || currentWorkerStatus?.availabilityStatus !== "AVAILABLE") {
    return;
  }

  workerLocationStartAttempted = true;
  workerLocationAcquisitionInProgress = true;
  getCurrentBrowserPosition()
    .then(async (position) => {
      await updateWorkerLocation(position.coords);
      startWorkerLocationTracking(position.coords);
      showAvailabilityFeedback("Location tracking is active.");
    })
    .catch((error) => {
      console.error("Unable to start worker location tracking:", error);
      showAvailabilityFeedback(
        `Location tracking is not active: ${error.message}`,
        true,
      );
    })
    .finally(() => {
      workerLocationAcquisitionInProgress = false;
    });
}

function startWorkerLocationTracking(initialCoords) {
  if (currentWorkerStatus?.availabilityStatus !== "AVAILABLE"
      || workerLocationWatchId !== null
      || workerLocationUpdateTimer !== null) {
    return;
  }

  workerLocationStartAttempted = true;
  lastWorkerLocation = {
    latitude: initialCoords.latitude,
    longitude: initialCoords.longitude,
  };
  if (!lastWorkerLocationUpdateAt) {
    lastWorkerLocationUpdateAt = Date.now();
  }

  if (navigator.geolocation?.watchPosition) {
    workerLocationWatchId = navigator.geolocation.watchPosition(
      (position) => updateWorkerLocationOnMovement(position.coords),
      (error) => {
        if (error.code === error.PERMISSION_DENIED) {
          stopWorkerLocationTracking(false);
          showAvailabilityFeedback(
            "Location permission was revoked. Location tracking has stopped.",
            true,
          );
        } else {
          showAvailabilityFeedback(
            "Unable to read your location. Tracking will retry.",
            true,
          );
        }
      },
      {
        enableHighAccuracy: true,
        maximumAge: 60_000,
        timeout: 20_000,
      },
    );
  }

  workerLocationUpdateTimer = setInterval(
    refreshWorkerLocation,
    WORKER_LOCATION_UPDATE_INTERVAL_MS,
  );
}

async function updateWorkerLocationOnMovement(coords) {
  if (!lastWorkerLocation
      || workerLocationUpdateInProgress
      || currentWorkerStatus?.availabilityStatus !== "AVAILABLE"
      || Date.now() - lastWorkerLocationUpdateAt
        < WORKER_LOCATION_MIN_UPDATE_INTERVAL_MS
      || distanceBetweenCoordinates(
        lastWorkerLocation.latitude,
        lastWorkerLocation.longitude,
        coords.latitude,
        coords.longitude,
      ) < WORKER_LOCATION_MOVEMENT_THRESHOLD_METERS) {
    return;
  }

  await sendWorkerLocation(coords);
}

async function refreshWorkerLocation() {
  if (document.visibilityState !== "visible"
      || workerLocationAcquisitionInProgress
      || workerLocationUpdateInProgress
      || currentWorkerStatus?.availabilityStatus !== "AVAILABLE") {
    return;
  }

  workerLocationAcquisitionInProgress = true;
  try {
    const position = await getCurrentBrowserPosition();
    await sendWorkerLocation(position.coords);
  } catch (error) {
    console.error("Worker location refresh failed:", error);
    showAvailabilityFeedback(
      `Location update failed; your location may become stale. ${error.message}`,
      true,
    );
  } finally {
    workerLocationAcquisitionInProgress = false;
  }
}

async function sendWorkerLocation(coords) {
  if (workerLocationUpdateInProgress
      || currentWorkerStatus?.availabilityStatus !== "AVAILABLE") {
    return;
  }

  workerLocationUpdateInProgress = true;
  try {
    await updateWorkerLocation(coords);
    showAvailabilityFeedback("Location updated.");
  } catch (error) {
    console.error("Worker location update failed:", error);
    showAvailabilityFeedback(
      `Location update failed; your location may become stale. ${error.message}`,
      true,
    );
  } finally {
    workerLocationUpdateInProgress = false;
  }
}

function stopWorkerLocationTracking(resetStartAttempt = true) {
  if (workerLocationWatchId !== null && navigator.geolocation?.clearWatch) {
    navigator.geolocation.clearWatch(workerLocationWatchId);
    workerLocationWatchId = null;
  }

  if (workerLocationUpdateTimer !== null) {
    clearInterval(workerLocationUpdateTimer);
    workerLocationUpdateTimer = null;
  }

  workerLocationAcquisitionInProgress = false;
  lastWorkerLocation = null;
  lastWorkerLocationUpdateAt = 0;
  if (resetStartAttempt) {
    workerLocationStartAttempted = false;
  }
}

function distanceBetweenCoordinates(latitude1, longitude1, latitude2, longitude2) {
  const radians = (degrees) => (degrees * Math.PI) / 180;
  const latitudeDelta = radians(latitude2 - latitude1);
  const longitudeDelta = radians(longitude2 - longitude1);
  const haversine = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(radians(latitude1))
      * Math.cos(radians(latitude2))
      * Math.sin(longitudeDelta / 2) ** 2;

  return 6_371_000 * 2 * Math.atan2(
    Math.sqrt(haversine),
    Math.sqrt(1 - haversine),
  );
}

let bookingRequestRefreshTimer = null;
let lastBookingRequestsSnapshot = null;
let workerPartsRefreshTimer = null;
let workerPartsBookingId = null;

// Booking Requests

async function loadBookingRequests() {
  const container = document.getElementById("bookingRequestsContainer");

  if (!container) {
    return;
  }

  try {
    const requests = await workerApiRequest(
      `${API_BASE_URL}/bookings/requests`,
    );

    const snapshot = JSON.stringify(requests || []);

    // Nothing changed → keep the existing UI
    if (snapshot === lastBookingRequestsSnapshot) {
      return;
    }

    lastBookingRequestsSnapshot = snapshot;

    renderBookingRequests(requests);
  } catch (error) {
    console.error("Failed to load booking requests:", error);

    // Only show the error if we don't already have usable content.
    if (!lastBookingRequestsSnapshot) {
      container.innerHTML = `
        <div class="bg-gray-900 border border-red-900/40 rounded-2xl p-8 text-center">
          <p class="text-red-400 text-sm">
            Unable to load booking requests.
          </p>
        </div>
      `;
    }
  }
}

function startWorkerPartsPolling(bookingId) {
  if (workerPartsRefreshTimer) {
    return;
  }

  workerPartsBookingId = bookingId;

  workerPartsRefreshTimer = setInterval(async () => {
    if (!workerPartsBookingId) {
      return;
    }

    try {
      await loadWorkerBookingParts(workerPartsBookingId);
    } catch (error) {
      console.error("Worker parts refresh failed:", error);
    }
  }, 3000);
}

function stopWorkerPartsPolling() {
  if (workerPartsRefreshTimer) {
    clearInterval(workerPartsRefreshTimer);
    workerPartsRefreshTimer = null;
  }

  workerPartsBookingId = null;
}

function startBookingRequestPolling() {
  if (bookingRequestRefreshTimer) {
    return;
  }

  bookingRequestRefreshTimer = setInterval(async () => {
    try {
      await loadBookingRequests();
    } catch (error) {
      console.error("Booking request refresh failed:", error);
    }
  }, 3000);
}

function stopBookingRequestPolling() {
  if (bookingRequestRefreshTimer) {
    clearInterval(bookingRequestRefreshTimer);
    bookingRequestRefreshTimer = null;
  }
}

function startWorkerBookingPolling() {
  if (workerBookingRefreshTimer) {
    return;
  }

  workerBookingRefreshTimer = setInterval(async () => {
    try {
      await loadWorkerBookings();
      await loadWorkerStatus();
    } catch (error) {
      console.error("Worker booking refresh failed:", error);
    }
  }, 3000);
}

function stopWorkerBookingPolling() {
  if (workerBookingRefreshTimer) {
    clearInterval(workerBookingRefreshTimer);
    workerBookingRefreshTimer = null;
  }
}

async function workerApiRequest(url, options = {}) {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  const headers = {
    Authorization: `Bearer ${token}`,
    ...(options.headers || {}),
  };

  // FormData sets its own Content-Type including the multipart boundary.
  if (options.body instanceof FormData) {
    delete headers["Content-Type"];
  } else {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    window.location.href = "/HTML/login.html";
    return;
  }

  if (!response.ok) {
    let message = "Something went wrong.";

    try {
      const errorData = await response.json();
      message = errorData.message || errorData.error || message;
    } catch (e) {
      // Ignore JSON parsing errors.
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

// Render Booking Requests

function renderBookingRequests(requests) {
  const container = document.getElementById("bookingRequestsContainer");

  if (!container) {
    return;
  }

  if (!requests || requests.length === 0) {
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
                            flex items-center justify-center">

                    <i class="fa-solid
                              fa-calendar-plus
                              text-gray-500
                              text-2xl"></i>

                </div>


                <h4 class="text-lg
                           font-semibold
                           mb-2">

                    No booking requests

                </h4>


                <p class="text-gray-400 text-sm">

                    New service requests from customers
                    will appear here.

                </p>

            </div>
        `;

    return;
  }

  container.innerHTML = requests
    .map(
      (request) => `

            <div class="bg-gray-900
                        border border-gray-800
                        rounded-2xl
                        p-6">

                <div class="flex flex-col
                            lg:flex-row
                            lg:items-start
                            lg:justify-between
                            gap-6">


                    <!-- Request Information -->

                    <div class="flex-1">


                        <!-- Service -->

                        <div class="flex items-center
                                    gap-3 mb-4">

                            <div class="w-11 h-11
                                        rounded-xl
                                        bg-violet-900/30
                                        border border-violet-700/40
                                        flex items-center
                                        justify-center">

                                <i class="fa-solid
                                          fa-screwdriver-wrench
                                          text-violet-400"></i>

                            </div>


                            <div>

                                <p class="text-xs
                                          text-gray-400">

                                    Service Request

                                </p>

                                <h4 class="text-lg
                                           font-semibold">

                                    ${escapeHtml(
                                      request.serviceName || "Service",
                                    )}

                                </h4>

                            </div>

                        </div>


                        <!-- Customer -->

                        <div class="mb-4">

                            <p class="text-sm
                                      text-gray-400
                                      mb-1">

                                Customer

                            </p>

                            <p class="font-medium">

                                ${escapeHtml(
                                  request.customerName || "Customer",
                                )}

                            </p>

                        </div>


                        <!-- Customer Address -->

                        <div class="mb-4">

                            <div class="flex items-start gap-3">

                                <div class="w-9 h-9 rounded-lg
                                            bg-violet-900/30
                                            border border-violet-700/30
                                            flex items-center justify-center
                                            shrink-0">

                                    <i class="fa-solid
                                              fa-location-dot
                                              text-violet-400"></i>

                                </div>

                                <div>

                                    <p class="text-sm
                                              text-gray-400
                                              mb-1">

                                        Service Location

                                    </p>

                                    <p class="text-sm
                                              text-gray-300
                                              leading-6">

                                        ${
                                          [
                                            request.customerAddressLine1,
                                            request.customerCity,
                                          ]
                                            .filter(Boolean)
                                            .map((part) => escapeHtml(part))
                                            .join(", ") ||
                                          "Address not provided"
                                        }

                                    </p>

                                </div>

                            </div>

                        </div>


                        <!-- Note -->

                        ${
                          request.customerNote
                            ? `

                                <div class="mb-4">

                                    <p class="text-sm
                                              text-gray-400
                                              mb-2">

                                        Customer Note

                                    </p>

                                    <div class="bg-gray-800/50
                                                border border-gray-700
                                                rounded-xl
                                                p-4
                                                text-sm
                                                text-gray-300">

                                        ${escapeHtml(request.customerNote)}

                                    </div>

                                </div>

                            `
                            : ""
                        }


                        <!-- Requested Time -->

                        <p class="text-xs text-gray-500">

                            Requested
                            ${formatDateTime(request.createdAt)}

                        </p>

                    </div>


                    <!-- Actions -->

                    <div class="flex flex-col
                                sm:flex-row
                                lg:flex-col
                                gap-3
                                lg:min-w-36">

                        <button
                            class="accept-booking-btn
                                   px-5 py-2.5
                                   rounded-xl
                                   bg-green-600
                                   hover:bg-green-500
                                   text-white
                                   text-sm
                                   font-medium
                                   transition"
                            data-booking-id="${request.bookingId}">

                            <i class="fa-solid
                                      fa-check
                                      mr-2"></i>

                            Accept

                        </button>


                        <button
                            class="reject-booking-btn
                                   px-5 py-2.5
                                   rounded-xl
                                   bg-gray-800
                                   hover:bg-gray-700
                                   border border-gray-700
                                   text-gray-200
                                   text-sm
                                   font-medium
                                   transition"
                            data-booking-id="${request.bookingId}">

                            <i class="fa-solid
                                      fa-xmark
                                      mr-2"></i>

                            Reject

                        </button>

                    </div>

                </div>

            </div>

        `,
    )
    .join("");
}

//get worker location

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
              new Error("Location permission is required to accept a booking."),
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

// Accept Booking

async function acceptBooking(bookingId, button) {
  const confirmed = confirm("Are you sure you want to accept this booking?");

  if (!confirmed) {
    return;
  }

  const restore = button && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(button, "Accepting...")
    : null;
  if (button && window.SERVICEHUB_BUTTON && !restore) return;

  try {
    const location = await getCurrentLocation();

    await workerApiRequest(`${API_BASE_URL}/bookings/${bookingId}/accept`, {
      method: "POST",
      body: JSON.stringify({
        latitude: location.latitude,
        longitude: location.longitude,
      }),
    });

    await Promise.all([
      loadBookingRequests(),
      loadWorkerBookings(),
      loadWorkerStatus(),
    ]);
  } catch (error) {
    alert(error.message);
  } finally {
    if (restore) {
      restore();
    }
  }
}

// Reject Booking

async function rejectBooking(bookingId, button) {
  const confirmed = confirm("Are you sure you want to reject this booking?");

  if (!confirmed) {
    return;
  }

  const restore = button && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(button, "Rejecting...")
    : null;
  if (button && window.SERVICEHUB_BUTTON && !restore) return;

  try {
    await workerApiRequest(`${API_BASE_URL}/bookings/${bookingId}/reject`, {
      method: "POST",
    });

    /*
     * Rejecting does not change the booking status.
     * Therefore the request can still appear again.
     *
     * We simply refresh the list.
     */

    await loadBookingRequests();
  } catch (error) {
    alert(error.message);
  } finally {
    if (restore) {
      restore();
    }
  }
}

// Active Service

async function loadWorkerBookings() {
  const container = document.getElementById("activeServiceContainer");

  if (!container) {
    return;
  }

  try {
    const bookings = await workerApiRequest(`${API_BASE_URL}/bookings`);

    if (razorpayConfirmationCountdowns.size > 0) {
      const [bookingId, countdown] =
        razorpayConfirmationCountdowns.entries().next().value;
      renderRazorpayPaymentConfirmation(bookingId, countdown);
      return;
    }

    const confirmedRazorpayBooking = bookings.find(
      (booking) =>
        booking.status === "COMPLETED" &&
        booking.paymentMethod === "RAZORPAY" &&
        booking.paymentStatus === "SUCCESS" &&
        !dismissedRazorpayBookingIds.has(String(booking.bookingId)) &&
        sessionStorage.getItem(
          `servicehub_razorpay_confirmation_dismissed_${booking.bookingId}`,
        ) !== "true",
    );

    if (confirmedRazorpayBooking) {
      lastWorkerBookingsSnapshot = JSON.stringify(bookings);
      if (startRazorpayPaymentConfirmation(confirmedRazorpayBooking.bookingId)) {
        return;
      }
    }

    const startOtpInput = document.getElementById("startServiceOtp");
    const startOtpBooking = startOtpInput
      ? bookings.find(
          (booking) =>
            String(booking.bookingId) ===
            String(startOtpInput.dataset.bookingId),
        )
      : null;

    if (startOtpInput && startOtpBooking?.status === "ACCEPTED") {
      return;
    }

    // Avoid rebuilding the card when the booking data is unchanged.
    const snapshot = JSON.stringify(bookings);

    if (snapshot === lastWorkerBookingsSnapshot) {
      return;
    }

    // Save the snapshot before rendering.
    lastWorkerBookingsSnapshot = snapshot;

    renderActiveService(bookings);
  } catch (error) {
    console.error("Failed to load worker bookings:", error);

    // Keep the existing dashboard content visible on refresh failures.
    if (!container.innerHTML.trim()) {
      container.innerHTML = `
        <div class="bg-gray-900 border border-red-900/40
                    rounded-2xl p-8 text-center">
          <p class="text-red-400 text-sm">
            Unable to load active service.
          </p>
        </div>
      `;
    }
  }
}

function renderRazorpayPaymentConfirmation(bookingId, countdown) {
  const container = document.getElementById("activeServiceContainer");

  if (!container) {
    return;
  }

  countdown.secondsRemaining = Math.max(
    1,
    Math.ceil((countdown.deadline - Date.now()) / 1000),
  );

  container.innerHTML = `
    <div
      class="rounded-2xl border border-green-700/40
             bg-green-900/20 p-8 text-center"
      role="status"
      aria-live="polite"
    >
      <div
        class="mx-auto mb-4 flex h-14 w-14 items-center justify-center
               rounded-full bg-green-500/20 text-4xl font-bold text-green-400"
        aria-hidden="true"
      >
        ✓
      </div>
      <p class="text-lg font-semibold text-green-300">
        Payment Confirmed — Closing this work in
        <span id="razorpayPaymentCountdown">${countdown.secondsRemaining}</span>
        seconds
      </p>
    </div>
  `;
}

function startRazorpayPaymentConfirmation(bookingId) {
  const bookingKey = String(bookingId);
  const deadlineKey =
    `servicehub_razorpay_confirmation_deadline_${bookingKey}`;
  const dismissedKey =
    `servicehub_razorpay_confirmation_dismissed_${bookingKey}`;
  let countdown = razorpayConfirmationCountdowns.get(bookingKey);

  if (!countdown) {
    let deadline = Number(sessionStorage.getItem(deadlineKey));
    if (!deadline) {
      deadline = Date.now() + 5000;
      sessionStorage.setItem(deadlineKey, String(deadline));
    }

    const secondsRemaining = Math.ceil((deadline - Date.now()) / 1000);
    if (secondsRemaining <= 0) {
      dismissedRazorpayBookingIds.add(bookingKey);
      sessionStorage.setItem(dismissedKey, "true");
      sessionStorage.removeItem(deadlineKey);
      return false;
    }

    countdown = {
      deadline,
      secondsRemaining,
      timerId: null,
    };
    razorpayConfirmationCountdowns.set(bookingKey, countdown);
  }

  renderRazorpayPaymentConfirmation(bookingKey, countdown);

  if (countdown.timerId !== null) {
    return;
  }

  countdown.timerId = setInterval(() => {
    countdown.secondsRemaining = Math.ceil(
      (countdown.deadline - Date.now()) / 1000,
    );

    if (countdown.secondsRemaining <= 0) {
      clearInterval(countdown.timerId);
      countdown.timerId = null;
      razorpayConfirmationCountdowns.delete(bookingKey);
      dismissedRazorpayBookingIds.add(bookingKey);
      sessionStorage.setItem(dismissedKey, "true");
      sessionStorage.removeItem(deadlineKey);
      lastWorkerBookingsSnapshot = null;
      const container = document.getElementById("activeServiceContainer");
      if (container) {
        container.innerHTML = "";
      }
      loadWorkerBookings();
      return;
    }

    const countdownElement = document.getElementById(
      "razorpayPaymentCountdown",
    );
    if (countdownElement) {
      countdownElement.textContent = String(countdown.secondsRemaining);
    }
  }, 1000);

  return true;
}

// Render Active Service

function renderActiveService(bookings) {
  const container = document.getElementById("activeServiceContainer");

  if (!container) {
    return;
  }

  const activeStatuses = [
    "ACCEPTED",
    "IN_PROGRESS",
    "WAITING_FOR_PAYMENT",
  ];

  const activeBooking = bookings.find((booking) =>
    activeStatuses.includes(booking.status),
  );

  if (!activeBooking) {
    container.innerHTML = `

            <div class="text-center">

                <div class="w-16 h-16
                            mx-auto mb-5
                            rounded-full
                            bg-gray-800
                            flex items-center
                            justify-center">

                    <i class="fa-solid
                              fa-briefcase
                              text-gray-500
                              text-2xl"></i>

                </div>


                <h4 class="text-lg
                           font-semibold
                           mb-2">

                    No active service

                </h4>


                <p class="text-gray-400 text-sm">

                    An accepted service will appear here.

                </p>

            </div>

        `;

    return;
  }

  const statusInfo = getBookingStatusInfo(activeBooking.status);

  let actionButton = "";
  let paymentSection = "";

  // ACCEPTED
  if (activeBooking.status === "ACCEPTED") {
    actionButton = `
    <div class="space-y-3">

      <button
        class="start-service-btn
               w-full h-12 px-4
               rounded-xl
               bg-violet-600
               hover:bg-violet-500
               text-white
               font-medium
               transition"
        data-booking-id="${activeBooking.bookingId}"
      >
        <i class="fa-solid fa-play mr-2"></i>
        Start Service
      </button>

      <button
        class="cancel-service-btn
               w-full h-12 px-4
               rounded-xl
               bg-gray-800
               hover:bg-gray-700
               border border-gray-700
               text-gray-200
               font-medium
               transition"
        data-booking-id="${activeBooking.bookingId}"
        data-booking-status="ACCEPTED"
      >
        <i class="fa-solid fa-xmark mr-2"></i>
        Cancel
      </button>

    </div>
  `;
  }

  // IN PROGRESS
  if (activeBooking.status === "IN_PROGRESS") {
    // Completion OTP has already been requested
    if (activeBooking.completionRequested) {
      actionButton = `
      <div class="space-y-3">

        <div>
          <label
            for="completionServiceOtp"
            class="block text-sm font-medium text-gray-300 mb-2"
          >
            Customer Completion OTP
          </label>

          <input
            type="text"
            id="completionServiceOtp"
            maxlength="4"
            inputmode="numeric"
            autocomplete="one-time-code"
            placeholder="Enter 4-digit OTP"
            class="w-full h-12 px-4 rounded-xl
                   bg-gray-800
                   border border-gray-700
                   text-gray-100
                   placeholder-gray-500
                   text-center text-lg font-semibold
                   tracking-[0.35em]
                   focus:outline-none
                   focus:border-violet-500"
          />
        </div>

        <button
          class="complete-service-btn
                 w-full h-12 px-4
                 rounded-xl
                 bg-green-600
                 hover:bg-green-500
                 text-white
                 font-medium
                 transition"
          data-booking-id="${activeBooking.bookingId}"
        >
          <i class="fa-solid fa-check mr-2"></i>
          Complete Service
        </button>

        <div
          class="rounded-xl
                 bg-violet-950/20
                 border border-violet-700/30
                 p-4"
        >
          <div class="flex items-start gap-3">

            <div
              class="w-9 h-9 rounded-lg
                     bg-violet-500/10
                     flex items-center
                     justify-center
                     shrink-0"
            >
              <i class="fa-solid fa-key text-violet-400"></i>
            </div>

            <div>
              <p class="text-sm font-semibold text-violet-300">
                Completion OTP Generated
              </p>

              <p class="text-xs text-gray-400 mt-1 leading-relaxed">
                The Completion OTP is now visible to the customer.
                Ask the customer for the OTP only after the service
                is fully completed and they have checked the work.
              </p>
            </div>

          </div>
        </div>

        <button
          class="cancel-service-btn
                 w-full h-12 px-4
                 rounded-xl
                 bg-gray-800
                 hover:bg-gray-700
                 border border-gray-700
                 text-gray-200
                 font-medium
                 transition"
          data-booking-id="${activeBooking.bookingId}"
          data-booking-status="IN_PROGRESS"
        >
          <i class="fa-solid fa-xmark mr-2"></i>
          Cancel Service
        </button>

      </div>
    `;
    }

    // Completion OTP has NOT been requested yet
    else {
      actionButton = `
      <div class="space-y-3">

        <button
          class="request-completion-btn
                 w-full h-12 px-4
                 rounded-xl
                 bg-violet-600
                 hover:bg-violet-500
                 text-white
                 font-medium
                 transition"
          data-booking-id="${activeBooking.bookingId}"
        >
          <i class="fa-solid fa-flag-checkered mr-2"></i>
          Mark Service as Complete
        </button>

        <button
          class="cancel-service-btn
                 w-full h-12 px-4
                 rounded-xl
                 bg-gray-800
                 hover:bg-gray-700
                 border border-gray-700
                 text-gray-200
                 font-medium
                 transition"
          data-booking-id="${activeBooking.bookingId}"
          data-booking-status="IN_PROGRESS"
        >
          <i class="fa-solid fa-xmark mr-2"></i>
          Cancel Service
        </button>

      </div>
    `;
    }
  }

  // WAITING FOR PAYMENT
  if (activeBooking.status === "WAITING_FOR_PAYMENT") {
    const method = activeBooking.paymentMethod;
    const paymentStatus = activeBooking.paymentStatus;

    let message = "Waiting for the customer to select a payment method.";
    let button = "";

    if (method === "RAZORPAY") {
      if (paymentStatus === "SUCCESS") {
        message =
          "Razorpay has verified the payment. The booking will be completed automatically.";
      } else if (paymentStatus === "FAILED") {
        message =
          "The online payment failed. Ask the customer to retry or choose cash.";
      } else if (paymentStatus === "CANCELLED") {
        message =
          "The online payment was cancelled. Waiting for the customer to retry or choose cash.";
      } else {
        message =
          "Waiting for the customer to complete online payment. Confirmation is unavailable until payment succeeds.";
      }
    } else if (method === "CASH") {
      if (paymentStatus === "PENDING") {
        message =
          "Collect the cash from the customer before confirming receipt.";

        button = `
          <button
            type="button"
            class="confirm-worker-payment-btn w-full h-12 px-4
                   rounded-xl bg-green-600 hover:bg-green-500
                   text-white font-medium transition"
            data-booking-id="${activeBooking.bookingId}"
            data-payment-method="CASH"
          >
            <i class="fa-solid fa-money-bill-wave mr-2"></i>
            Confirm Cash Received
          </button>
        `;
      } else {
        message = "Cash payment is not awaiting confirmation.";
      }
    }

    paymentSection = `
      <div class="rounded-xl border border-gray-700
                  bg-gray-800/50 p-5 mb-6">
        <h3 class="font-semibold text-white mb-2">
          <i class="fa-solid fa-wallet mr-2 text-violet-400"></i>
          Payment Method: ${
            method === "CASH"
              ? "Cash"
              : method === "RAZORPAY"
                ? "Razorpay"
                : "Not selected"
          }
        </h3>
        <p class="text-sm text-gray-400 mb-2">
          Payment Status: ${escapeHtml(paymentStatus || "NOT_STARTED")}
        </p>
        <p class="text-sm text-gray-300 leading-relaxed">
          ${message}
        </p>
      </div>
    `;

    actionButton = button;
  }

  /*
   * Build the customer's complete address.
   *
   * Only the fields that actually contain
   * information will be displayed.
   */

  const addressParts = [
    activeBooking.customerAddressLine1,

    activeBooking.customerAddressLine2,

    activeBooking.customerLandmark,

    activeBooking.customerCity,

    activeBooking.customerState,

    activeBooking.customerPinCode,
  ].filter(Boolean);

  const customerAddress =
    addressParts.length > 0
      ? addressParts.map((part) => escapeHtml(part)).join(", ")
      : "Address not provided";

  const customerPhone = activeBooking.customerPhone
    ? escapeHtml(activeBooking.customerPhone)
    : "Phone number not provided";

  const callButton = activeBooking.customerPhone
    ? `

                <a
                    href="tel:${encodeURIComponent(
                      activeBooking.customerPhone,
                    )}"
                    class="inline-flex
                           items-center
                           justify-center
                           gap-2
                           w-full h-12 px-4
                           rounded-xl
                           bg-green-600
                           hover:bg-green-500
                           text-white
                           text-sm
                           font-medium
                           transition">

                    <i class="fa-solid fa-phone"></i>

                    Call Customer

                </a>

              `
    : "";

  const partsSection =
    activeBooking.status === "IN_PROGRESS"
      ? `
        <div id="workerPartsContainer"
             class="mt-6 pt-6 border-t border-gray-800">
          <div class="text-left">

            <div class="flex items-center justify-between mb-4">
              <div>
                <h3 class="text-lg font-semibold text-white">
                  Parts
                </h3>

                <p class="text-sm text-gray-400 mt-1">
                  ${
                    activeBooking.completionRequested
                      ? "Completion has been requested. No more parts can be added."
                      : "Add parts used for this service."
                  }
                </p>
              </div>

              ${
                !activeBooking.completionRequested
                  ? `
                    <button
                      id="addPartButton"
                      data-booking-id="${activeBooking.bookingId}"
                      type="button"
                      class="px-4 py-2 bg-blue-600 hover:bg-blue-700
                             text-white text-sm font-medium rounded-lg
                             transition">
                      + Add Part
                    </button>
                  `
                  : ""
              }
            </div>

            <div id="workerPartsList"
                 class="space-y-3">
              <p class="text-sm text-gray-400">
                Loading parts...
              </p>
            </div>

          </div>
        </div>
      `
      : "";
  container.innerHTML = `

        <div class="text-left">


            <!-- Header -->

            <div class="flex flex-col
                        md:flex-row
                        md:items-start
                        md:justify-between
                        gap-5 mb-8">


                <div>

                    <p class="text-sm
                              text-gray-400
                              mb-2">

                        Current Customer

                    </p>


                    <h4 class="text-2xl
                               font-bold">

                        ${escapeHtml(activeBooking.customerName || "Customer")}

                    </h4>

                </div>


                <span class="inline-flex
                             items-center
                             self-start
                             px-3 py-1.5
                             rounded-full
                             text-xs
                             font-medium
                             ${statusInfo.classes}">

                    ${statusInfo.label}

                </span>

            </div>



            <!-- Service + Requested Time -->

            <div class="grid md:grid-cols-2
                        gap-4 mb-6">


                <div class="bg-gray-800/50
                            border border-gray-700
                            rounded-xl
                            p-5">

                    <p class="text-sm
                              text-gray-400
                              mb-1">

                        Service

                    </p>


                    <p class="font-semibold">

                        ${escapeHtml(activeBooking.serviceName || "Service")}

                    </p>

                </div>


                <div class="bg-gray-800/50
                            border border-gray-700
                            rounded-xl
                            p-5">

                    <p class="text-sm
                              text-gray-400
                              mb-1">

                        Requested

                    </p>


                    <p class="font-semibold">

                        ${formatDateTime(activeBooking.createdAt)}

                    </p>

                </div>

            </div>



            <!-- Customer Contact -->

            <div class="bg-gray-800/50
                        border border-gray-700
                        rounded-xl
                        p-5 mb-6">


                <div class="flex items-center
                            gap-3 mb-4">

                    <div class="w-10 h-10
                                rounded-xl
                                bg-green-900/30
                                border border-green-700/40
                                flex items-center
                                justify-center">

                        <i class="fa-solid
                                  fa-address-card
                                  text-green-400"></i>

                    </div>


                    <div>

                        <p class="text-sm
                                  text-gray-400">

                            Customer Contact

                        </p>

                        <p class="font-semibold">

                            ${escapeHtml(
                              activeBooking.customerName || "Customer",
                            )}

                        </p>

                    </div>

                </div>


                <div class="flex flex-col
                            sm:flex-row
                            sm:items-center
                            gap-3">


                    <div class="flex items-center
                                gap-3
                                text-gray-300">

                        <i class="fa-solid
                                  fa-phone
                                  text-gray-500"></i>

                        <span>

                            ${customerPhone}

                        </span>

                    </div>


                    ${callButton}

                </div>

            </div>



            <!-- Customer Address -->

            <div class="bg-gray-800/50
                        border border-gray-700
                        rounded-xl
                        p-5 mb-6">


                <div class="flex items-center
                            gap-3 mb-4">

                    <div class="w-10 h-10
                                rounded-xl
                                bg-violet-900/30
                                border border-violet-700/40
                                flex items-center
                                justify-center">

                        <i class="fa-solid
                                  fa-location-dot
                                  text-violet-400"></i>

                    </div>


                    <div>

                        <p class="text-sm
                                  text-gray-400">

                            Service Location

                        </p>

                        <p class="font-semibold">

                            Customer Address

                        </p>

                    </div>

                </div>


                <div class="flex items-start
                            gap-3
                            text-gray-300">

                    <i class="fa-solid
                              fa-map-pin
                              text-gray-500
                              mt-1"></i>


                    <p class="text-sm
                              leading-6">

                        ${customerAddress}

                    </p>

                </div>

            </div>



            <!-- Customer Note -->

            ${
              activeBooking.customerNote
                ? `

                    <div class="mb-8">

                        <p class="text-sm
                                  text-gray-400
                                  mb-2">

                            Customer Note

                        </p>


                        <div class="bg-gray-800/50
                                    border border-gray-700
                                    rounded-xl
                                    p-4
                                    text-sm
                                    text-gray-300">

                            ${escapeHtml(activeBooking.customerNote)}

                        </div>

                    </div>

                  `
                : ""
            }

              ${partsSection}

              ${paymentSection}

            <!-- Actions -->

            <div class="grid grid-cols-1
                items-stretch
                gap-4 mt-6">

                ${actionButton}

            </div>

        </div>

    `;

  if (activeBooking.status === "IN_PROGRESS") {
    loadWorkerBookingParts(activeBooking.bookingId);
    startWorkerPartsPolling(activeBooking.bookingId);
  } else {
    stopWorkerPartsPolling();
  }
}

// Worker Cancellation

const ACCEPTED_CANCELLATION_REASONS = [
  { value: "EMERGENCY_PERSONAL_ISSUE", label: "Emergency / personal issue" },
  { value: "VEHICLE_TRANSPORT_PROBLEM", label: "Vehicle / transport problem" },
  { value: "UNABLE_TO_REACH_CUSTOMER", label: "Unable to reach customer" },
  {
    value: "INCORRECT_BOOKING_SERVICE_INFORMATION",
    label: "Incorrect booking / service information",
  },
  { value: "OTHER", label: "Other" },
];

const IN_PROGRESS_CANCELLATION_REASONS = [
  { value: "CANNOT_SOLVE_PROBLEM", label: "Cannot solve the problem" },
  {
    value: "REQUIRES_DIFFERENT_EXPERTISE",
    label: "Requires different expertise",
  },
  {
    value: "REQUIRED_EQUIPMENT_UNAVAILABLE",
    label: "Required equipment unavailable",
  },
  {
    value: "REQUIRED_PART_MATERIAL_UNAVAILABLE",
    label: "Required part / material unavailable",
  },
  { value: "OTHER", label: "Other" },
];

function showWorkerCancellationDialog(bookingId, bookingStatus) {
  return new Promise((resolve) => {
    const reasons =
      bookingStatus === "ACCEPTED"
        ? ACCEPTED_CANCELLATION_REASONS
        : IN_PROGRESS_CANCELLATION_REASONS;

    const modal = document.createElement("div");

    modal.id = "workerCancellationModal";
    modal.className =
      "fixed inset-0 z-[100] flex items-center justify-center " +
      "bg-black/70 backdrop-blur-sm px-4";

    modal.innerHTML = `
            <div class="w-full max-w-lg bg-gray-900 border border-gray-800 rounded-2xl shadow-2xl p-6">
                <div class="flex items-start justify-between gap-4 mb-6">
                    <div>
                        <h3 class="text-xl font-bold">Cancel Booking</h3>
                        <p class="text-sm text-gray-400 mt-1">
                            ${
                              bookingStatus === "ACCEPTED"
                                ? "Please select a reason for cancelling this booking."
                                : "Please tell us why you cannot complete this service."
                            }
                        </p>
                    </div>
                    <button type="button" id="closeWorkerCancellationModal" class="text-gray-500 hover:text-gray-300 text-xl">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                </div>

                <div class="mb-5">
                    <label for="workerCancellationReason" class="block text-sm font-medium text-gray-300 mb-2">Reason</label>
                    <select id="workerCancellationReason" class="w-full bg-gray-800 border border-gray-700 rounded-xl px-4 py-3 text-gray-100 focus:outline-none focus:border-violet-500">
                        <option value="">Select a reason</option>
                        ${reasons.map((reason) => `<option value="${reason.value}">${reason.label}</option>`).join("")}
                    </select>
                </div>

                <div id="workerCancellationMessageContainer" class="mb-5 hidden">
                    <label for="workerCancellationMessage" class="block text-sm font-medium text-gray-300 mb-2">Please explain</label>
                    <textarea id="workerCancellationMessage" rows="4" maxlength="1000" placeholder="Please provide a short explanation..." class="w-full bg-gray-800 border border-gray-700 rounded-xl px-4 py-3 text-gray-100 placeholder-gray-500 resize-none focus:outline-none focus:border-violet-500"></textarea>
                </div>

                <p id="workerCancellationError" class="text-sm text-red-400 mb-4 hidden"></p>

                <div class="flex justify-end gap-3">
                    <button type="button" id="cancelWorkerCancellation" class="px-5 py-2.5 rounded-xl bg-gray-800 hover:bg-gray-700 border border-gray-700 text-gray-200 text-sm font-medium transition">Keep Booking</button>
                    <button type="button" id="confirmWorkerCancellation" class="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-sm font-medium transition">Confirm Cancellation</button>
                </div>
            </div>
        `;

    document.body.appendChild(modal);

    const reasonSelect = document.getElementById("workerCancellationReason");
    const messageContainer = document.getElementById(
      "workerCancellationMessageContainer",
    );
    const messageInput = document.getElementById("workerCancellationMessage");
    const errorElement = document.getElementById("workerCancellationError");

    function closeModal(result = null) {
      modal.remove();
      resolve(result);
    }

    reasonSelect.addEventListener("change", () => {
      if (reasonSelect.value === "OTHER") {
        messageContainer.classList.remove("hidden");
      } else {
        messageContainer.classList.add("hidden");
        messageInput.value = "";
      }

      errorElement.classList.add("hidden");
      errorElement.textContent = "";
    });

    document
      .getElementById("closeWorkerCancellationModal")
      .addEventListener("click", () => closeModal(null));
    document
      .getElementById("cancelWorkerCancellation")
      .addEventListener("click", () => closeModal(null));
    document
      .getElementById("confirmWorkerCancellation")
      .addEventListener("click", () => {
        const reason = reasonSelect.value;
        const message = messageInput.value.trim();

        if (!reason) {
          errorElement.textContent = "Please select a reason.";
          errorElement.classList.remove("hidden");
          return;
        }

        if (reason === "OTHER" && !message) {
          errorElement.textContent = "Please provide an explanation.";
          errorElement.classList.remove("hidden");
          return;
        }

        closeModal({ reason, message });
      });
  });
}

async function cancelWorkerBooking(bookingId, bookingStatus, triggerBtn) {
  const cancellation = await showWorkerCancellationDialog(
    bookingId,
    bookingStatus,
  );

  if (!cancellation) {
    return;
  }

  if (!confirm("Are you sure you want to cancel this booking?")) {
    return;
  }

  const restore = triggerBtn && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(triggerBtn, "Cancelling...")
    : null;
  if (triggerBtn && window.SERVICEHUB_BUTTON && !restore) return;

  try {
    await workerApiRequest(`${API_BASE_URL}/bookings/${bookingId}/cancel`, {
      method: "POST",
      body: JSON.stringify({
        reason: cancellation.reason,
        message: cancellation.message,
      }),
    });

    await Promise.all([loadWorkerBookings(), loadWorkerStatus()]);
  } catch (error) {
    alert(error.message);
  } finally {
    if (restore) {
      restore();
    }
  }
}

// Start Service

async function startService(bookingId, triggerBtn) {
  const startButton = triggerBtn || document.querySelector(
    `.start-service-btn[data-booking-id="${bookingId}"]`,
  );

  if (!startButton) {
    alert("Unable to start the service.");
    return;
  }

  const actionContainer = startButton.closest(".space-y-3");

  if (!actionContainer) {
    alert("Unable to open Start Service.");
    return;
  }

  const existingOtpInput = document.getElementById("startServiceOtp");

  // --------------------------------------------------
  // SECOND CLICK → VERIFY OTP AND START SERVICE
  // --------------------------------------------------
  if (existingOtpInput) {
    const otp = existingOtpInput.value.trim();

    if (!otp) {
      alert("Please enter the 4-digit Start OTP provided by the customer.");
      existingOtpInput.focus();
      return;
    }

    if (!/^\d{4}$/.test(otp)) {
      alert("Please enter a valid 4-digit Start OTP.");
      existingOtpInput.focus();
      existingOtpInput.select();
      return;
    }

    const restore = window.SERVICEHUB_BUTTON
      ? window.SERVICEHUB_BUTTON.setLoading(startButton, "Starting...")
      : null;
    if (window.SERVICEHUB_BUTTON && !restore) return;

    try {
      await workerApiRequest(`${API_BASE_URL}/bookings/${bookingId}/start`, {
        method: "POST",
        body: JSON.stringify({
          otp: otp,
        }),
      });

      await Promise.all([loadWorkerBookings(), loadWorkerStatus()]);
    } catch (error) {
      alert(error.message);
      existingOtpInput.focus();
      existingOtpInput.select();
    } finally {
      if (restore) restore();
    }

    return;
  }

  // --------------------------------------------------
  // FIRST CLICK → REQUEST START OTP
  // --------------------------------------------------
  const restoreFirst = window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(startButton, "Requesting OTP...")
    : null;
  if (window.SERVICEHUB_BUTTON && !restoreFirst) return;

  try {
    await workerApiRequest(
      `${API_BASE_URL}/bookings/${bookingId}/request-start`,
      {
        method: "POST",
      },
    );

    // Show OTP input only after backend successfully
    // generates the Start OTP.
    actionContainer.insertAdjacentHTML(
      "afterbegin",
      `
        <div id="startOtpSection">
          <label
            for="startServiceOtp"
            class="block text-sm font-medium text-gray-300 mb-2"
          >
            Customer Start OTP
          </label>

          <input
            type="text"
            id="startServiceOtp"
            data-booking-id="${bookingId}"
            maxlength="4"
            inputmode="numeric"
            autocomplete="one-time-code"
            placeholder="Enter 4-digit OTP"
            class="w-full h-12 px-4 rounded-xl
                   bg-gray-800
                   border border-gray-700
                   text-gray-100
                   placeholder-gray-500
                   text-center text-lg font-semibold
                   tracking-[0.35em]
                   focus:outline-none
                   focus:border-violet-500"
          />

          <p class="text-xs text-gray-500 mt-2">
            Ask the customer for the Start OTP in person before starting the service.
          </p>
        </div>
      `,
    );

    document.getElementById("startServiceOtp")?.focus();
  } catch (error) {
    alert(error.message);
  } finally {
    if (restoreFirst) restoreFirst();
  }
}

//request for completion

async function requestCompletion(bookingId, button) {
  const confirmed = confirm(
    "Are you sure the service is fully completed and ready for customer verification?",
  );

  if (!confirmed) {
    return;
  }

  const targetBtn = button || document.querySelector(`.request-completion-btn[data-booking-id="${bookingId}"]`);
  const restore = targetBtn && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(targetBtn, "Requesting...")
    : null;
  if (targetBtn && window.SERVICEHUB_BUTTON && !restore) return;

  try {
    await workerApiRequest(
      `${API_BASE_URL}/bookings/${bookingId}/request-completion`,
      {
        method: "POST",
      },
    );

    await loadWorkerBookings();
  } catch (error) {
    if (error.message.includes("Completion has already been requested")) {
      await loadWorkerBookings();
      return;
    }

    alert(error.message);
  } finally {
    if (restore) restore();
  }
}

// Complete Service

async function completeService(bookingId, button) {
  const otpInput = document.getElementById("completionServiceOtp");

  if (!otpInput) {
    alert("Completion OTP field is unavailable.");
    return;
  }

  const otp = otpInput.value.trim();

  if (!/^\d{4}$/.test(otp)) {
    alert("Please enter the 4-digit Completion OTP provided by the customer.");
    otpInput.focus();
    return;
  }

  const targetBtn = button || document.querySelector(`.complete-service-btn[data-booking-id="${bookingId}"]`);
  const restore = targetBtn && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(targetBtn, "Completing...")
    : null;
  if (targetBtn && window.SERVICEHUB_BUTTON && !restore) return;

  try {
    await workerApiRequest(`${API_BASE_URL}/bookings/${bookingId}/complete`, {
      method: "POST",
      body: JSON.stringify({
        otp: otp,
      }),
    });

    await Promise.all([loadWorkerBookings(), loadWorkerStatus()]);
  } catch (error) {
    alert(error.message);
  } finally {
    if (restore) restore();
  }
}

// Confirm Cash Payment

async function confirmWorkerPayment(bookingId, paymentMethod, button) {
  if (paymentMethod !== "CASH") {
    return;
  }

  const message = "Confirm that you have actually received the cash from the customer?";

  if (!confirm(message)) {
    return;
  }

  const targetBtn = button || document.querySelector(`.confirm-payment-btn[data-booking-id="${bookingId}"]`);
  const restore = targetBtn && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(targetBtn, "Confirming...")
    : null;
  if (targetBtn && window.SERVICEHUB_BUTTON && !restore) return;

  try {
    await workerApiRequest(
      `${API_BASE_URL}/payments/cash/${encodeURIComponent(bookingId)}/confirm`,
      {
        method: "POST",
      },
    );

    await Promise.all([loadWorkerBookings(), loadWorkerStatus()]);
  } catch (error) {
    alert(error.message);
  } finally {
    if (restore) restore();
  }
}

// Event Delegation

document.addEventListener("click", (event) => {
  const confirmPaymentButton = event.target.closest(
    ".confirm-worker-payment-btn",
  );

  if (confirmPaymentButton) {
    confirmWorkerPayment(
      confirmPaymentButton.dataset.bookingId,
      confirmPaymentButton.dataset.paymentMethod,
      confirmPaymentButton,
    );

    return;
  }

  const acceptButton = event.target.closest(".accept-booking-btn");

  if (acceptButton) {
    acceptBooking(acceptButton.dataset.bookingId, acceptButton);

    return;
  }

  const rejectButton = event.target.closest(".reject-booking-btn");

  if (rejectButton) {
    rejectBooking(rejectButton.dataset.bookingId, rejectButton);

    return;
  }

  const startButton = event.target.closest(".start-service-btn");

  if (startButton) {
    startService(startButton.dataset.bookingId, startButton);

    return;
  }

  const requestCompletionButton = event.target.closest(
    ".request-completion-btn",
  );

  if (requestCompletionButton) {
    requestCompletion(requestCompletionButton.dataset.bookingId, requestCompletionButton);

    return;
  }

  const completeButton = event.target.closest(".complete-service-btn");

  if (completeButton) {
    completeService(completeButton.dataset.bookingId, completeButton);

    return;
  }

  const addPartButton = event.target.closest("#addPartButton");

  if (addPartButton) {
    openAddPartModal(addPartButton.dataset.bookingId);

    return;
  }

  const cancelButton = event.target.closest(".cancel-service-btn");

  if (cancelButton) {
    cancelWorkerBooking(
      cancelButton.dataset.bookingId,
      cancelButton.dataset.bookingStatus,
      cancelButton,
    );
  }
});

// Helpers

function getBookingStatusInfo(status) {
  const statuses = {
    ACCEPTED: {
      label: "Accepted",

      classes:
        "bg-blue-900/30 " + "text-blue-400 " + "border border-blue-700/30",
    },

    IN_PROGRESS: {
      label: "In Progress",

      classes:
        "bg-yellow-900/30 " +
        "text-yellow-400 " +
        "border border-yellow-700/30",
    },
  };

  return (
    statuses[status] || {
      label: status || "Unknown",

      classes: "bg-gray-800 " + "text-gray-400 " + "border border-gray-700",
    }
  );
}

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

async function loadWorkerBookingParts(bookingId) {
  const partsList = document.getElementById("workerPartsList");

  if (!partsList) {
    return;
  }

  try {
    const parts = await workerApiRequest(
      `${API_BASE_URL}/bookings/${bookingId}/parts`,
    );

    const currentParts = parts || [];
    const snapshot = JSON.stringify(currentParts);

    if (partsList.dataset.partsSnapshot === snapshot) {
      return;
    }

    partsList.dataset.partsSnapshot = snapshot;

    renderWorkerParts(currentParts);
  } catch (error) {
    console.error("Failed to load booking parts:", error);

    partsList.dataset.partsSnapshot = "";

    partsList.innerHTML = `
            <p class="text-sm text-red-400">
                Unable to load parts.
            </p>
        `;
  }
}

function renderWorkerParts(parts) {
  const partsList = document.getElementById("workerPartsList");

  if (!partsList) {
    return;
  }

  if (!parts || parts.length === 0) {
    partsList.innerHTML = `
            <div class="border border-dashed border-gray-700
                        rounded-lg p-5 text-center">
                <p class="text-sm text-gray-400">
                    No parts have been added yet.
                </p>
            </div>
        `;
    return;
  }

  partsList.innerHTML = parts
    .map(
      (part) => `
        <div class="bg-gray-800/70 border border-gray-700
                    rounded-lg p-4">

            <div class="flex items-start justify-between gap-4">

                <div>
                    <h4 class="text-white font-medium">
                        ${escapeHtml(part.partName)}
                    </h4>

                    <p class="text-sm text-gray-400 mt-1">
                        ₹${Number(part.unitPrice).toFixed(2)}
                        × ${part.quantity}
                    </p>

                    <p class="text-sm text-gray-300 mt-1">
                        Total: ₹${Number(part.totalPrice).toFixed(2)}
                    </p>
                </div>

                <span class="
                    px-2.5 py-1 rounded-full text-xs font-medium
                    ${
                      part.status === "APPROVED"
                        ? "bg-green-900/40 text-green-400"
                        : "bg-yellow-900/40 text-yellow-400"
                    }
                ">
                    ${
                      part.status === "APPROVED"
                        ? "Approved"
                        : "Waiting for customer"
                    }
                </span>

            </div>

            <div class="mt-4">

              <img
                  id="part-photo-${part.bookingPartId}"
                  alt="Part photo"
                  class="hidden w-28 h-28
                        object-cover
                        rounded-xl
                        border border-gray-700
                        bg-gray-900
                        cursor-pointer
                        hover:opacity-90
                        transition"
                  title="Click to view image"
              >

          </div>

        </div>
    `,
    )
    .join("");

  parts.forEach((part) => {
    loadWorkerPartPhoto(
      part.bookingPartId,
      document.getElementById(`part-photo-${part.bookingPartId}`),
    );
  });
}

async function loadWorkerPartPhoto(partId, imageElement) {
  if (!imageElement) {
    return;
  }

  try {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);

    const response = await fetch(
      `${API_ROOT}/api/customer/bookings/parts/${partId}/photo`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      },
    );

    if (!response.ok) {
      return;
    }

    const blob = await response.blob();
    const imageUrl = URL.createObjectURL(blob);

    imageElement.src = imageUrl;
    imageElement.classList.remove("hidden");
    imageElement.style.cursor = "pointer";

    imageElement.onclick = () => {
      openWorkerPartImageViewer(partId);
    };
  } catch (error) {
    console.error("Failed to load part photo:", error);
  }
}

async function openWorkerPartImageViewer(partId) {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  try {
    const response = await fetch(
      `${API_ROOT}/api/customer/bookings/parts/${partId}/photo`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      },
    );

    if (!response.ok) {
      throw new Error("Unable to load part photo.");
    }

    const blob = await response.blob();
    const imageUrl = URL.createObjectURL(blob);

    const existingViewer = document.getElementById("workerPartImageViewer");

    if (existingViewer) {
      existingViewer.remove();
    }

    const viewer = document.createElement("div");

    viewer.id = "workerPartImageViewer";

    viewer.className =
      "fixed inset-0 z-[200] " +
      "flex items-center justify-center " +
      "bg-black/80 backdrop-blur-sm " +
      "px-4 py-8 " +
      "opacity-0 transition-opacity duration-200";

    viewer.innerHTML = `
      <div
        id="workerPartImageCard"
        class="relative
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
               duration-200"
      >

        <button
          type="button"
          id="closeWorkerPartImageViewer"
          class="absolute
                 top-3 right-3
                 w-9 h-9
                 rounded-full
                 bg-gray-800/90
                 border border-gray-700
                 text-gray-300
                 hover:text-white
                 hover:bg-gray-700
                 transition
                 z-10"
        >
          <i class="fa-solid fa-xmark"></i>
        </button>

        <img
          src="${imageUrl}"
          alt="Part photo"
          class="block
                 max-w-[74vw]
                 max-h-[72vh]
                 object-contain
                 rounded-xl"
        >

      </div>
    `;

    document.body.appendChild(viewer);

    const imageCard = document.getElementById("workerPartImageCard");

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
      .getElementById("closeWorkerPartImageViewer")
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
    console.error("Failed to open part photo:", error);
  }
}

async function openAddPartModal(bookingId) {
  const existingModal = document.getElementById("addPartModal");

  if (existingModal) {
    existingModal.remove();
  }

  const modal = document.createElement("div");

  modal.id = "addPartModal";
  modal.className =
    "fixed inset-0 z-[100] flex items-center justify-center " +
    "bg-black/70 backdrop-blur-sm px-4 py-6";

  modal.innerHTML = `
    <div
      class="w-full max-w-md max-h-[88vh] overflow-y-auto
             bg-gray-900 border border-gray-800
             rounded-2xl shadow-2xl p-5"
    >

      <!-- Header -->
      <div class="flex items-start justify-between gap-4 mb-5">

        <div>
          <h3 class="text-xl font-bold text-white">
            Add Part
          </h3>

          <p class="text-sm text-gray-400 mt-1">
            Add the part used for this service.
          </p>
        </div>

        <button
          type="button"
          id="closeAddPartModal"
          class="shrink-0 w-9 h-9 rounded-full
                 flex items-center justify-center
                 text-gray-500 hover:text-gray-200
                 hover:bg-gray-800 transition"
        >
          <i class="fa-solid fa-xmark"></i>
        </button>

      </div>


      <form id="addPartForm">

        <!-- Part Name -->
        <div class="mb-4">

          <label
            for="partName"
            class="block text-sm font-medium
                   text-gray-300 mb-2"
          >
            Part Name
          </label>

          <input
            type="text"
            id="partName"
            maxlength="150"
            required
            placeholder="Enter part name"
            class="w-full bg-gray-800
                   border border-gray-700
                   rounded-xl px-4 py-3
                   text-gray-100
                   placeholder-gray-500
                   focus:outline-none
                   focus:border-violet-500"
          >

        </div>


        <!-- Price -->
        <div class="mb-4">

          <label
            for="partPrice"
            class="block text-sm font-medium
                   text-gray-300 mb-2"
          >
            Price per unit
          </label>

          <div class="relative">

            <span
              class="absolute left-4 top-1/2
                     -translate-y-1/2
                     text-gray-400"
            >
              ₹
            </span>

            <input
              type="number"
              id="partPrice"
              min="0.01"
              step="0.01"
              required
              placeholder="0.00"
              class="w-full bg-gray-800
                     border border-gray-700
                     rounded-xl pl-9 pr-4 py-3
                     text-gray-100
                     placeholder-gray-500
                     focus:outline-none
                     focus:border-violet-500"
            >

          </div>

        </div>


        <!-- Quantity -->
        <div class="mb-4">

          <label
            for="partQuantity"
            class="block text-sm font-medium
                   text-gray-300 mb-2"
          >
            Quantity
          </label>

          <input
            type="number"
            id="partQuantity"
            min="1"
            step="1"
            value="1"
            required
            class="w-full bg-gray-800
                   border border-gray-700
                   rounded-xl px-4 py-3
                   text-gray-100
                   focus:outline-none
                   focus:border-violet-500"
          >

        </div>


        <!-- Part Photo -->
        <div class="mb-4">

          <label
            class="block text-sm font-medium
                   text-gray-300 mb-2"
          >
            Part Photo
          </label>


          <!-- Hidden file input -->
          <input
            type="file"
            id="partPhoto"
            accept="image/*"
            class="hidden"
          >


          <div
            id="partPhotoArea"
            class="relative w-full h-44
                   rounded-xl
                   border border-dashed
                   border-gray-700
                   bg-gray-800/60
                   overflow-hidden"
          >

            <!-- Camera preview -->
            <video
              id="partCameraPreview"
              class="hidden absolute inset-0
                     w-full h-full
                     object-cover"
              autoplay
              playsinline
              muted
            ></video>


            <!-- Selected image -->
            <img
              id="partPhotoPreview"
              class="hidden absolute inset-0
                     w-full h-full
                     object-contain
                     bg-gray-900"
              alt="Part preview"
            >


            <!-- TWO PHOTO OPTIONS -->
            <div
              id="partPhotoOptions"
              class="absolute inset-0
                     flex items-center
                     justify-center
                     gap-4 px-4"
            >

              <!-- CAMERA -->
              <button
                type="button"
                id="takePartPhoto"
                class="flex-1 h-28
                       rounded-xl
                       bg-gray-800
                       border border-gray-700
                       hover:border-violet-500
                       hover:bg-gray-700
                       transition
                       flex flex-col
                       items-center
                       justify-center
                       text-gray-200"
              >

                <i
                  class="fa-solid fa-camera
                         text-2xl
                         text-violet-400
                         mb-2"
                ></i>

                <span class="text-sm font-medium">
                  Take Photo
                </span>

              </button>


              <!-- UPLOAD -->
              <button
                type="button"
                id="uploadPartPhoto"
                class="flex-1 h-28
                       rounded-xl
                       bg-gray-800
                       border border-gray-700
                       hover:border-violet-500
                       hover:bg-gray-700
                       transition
                       flex flex-col
                       items-center
                       justify-center
                       text-gray-200"
              >

                <i
                  class="fa-solid fa-image
                         text-2xl
                         text-violet-400
                         mb-2"
                ></i>

                <span class="text-sm font-medium">
                  Upload Photo
                </span>

              </button>

            </div>


            <!-- Camera capture button -->
            <button
              type="button"
              id="capturePartPhoto"
              class="hidden absolute
                     bottom-3 left-1/2
                     -translate-x-1/2
                     w-12 h-12
                     rounded-full
                     bg-white
                     border-4
                     border-gray-300
                     shadow-lg
                     hover:scale-105
                     transition"
              aria-label="Take photo"
            ></button>


            <!-- Cancel camera -->
            <button
              type="button"
              id="cancelCamera"
              class="hidden absolute
                     top-3 right-3
                     px-3 py-2
                     rounded-lg
                     bg-gray-900/80
                     border border-gray-700
                     text-sm text-white
                     hover:bg-gray-800
                     transition"
            >
              Cancel
            </button>


            <!-- Retake -->
            <button
              type="button"
              id="retakePartPhoto"
              class="hidden absolute
                     bottom-3 right-3
                     px-3 py-2
                     rounded-lg
                     bg-gray-900/80
                     border border-gray-700
                     text-sm text-white
                     hover:bg-gray-800
                     transition"
            >
              Retake
            </button>

          </div>


          <p class="text-xs text-gray-500 mt-2">
            Take a photo with the camera or choose an existing image.
          </p>

        </div>


        <!-- Error -->
        <p
          id="addPartError"
          class="text-sm text-red-400
                 mb-4 hidden"
        ></p>


        <!-- Buttons -->
        <div class="flex justify-end gap-3">

          <button
            type="button"
            id="cancelAddPart"
            class="px-5 py-2.5
                   rounded-xl
                   bg-gray-800
                   hover:bg-gray-700
                   border border-gray-700
                   text-gray-200
                   text-sm
                   font-medium
                   transition"
          >
            Cancel
          </button>


          <button
            type="submit"
            id="submitAddPart"
            class="px-5 py-2.5
                   rounded-xl
                   bg-violet-600
                   hover:bg-violet-500
                   text-white
                   text-sm
                   font-medium
                   transition"
          >
            Add Part
          </button>

        </div>

      </form>

    </div>
  `;

  document.body.appendChild(modal);

  // Elements

  const closeButton = document.getElementById("closeAddPartModal");

  const cancelButton = document.getElementById("cancelAddPart");

  const form = document.getElementById("addPartForm");

  const photoInput = document.getElementById("partPhoto");

  const photoOptions = document.getElementById("partPhotoOptions");

  const cameraPreview = document.getElementById("partCameraPreview");

  const photoPreview = document.getElementById("partPhotoPreview");

  const takePhotoButton = document.getElementById("takePartPhoto");

  const uploadPhotoButton = document.getElementById("uploadPartPhoto");

  const captureButton = document.getElementById("capturePartPhoto");

  const cancelCameraButton = document.getElementById("cancelCamera");

  const retakeButton = document.getElementById("retakePartPhoto");

  let cameraStream = null;

  // Camera

  const stopCamera = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((track) => track.stop());

      cameraStream = null;
    }

    cameraPreview.srcObject = null;
  };

  // Close modal

  const closeModal = () => {
    stopCamera();

    modal.remove();
  };

  closeButton.addEventListener("click", closeModal);

  cancelButton.addEventListener("click", closeModal);

  // Error helpers

  const showError = (message) => {
    const errorElement = document.getElementById("addPartError");

    errorElement.textContent = message;

    errorElement.classList.remove("hidden");
  };

  const clearError = () => {
    const errorElement = document.getElementById("addPartError");

    errorElement.textContent = "";

    errorElement.classList.add("hidden");
  };

  // Show selected photo

  const showPhotoPreview = (file) => {
    const imageUrl = URL.createObjectURL(file);

    photoPreview.src = imageUrl;

    photoPreview.classList.remove("hidden");

    cameraPreview.classList.add("hidden");

    photoOptions.classList.add("hidden");

    captureButton.classList.add("hidden");

    cancelCameraButton.classList.add("hidden");

    retakeButton.classList.remove("hidden");

    stopCamera();

    photoPreview.onload = () => {
      URL.revokeObjectURL(imageUrl);
    };
  };

  // Start camera

  const startCamera = async () => {
    clearError();

    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        showError(
          "Camera access is not supported here. You can use Upload Photo instead.",
        );

        return;
      }

      stopCamera();

      cameraStream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: {
            ideal: "environment",
          },
        },
        audio: false,
      });

      cameraPreview.srcObject = cameraStream;

      photoOptions.classList.add("hidden");

      photoPreview.classList.add("hidden");

      cameraPreview.classList.remove("hidden");

      captureButton.classList.remove("hidden");

      cancelCameraButton.classList.remove("hidden");

      retakeButton.classList.add("hidden");
    } catch (error) {
      console.error("Unable to access camera:", error);

      if (error.name === "NotAllowedError") {
        showError(
          "Camera permission was denied. Please allow camera access or use Upload Photo.",
        );
      } else if (error.name === "NotFoundError") {
        showError("No camera was found. Please use Upload Photo instead.");
      } else {
        showError(
          "Unable to access the camera. Please use Upload Photo instead.",
        );
      }
    }
  };

  // Capture camera photo

  const capturePhoto = () => {
    if (!cameraStream || !cameraPreview.videoWidth) {
      showError("Camera is not ready yet. Please try again.");

      return;
    }

    const canvas = document.createElement("canvas");

    canvas.width = cameraPreview.videoWidth;

    canvas.height = cameraPreview.videoHeight;

    const context = canvas.getContext("2d");

    if (!context) {
      showError("Unable to capture the photo.");

      return;
    }

    context.drawImage(cameraPreview, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(
      (blob) => {
        if (!blob) {
          showError("Unable to create the photo.");

          return;
        }

        const file = new File([blob], `part-${Date.now()}.jpg`, {
          type: "image/jpeg",
        });

        /*
         * Put the camera photo into
         * the hidden file input so
         * the existing multipart
         * submission can use it.
         */

        const dataTransfer = new DataTransfer();

        dataTransfer.items.add(file);

        photoInput.files = dataTransfer.files;

        showPhotoPreview(file);
      },
      "image/jpeg",
      0.9,
    );
  };

  // Take Photo button

  takePhotoButton.addEventListener("click", startCamera);

  // Upload Photo button

  uploadPhotoButton.addEventListener("click", () => {
    clearError();

    stopCamera();

    photoInput.click();
  });

  // Uploaded image selected

  photoInput.addEventListener("change", () => {
    const file = photoInput.files[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      photoInput.value = "";

      showError("Only image files are allowed.");

      return;
    }

    clearError();

    showPhotoPreview(file);
  });

  // Capture button

  captureButton.addEventListener("click", capturePhoto);

  // Cancel camera

  cancelCameraButton.addEventListener("click", () => {
    stopCamera();

    cameraPreview.classList.add("hidden");

    captureButton.classList.add("hidden");

    cancelCameraButton.classList.add("hidden");

    photoOptions.classList.remove("hidden");
  });

  // Retake

  retakeButton.addEventListener("click", () => {
    photoInput.value = "";

    photoPreview.classList.add("hidden");

    photoPreview.removeAttribute("src");

    retakeButton.classList.add("hidden");

    startCamera();
  });

  // Submit

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    await submitAddPart(bookingId, modal);
  });
}

async function submitAddPart(bookingId, modal) {
  const partNameInput = document.getElementById("partName");

  const priceInput = document.getElementById("partPrice");

  const quantityInput = document.getElementById("partQuantity");

  const photoInput = document.getElementById("partPhoto");

  const errorElement = document.getElementById("addPartError");

  const submitButton = document.getElementById("submitAddPart");

  const partName = partNameInput.value.trim();

  const price = priceInput.value;

  const quantity = quantityInput.value;

  const photo = photoInput.files[0];

  errorElement.classList.add("hidden");

  errorElement.textContent = "";

  if (!partName) {
    errorElement.textContent = "Please enter the part name.";

    errorElement.classList.remove("hidden");

    return;
  }

  if (!price || Number(price) <= 0) {
    errorElement.textContent = "Please enter a valid price.";

    errorElement.classList.remove("hidden");

    return;
  }

  if (!quantity || Number(quantity) < 1) {
    errorElement.textContent = "Please enter a valid quantity.";

    errorElement.classList.remove("hidden");

    return;
  }

  if (!photo) {
    errorElement.textContent =
      "Please take a photo or upload a photo of the part.";

    errorElement.classList.remove("hidden");

    return;
  }

  if (!photo.type.startsWith("image/")) {
    errorElement.textContent = "Only image files are allowed.";

    errorElement.classList.remove("hidden");

    return;
  }

  const restore = submitButton && window.SERVICEHUB_BUTTON
    ? window.SERVICEHUB_BUTTON.setLoading(submitButton, "Adding...")
    : null;
  if (submitButton && window.SERVICEHUB_BUTTON && !restore) return;

  try {
    if (!restore && submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = "Adding...";
    }

    const formData = new FormData();

    formData.append("partName", partName);

    formData.append("price", price);

    formData.append("quantity", quantity);

    formData.append("photo", photo);

    await workerApiRequest(`${API_BASE_URL}/bookings/${bookingId}/parts`, {
      method: "POST",
      body: formData,
    });

    modal.remove();

    await loadWorkerBookingParts(bookingId);
  } catch (error) {
    console.error("Failed to add part:", error);

    errorElement.textContent = error.message || "Unable to add part.";

    errorElement.classList.remove("hidden");
  } finally {
    if (restore) {
      restore();
    } else if (submitButton) {
      submitButton.disabled = false;
      submitButton.textContent = "Add Part";
    }
  }
}

window.addEventListener("beforeunload", () => {
  stopBookingRequestPolling();
  stopWorkerPartsPolling();
  stopWorkerBookingPolling();
  stopWorkerLocationTracking();
  for (const countdown of razorpayConfirmationCountdowns.values()) {
    if (countdown.timerId !== null) {
      clearInterval(countdown.timerId);
    }
  }
  razorpayConfirmationCountdowns.clear();
});

// Dashboard Initialisation

const availabilityToggleButton = document.getElementById(
  "availabilityToggleButton",
);

if (availabilityToggleButton) {
  availabilityToggleButton.addEventListener(
    "click",
    toggleWorkerAvailability,
  );
}

if (checkWorkerLogin()) {
  loadWorkerInformation();

  loadWorkerStatus();

  loadBookingRequests();
  startBookingRequestPolling();

  loadWorkerBookings();
  startWorkerBookingPolling();
}
