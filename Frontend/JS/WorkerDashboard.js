const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";
const USER_NAME_KEY = "servicehub_user_name";

const API_BASE_URL = "http://localhost:8080/api/worker";


// Authentication

function checkWorkerLogin() {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const role =
        localStorage.getItem(ROLE_KEY);
    if (!token || role !== "WORKER") {

        window.location.href =
            "../HTML/login.html";

        return false;
    }

    return true;
}


// Worker Information

async function loadWorkerInformation() {

    try {

        const profile =
            await workerApiRequest(
                `${API_BASE_URL}/profile`
            );

        const workerName =
            profile?.fullName || "Worker";

        localStorage.setItem(
            USER_NAME_KEY,
            workerName
        );

        const welcomeMessage =
            document.getElementById(
                "welcomeMessage"
            );

        if (welcomeMessage) {

            welcomeMessage.textContent =
                `Welcome back, ${workerName}!`;
        }

    } catch (error) {

        console.error(
            "Failed to load worker profile:",
            error
        );

        const workerName =
            localStorage.getItem(
                USER_NAME_KEY
            ) || "Worker";

        const welcomeMessage =
            document.getElementById(
                "welcomeMessage"
            );

        if (welcomeMessage) {

            welcomeMessage.textContent =
                `Welcome back, ${workerName}!`;
        }
    }
}


// Worker Status

