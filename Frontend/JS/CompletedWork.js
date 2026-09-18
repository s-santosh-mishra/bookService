const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";

const API_BASE_URL = "http://localhost:8080/api/worker";


// Authentication

function checkWorkerLogin() {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const role =
        localStorage.getItem(ROLE_KEY);

    if (!token || role !== "WORKER") {

        window.location.href =
            "/bookService/Frontend/HTML/login.html";

        return false;
    }

    return true;
}


// API Helper

async function workerApiRequest(url, options = {}) {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const response =
        await fetch(url, {

            ...options,

            headers: {

                "Content-Type":
                    "application/json",

                "Authorization":
                    `Bearer ${token}`,

                ...(options.headers || {})

            }

        });


    /*
     * Only clear authentication when the token
     * itself is invalid or expired.
     */

    if (response.status === 401) {

        localStorage.removeItem(
            ACCESS_TOKEN_KEY
        );

        localStorage.removeItem(
            ROLE_KEY
        );

        window.location.href =
            "/bookService/Frontend/HTML/login.html";

        throw new Error("Unauthorized");
    }


    if (!response.ok) {

        let message =
            "Something went wrong.";

        try {

            const data =
                await response.json();

            if (data.message) {

                message =
                    data.message;
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

    const container =
        document.getElementById(
            "completedWorkContainer"
        );


    if (!container) {

        return;
    }


    try {

        const bookings =
            await workerApiRequest(
                `${API_BASE_URL}/bookings`
            );


        const completedBookings =
            (bookings || [])
                .filter(
                    booking =>
                        booking.status === "COMPLETED"
                )
                .sort(
                    (a, b) =>
                        new Date(b.completedAt || b.updatedAt)
                        -
                        new Date(a.completedAt || a.updatedAt)
                );


        renderCompletedWork(
            completedBookings
        );


    } catch (error) {

        console.error(
            "Failed to load completed work:",
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


        const retryButton =
            document.getElementById(
                "retryCompletedWork"
            );


        if (retryButton) {

            retryButton.addEventListener(
                "click",
                loadCompletedWork
            );
        }
    }
}


// Render Completed Work

function renderCompletedWork(bookings) {

    const container =
        document.getElementById(
            "completedWorkContainer"
        );


    if (!container) {

        return;
    }


    if (!bookings ||
        bookings.length === 0) {

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


    container.innerHTML =
        bookings.map(
            booking =>
                createCompletedBookingCard(
                    booking
                )
        ).join("");
}


// Completed Booking Card

function createCompletedBookingCard(booking) {

    const customerName =
        escapeHtml(
            booking.customerName ||
            "Customer"
        );

    const serviceName =
        escapeHtml(
            booking.serviceName ||
            "Service"
        );

    return `

        <article
            class="bg-gray-900
                   border border-gray-800
                   rounded-2xl
                   p-5 md:p-6">

            <!-- Header -->

            <div class="flex items-start
                        justify-between
                        gap-4
                        mb-5">

                <div>

                    <p class="text-xs
                              text-gray-500
                              mb-1">

                        Completed Service

                    </p>

                    <h3 class="text-lg
                               md:text-xl
                               font-semibold">

                        ${serviceName}

                    </h3>

                </div>


                <span
                    class="inline-flex
                           items-center
                           px-3
                           py-1
                           rounded-full
                           text-xs
                           font-medium
                           bg-green-900/30
                           text-green-400
                           border border-green-700/30">

                    <i class="fa-solid
                              fa-check
                              mr-1.5"></i>

                    Completed

                </span>

            </div>


            <!-- Customer -->

            <div class="mb-5">

                <p class="text-xs
                          text-gray-500
                          mb-1">

                    Customer

                </p>

                <p class="font-medium">

                    ${customerName}

                </p>

            </div>


            <!-- Booking Times -->

            <div class="grid grid-cols-2
                        gap-4
                        mb-5">

                <div>

                    <p class="text-xs
                              text-gray-500
                              mb-1">

                        Booked

                    </p>

                    <p class="text-sm
                              text-gray-300">

                        ${formatDateTime(
                            booking.createdAt
                        )}

                    </p>

                </div>


                <div>

                    <p class="text-xs
                              text-gray-500
                              mb-1">

                        Completed

                    </p>

                    <p class="text-sm
                              text-gray-300">

                        ${formatDateTime(
                            booking.completedAt
                        )}

                    </p>

                </div>

            </div>


            <!-- Payment -->

            <div class="pt-4
                        border-t
                        border-gray-800">

                <div class="flex items-center
                            justify-between
                            gap-4">

                    <div class="flex items-center
                                gap-2">

                        <i class="fa-solid
                                  fa-credit-card
                                  text-gray-500"></i>

                        <span class="text-sm
                                     text-gray-400">

                            Payment

                        </span>

                    </div>


                    <span class="text-sm
                                 text-gray-500">

                        Not available yet

                    </span>

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

        booking.customerPinCode

    ].filter(Boolean);


    if (addressParts.length === 0) {

        return "Address not provided";
    }


    return addressParts
        .map(part =>
            escapeHtml(part)
        )
        .join(", ");
}


// Date Formatting

function formatDateTime(dateString) {

    if (!dateString) {

        return "—";
    }


    const date =
        new Date(dateString);


    if (Number.isNaN(
        date.getTime()
    )) {

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


// HTML Escape

function escapeHtml(value) {

    if (value === null ||
        value === undefined) {

        return "";
    }


    return String(value)

        .replace(
            /&/g,
            "&amp;"
        )

        .replace(
            /</g,
            "&lt;"
        )

        .replace(
            />/g,
            "&gt;"
        )

        .replace(
            /"/g,
            "&quot;"
        )

        .replace(
            /'/g,
            "&#039;"
        );
}


// Initialisation

if (checkWorkerLogin()) {

    loadCompletedWork();

}