async function loadWorkerStatus() {

    try {

        const status =
            await workerApiRequest(
                `${API_BASE_URL}/status`
            );

        renderWorkerStatus(status);

    } catch (error) {

        console.error(
            "Failed to load worker status:",
            error
        );

        const statusElement =
            document.getElementById(
                "availabilityStatus"
            );

        const badge =
            document.getElementById(
                "availabilityBadge"
            );

        if (statusElement) {
            statusElement.textContent =
                "Unable to load";
        }

        if (badge) {
            badge.textContent =
                "Error";

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

    const verificationStatusElement =
        document.getElementById(
            "verificationStatus"
        );

    const verificationBadge =
        document.getElementById(
            "verificationBadge"
        );

    const statusElement =
        document.getElementById(
            "availabilityStatus"
        );

    const badge =
        document.getElementById(
            "availabilityBadge"
        );

    const iconContainer =
        document.getElementById(
            "availabilityIcon"
        );

    if (!statusElement ||
        !badge ||
        !iconContainer) {

        return;
    }

    if (status.verificationStatus) {

        const verificationLabel =
            status.verificationStatus
                .replace(/_/g, " ")
                .toLowerCase()
                .replace(/^\w/, character =>
                    character.toUpperCase()
                );

        if (verificationStatusElement) {
            verificationStatusElement.textContent =
                verificationLabel;
        }

        if (verificationBadge) {
            verificationBadge.textContent =
                verificationLabel;
        }
    }

    /*
     * Availability only applies to verified workers.
     * A worker who is not verified cannot be considered available.
     */

    if (status.verificationStatus !== "VERIFIED") {

        statusElement.textContent =
            "Not Available";

        badge.textContent =
            "Not Available";

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

    const availability =
        status.availabilityStatus;

    // AVAILABLE

    if (availability === "AVAILABLE") {

        statusElement.textContent =
            "Available";

        badge.textContent =
            "Available";

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

        statusElement.textContent =
            "Unavailable";

        badge.textContent =
            "Unavailable";

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

        statusElement.textContent =
            "Busy";

        badge.textContent =
            "Busy";

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

    statusElement.textContent =
        availability || "Unknown";

    badge.textContent =
        "Unknown";

}


// API Helper

async function workerApiRequest(url, options = {}) {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const response =
        await fetch(url, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
                ...(options.headers || {})
            }
        });


    if (response.status === 401) {

    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);

    window.location.href =
        "../HTML/login.html";

    throw new Error("Unauthorized");
}


    if (!response.ok) {

        let message =
            "Something went wrong.";

        try {

            const data =
                await response.json();

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


// Booking Requests

async function loadBookingRequests() {

    const container =
        document.getElementById(
            "bookingRequestsContainer"
        );

    if (!container) {
        return;
    }


    try {

        const requests =
            await workerApiRequest(
                `${API_BASE_URL}/bookings/requests`
            );

        renderBookingRequests(requests);

    } catch (error) {

        console.error(
            "Failed to load booking requests:",
            error
        );


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
                            flex items-center justify-center">

                    <i class="fa-solid
                              fa-triangle-exclamation
                              text-red-400
                              text-2xl"></i>

                </div>


                <h4 class="text-lg font-semibold mb-2">
                    Unable to load booking requests
                </h4>


                <p class="text-gray-400 text-sm mb-5">
                    ${escapeHtml(error.message)}
                </p>


                <button
                    id="retryBookingRequests"
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


        const retryButton =
            document.getElementById(
                "retryBookingRequests"
            );


        if (retryButton) {

            retryButton.addEventListener(
                "click",
                loadBookingRequests
            );
        }
    }
}


// Render Booking Requests

function renderBookingRequests(requests) {

    const container =
        document.getElementById(
            "bookingRequestsContainer"
        );

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


    container.innerHTML =
        requests.map(request => `

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
            request.serviceName ||
            "Service"
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
            request.customerName ||
            "Customer"
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
                                                request.customerCity
                                            ]
                                            .filter(Boolean)
                                            .map(part => escapeHtml(part))
                                            .join(", ")
                                            ||
                                            "Address not provided"
                                        }

                                    </p>

                                </div>

                            </div>

                        </div>


                        <!-- Note -->

                        ${request.customerNote
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

                                        ${escapeHtml(
                    request.customerNote
                )}

                                    </div>

                                </div>

                            `
                : ""
            }


                        <!-- Requested Time -->

                        <p class="text-xs text-gray-500">

                            Requested
                            ${formatDateTime(
                request.createdAt
            )}

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

        `).join("");
}


// Accept Booking

async function acceptBooking(bookingId) {

    const confirmed =
        confirm(
            "Are you sure you want to accept this booking?"
        );

    if (!confirmed) {
        return;
    }


    try {

        await workerApiRequest(
            `${API_BASE_URL}/bookings/${bookingId}/accept`,
            {
                method: "POST"
            }
        );


        /*
         * Refresh both sections.
         *
         * The accepted booking disappears
         * from requests and appears in Active Service.
         */

        await Promise.all([
            loadBookingRequests(),
            loadWorkerBookings()
        ]);


    } catch (error) {

        alert(error.message);

    }
}


// Reject Booking

async function rejectBooking(bookingId) {

    const confirmed =
        confirm(
            "Are you sure you want to reject this booking?"
        );

    if (!confirmed) {
        return;
    }


    try {

        await workerApiRequest(
            `${API_BASE_URL}/bookings/${bookingId}/reject`,
            {
                method: "POST"
            }
        );


        /*
         * Rejecting does not change the booking status.
         * Therefore the request can still appear again.
         *
         * We simply refresh the list.
         */

        await loadBookingRequests();


    } catch (error) {

        alert(error.message);

    }
}


// Active Service

async function loadWorkerBookings() {

    const container =
        document.getElementById(
            "activeServiceContainer"
        );

    if (!container) {
        return;
    }


    try {

        const bookings =
            await workerApiRequest(
                `${API_BASE_URL}/bookings`
            );

        renderActiveService(bookings);

    } catch (error) {

        console.error(
            "Failed to load worker bookings:",
            error
        );


        container.innerHTML = `

            <div class="text-center">

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

                    Unable to load active service

                </h4>


                <p class="text-gray-400
                          text-sm
                          mb-5">

                    ${escapeHtml(error.message)}

                </p>


                <button
                    id="retryWorkerBookings"
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


        const retryButton =
            document.getElementById(
                "retryWorkerBookings"
            );


        if (retryButton) {

            retryButton.addEventListener(
                "click",
                loadWorkerBookings
            );
        }
    }
}


// Render Active Service

function renderActiveService(bookings) {

    const container =
        document.getElementById(
            "activeServiceContainer"
        );

    if (!container) {
        return;
    }


    const activeStatuses = [
        "ACCEPTED",
        "IN_PROGRESS"
    ];


    const activeBooking =
        bookings.find(
            booking =>
                activeStatuses.includes(
                    booking.status
                )
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


    const statusInfo =
        getBookingStatusInfo(
            activeBooking.status
        );


    let actionButton = "";


    if (activeBooking.status === "ACCEPTED") {

        actionButton = `

            <div class="flex flex-wrap items-center gap-3">

                <button
                    class="start-service-btn
                           px-6 py-3
                           rounded-xl
                           bg-violet-600
                           hover:bg-violet-500
                           text-white
                           font-medium
                           transition"
                    data-booking-id="${activeBooking.bookingId}">

                    <i class="fa-solid fa-play mr-2"></i>

                    Start Service

                </button>

                <button
                    class="cancel-service-btn
                           px-6 py-3
                           rounded-xl
                           bg-gray-800
                           hover:bg-gray-700
                           border border-gray-700
                           text-gray-200
                           font-medium
                           transition"
                    data-booking-id="${activeBooking.bookingId}"
                    data-booking-status="ACCEPTED">

                    <i class="fa-solid fa-xmark mr-2"></i>

                    Cancel

                </button>

            </div>

        `;
    }


    if (activeBooking.status === "IN_PROGRESS") {

        if (activeBooking.workerConfirmedCompletion) {

            actionButton = `

                <div class="text-sm
                            text-yellow-400">

                    <i class="fa-solid
                              fa-clock
                              mr-2"></i>

                    Waiting for customer confirmation

                </div>

            `;

        } else {

            actionButton = `

                <div class="flex flex-wrap items-center gap-3">

                    <button
                        class="complete-service-btn
                               px-6 py-3
                               rounded-xl
                               bg-green-600
                               hover:bg-green-500
                               text-white
                               font-medium
                               transition"
                        data-booking-id="${activeBooking.bookingId}">

                        <i class="fa-solid fa-circle-check mr-2"></i>

                        Mark Service Completed

                    </button>

                    <button
                        class="cancel-service-btn
                               px-6 py-3
                               rounded-xl
                               bg-gray-800
                               hover:bg-gray-700
                               border border-gray-700
                               text-gray-200
                               font-medium
                               transition"
                        data-booking-id="${activeBooking.bookingId}"
                        data-booking-status="IN_PROGRESS">

                        <i class="fa-solid fa-xmark mr-2"></i>

                        Cancel

                    </button>

                </div>

            `;
        }
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

        activeBooking.customerPinCode

    ].filter(Boolean);


    const customerAddress =
        addressParts.length > 0
            ? addressParts
                .map(part => escapeHtml(part))
                .join(", ")
            : "Address not provided";


    const customerPhone =
        activeBooking.customerPhone
            ? escapeHtml(activeBooking.customerPhone)
            : "Phone number not provided";


    const callButton =
        activeBooking.customerPhone
            ? `

                <a
                    href="tel:${encodeURIComponent(
                        activeBooking.customerPhone
                    )}"
                    class="inline-flex
                           items-center
                           justify-center
                           gap-2
                           px-4 py-2.5
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

                        ${escapeHtml(
                            activeBooking.customerName ||
                            "Customer"
                        )}

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

                        ${escapeHtml(
                            activeBooking.serviceName ||
                            "Service"
                        )}

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

                        ${formatDateTime(
                            activeBooking.createdAt
                        )}

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
                                activeBooking.customerName ||
                                "Customer"
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

            ${activeBooking.customerNote
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

                            ${escapeHtml(
                                activeBooking.customerNote
                            )}

                        </div>

                    </div>

                  `
                : ""
            }



            <!-- Actions -->

            <div class="flex flex-wrap
                        items-center
                        gap-4">

                ${callButton}

                ${actionButton}

            </div>

        </div>

    `;
}

// Worker Cancellation

const ACCEPTED_CANCELLATION_REASONS = [
    { value: "EMERGENCY_PERSONAL_ISSUE", label: "Emergency / personal issue" },
    { value: "VEHICLE_TRANSPORT_PROBLEM", label: "Vehicle / transport problem" },
    { value: "UNABLE_TO_REACH_CUSTOMER", label: "Unable to reach customer" },
    { value: "INCORRECT_BOOKING_SERVICE_INFORMATION", label: "Incorrect booking / service information" },
    { value: "OTHER", label: "Other" }
];

const IN_PROGRESS_CANCELLATION_REASONS = [
    { value: "CANNOT_SOLVE_PROBLEM", label: "Cannot solve the problem" },
    { value: "REQUIRES_DIFFERENT_EXPERTISE", label: "Requires different expertise" },
    { value: "REQUIRED_EQUIPMENT_UNAVAILABLE", label: "Required equipment unavailable" },
    { value: "REQUIRED_PART_MATERIAL_UNAVAILABLE", label: "Required part / material unavailable" },
    { value: "OTHER", label: "Other" }
];

function showWorkerCancellationDialog(bookingId, bookingStatus) {

    return new Promise(resolve => {

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
                            ${bookingStatus === "ACCEPTED"
                                ? "Please select a reason for cancelling this booking."
                                : "Please tell us why you cannot complete this service."}
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
                        ${reasons.map(reason => `<option value="${reason.value}">${reason.label}</option>`).join("")}
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

        const reasonSelect =
            document.getElementById("workerCancellationReason");
        const messageContainer =
            document.getElementById("workerCancellationMessageContainer");
        const messageInput =
            document.getElementById("workerCancellationMessage");
        const errorElement =
            document.getElementById("workerCancellationError");

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

        document.getElementById("closeWorkerCancellationModal")
            .addEventListener("click", () => closeModal(null));
        document.getElementById("cancelWorkerCancellation")
            .addEventListener("click", () => closeModal(null));
        document.getElementById("confirmWorkerCancellation")
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

async function cancelWorkerBooking(bookingId, bookingStatus) {

    const cancellation =
        await showWorkerCancellationDialog(bookingId, bookingStatus);

    if (!cancellation) {
        return;
    }

    if (!confirm("Are you sure you want to cancel this booking?")) {
        return;
    }

    try {
        await workerApiRequest(
            `${API_BASE_URL}/bookings/${bookingId}/cancel`,
            {
                method: "POST",
                body: JSON.stringify({
                    reason: cancellation.reason,
                    message: cancellation.message
                })
            }
        );

        await Promise.all([
            loadWorkerBookings(),
            loadWorkerStatus()
        ]);
    } catch (error) {
        alert(error.message);
    }
}

// Start Service

async function startService(bookingId) {

    const confirmed =
        confirm(
            "Are you sure you want to start this service?"
        );

    if (!confirmed) {
        return;
    }


    try {

        await workerApiRequest(
            `${API_BASE_URL}/bookings/${bookingId}/start`,
            {
                method: "POST"
            }
        );


        await loadWorkerBookings();


    } catch (error) {

        alert(error.message);

    }
}


// Complete Service

async function completeService(bookingId) {

    const confirmed =
        confirm(
            "Have you finished providing this service?"
        );

    if (!confirmed) {
        return;
    }


    try {

        await workerApiRequest(
            `${API_BASE_URL}/bookings/${bookingId}/complete`,
            {
                method: "POST"
            }
        );


        await loadWorkerBookings();


    } catch (error) {

        alert(error.message);

    }
}


// Event Delegation

document.addEventListener("click", event => {

    const acceptButton =
        event.target.closest(
            ".accept-booking-btn"
        );


    if (acceptButton) {

        acceptBooking(
            acceptButton.dataset.bookingId
        );

        return;
    }


    const rejectButton =
        event.target.closest(
            ".reject-booking-btn"
        );


    if (rejectButton) {

        rejectBooking(
            rejectButton.dataset.bookingId
        );

        return;
    }


    const startButton =
        event.target.closest(
            ".start-service-btn"
        );


    if (startButton) {

        startService(
            startButton.dataset.bookingId
        );

        return;
    }


    const completeButton =
        event.target.closest(
            ".complete-service-btn"
        );


    if (completeButton) {

        completeService(
            completeButton.dataset.bookingId
        );

        return;
    }


    const cancelButton =
        event.target.closest(
            ".cancel-service-btn"
        );


    if (cancelButton) {

        cancelWorkerBooking(
            cancelButton.dataset.bookingId,
            cancelButton.dataset.bookingStatus
        );

    }

});


// Helpers

function getBookingStatusInfo(status) {

    const statuses = {

        ACCEPTED: {

            label: "Accepted",

            classes:
                "bg-blue-900/30 " +
                "text-blue-400 " +
                "border border-blue-700/30"
        },


        IN_PROGRESS: {

            label: "In Progress",

            classes:
                "bg-yellow-900/30 " +
                "text-yellow-400 " +
                "border border-yellow-700/30"
        }

    };


    return statuses[status] || {

        label: status || "Unknown",

        classes:
            "bg-gray-800 " +
            "text-gray-400 " +
            "border border-gray-700"
    };
}


function formatDateTime(dateString) {

    if (!dateString) {
        return "—";
    }


    const date =
        new Date(dateString);


    if (Number.isNaN(date.getTime())) {
        return "—";
    }


    return date.toLocaleString(
        undefined,
        {
            dateStyle: "medium",
            timeStyle: "short"
        }
    );
}


function escapeHtml(value) {

    if (value === null ||
        value === undefined) {

        return "";
    }


    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


// Dashboard Initialisation

if (checkWorkerLogin()) {

    loadWorkerInformation();

    loadWorkerStatus();

    loadBookingRequests();

    loadWorkerBookings();

}