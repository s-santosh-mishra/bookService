const API_BASE_URL = "http://localhost:8080/api/admin/workers";
const WORKER_ACCESS_TOKEN_KEY = "servicehub_access_token";

const tabs = document.querySelectorAll(".worker-tab");
const tableBody = document.getElementById("workerTableBody");
const workerCount = document.getElementById("workerCount");
const workerListTitle = document.getElementById("workerListTitle");
const workerListDescription = document.getElementById("workerListDescription");

const workerDetailsModal = document.getElementById("workerDetailsModal");
const workerDetailsOverlay = document.getElementById("workerDetailsOverlay");
const workerDetailsContent = document.getElementById("workerDetailsContent");
const closeWorkerDetails = document.getElementById("closeWorkerDetails");

const statusEndpoints = {
    Pending: "pending",
    Verified: "verified",
    Rejected: "rejected",
    Suspended: "suspended"
};

let currentStatus = "Pending";
let loadRequestId = 0;


async function loadWorkers(status) {

    currentStatus = status;

    const requestId = ++loadRequestId;
    const endpoint = statusEndpoints[status];
    const token = sessionStorage.getItem(WORKER_ACCESS_TOKEN_KEY);

    tableBody.innerHTML = `
        <tr>
            <td colspan="5" class="px-6 py-12 text-center text-gray-500">
                Loading workers...
            </td>
        </tr>
    `;

    try {

        const response = await fetch(`${API_BASE_URL}/${endpoint}`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {

            if (response.status === 401 || response.status === 403) {
                window.location.href = "AdminLogin.html";
                return;
            }

            throw new Error("Failed to load workers.");
        }

        const workers = await response.json();

        if (requestId !== loadRequestId) {
            return;
        }

        workerCount.textContent = workers.length;

        renderWorkers(workers, status);

    } catch (error) {

        if (requestId !== loadRequestId) {
            return;
        }

        console.error(error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-12 text-center text-red-400">
                    Failed to load workers.
                </td>
            </tr>
        `;
    }
}


function renderWorkers(workers, status) {

    if (workers.length === 0) {

        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-16 text-center">

                    <div class="flex flex-col items-center">

                        <div class="w-12 h-12 rounded-full bg-gray-800
                                    flex items-center justify-center mb-4">

                            <i class="fa-solid fa-users
                                      text-gray-600 text-lg"></i>

                        </div>

                        <p class="text-gray-400">
                            No ${status.toLowerCase()} workers
                        </p>

                        <p class="text-gray-600 text-sm mt-1">
                            There are currently no workers in this category.
                        </p>

                    </div>

                </td>
            </tr>
        `;

        return;
    }


    tableBody.innerHTML = workers.map(worker => `
        <tr class="border-b border-gray-800 last:border-0
                   hover:bg-gray-800/40 transition-colors">

            <td class="px-6 py-4">

                <div class="flex items-center gap-3">

                    <div class="w-10 h-10 rounded-full bg-violet-600/15
                                flex items-center justify-center
                                text-violet-400">

                        <i class="fa-solid fa-user"></i>

                    </div>

                    <div>

                        <p class="font-medium text-white">
                            ${escapeHtml(worker.fullName)}
                        </p>

                        <p class="text-xs text-gray-500">
                            ${escapeHtml(worker.workerId)}
                        </p>

                    </div>

                </div>

            </td>


            <td class="px-6 py-4">

                <p class="text-sm text-gray-300">
                    ${escapeHtml(worker.email)}
                </p>

                <p class="text-xs text-gray-500 mt-1">
                    ${escapeHtml(worker.phone)}
                </p>

            </td>


            <td class="px-6 py-4">

                <span class="text-sm text-gray-300">
                    ${worker.experienceYears} years
                </span>

            </td>


            <td class="px-6 py-4">

                <span class="inline-flex px-2.5 py-1 rounded-full
                             text-xs font-medium
                             ${getStatusClass(worker.verificationStatus)}">

                    ${formatStatus(worker.verificationStatus)}

                </span>

            </td>


            <td class="px-6 py-4 text-right">

                <button
                    class="view-worker-button px-3 py-2 rounded-lg
                           text-sm text-violet-400
                           hover:bg-violet-600/10
                           transition-colors"
                    data-worker-id="${escapeHtml(worker.workerId)}">

                    View

                </button>

            </td>

        </tr>
    `).join("");
}


function formatStatus(status) {

    if (!status) {
        return "Unknown";
    }

    return status.charAt(0) + status.slice(1).toLowerCase();
}


function getStatusClass(status) {

    switch (status) {

        case "PENDING":
            return "bg-yellow-500/10 text-yellow-400";

        case "VERIFIED":
            return "bg-green-500/10 text-green-400";

        case "REJECTED":
            return "bg-red-500/10 text-red-400";

        case "SUSPENDED":
            return "bg-gray-500/10 text-gray-400";

        default:
            return "bg-gray-500/10 text-gray-400";
    }
}


function escapeHtml(value) {

    if (value === null || value === undefined) {
        return "";
    }

    const div = document.createElement("div");

    div.textContent = value;

    return div.innerHTML;
}


tabs.forEach(tab => {

    tab.addEventListener("click", () => {

        tabs.forEach(item => {

            item.classList.remove(
                "active",
                "bg-violet-600",
                "text-white"
            );

            item.classList.add("text-gray-400");

        });


        tab.classList.add(
            "active",
            "bg-violet-600",
            "text-white"
        );

        tab.classList.remove("text-gray-400");


        const status = tab.textContent.trim();

        workerListTitle.textContent = `${status} Workers`;

        workerListDescription.textContent =
            getDescription(status);

        loadWorkers(status);

    });

});


function getDescription(status) {

    switch (status) {

        case "Pending":
            return "Workers waiting for verification.";

        case "Verified":
            return "Workers currently verified.";

        case "Rejected":
            return "Workers whose registration was rejected.";

        case "Suspended":
            return "Workers currently suspended.";

        default:
            return "";
    }
}


tableBody.addEventListener("click", event => {

    const button = event.target.closest(".view-worker-button");

    if (!button) {
        return;
    }

    const workerId = button.dataset.workerId;

    loadWorkerDetails(workerId);

});


async function loadWorkerDetails(workerId) {

    const token = sessionStorage.getItem(WORKER_ACCESS_TOKEN_KEY);

    workerDetailsModal.classList.remove("hidden");

    workerDetailsContent.innerHTML = `
        <div class="flex flex-col items-center justify-center py-12">

            <i class="fa-solid fa-spinner fa-spin
                      text-2xl text-violet-400 mb-4"></i>

            <p class="text-gray-400">
                Loading worker details...
            </p>

        </div>
    `;

    try {

        const response = await fetch(`${API_BASE_URL}/${workerId}`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {

            if (response.status === 401 || response.status === 403) {
                window.location.href = "AdminLogin.html";
                return;
            }

            throw new Error("Failed to load worker details.");
        }

        const worker = await response.json();

        renderWorkerDetails(worker);

    } catch (error) {

        console.error(error);

        workerDetailsContent.innerHTML = `
            <div class="text-center py-12">

                <i class="fa-solid fa-circle-exclamation
                          text-2xl text-red-400 mb-4"></i>

                <p class="text-red-400">
                    Failed to load worker details.
                </p>

            </div>
        `;
    }
}


function renderWorkerDetails(worker) {

    workerDetailsContent.innerHTML = `

        <div class="space-y-8">

            <div>

                <h3 class="text-lg font-semibold mb-4">
                    Basic Information
                </h3>

                <div class="grid gap-4 sm:grid-cols-2">

                    <div>
                        <p class="text-xs text-gray-500">
                            Full Name
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.fullName)}
                        </p>
                    </div>

                    <div>
                        <p class="text-xs text-gray-500">
                            Email
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.email)}
                        </p>
                    </div>

                    <div>
                        <p class="text-xs text-gray-500">
                            Phone
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.phone)}
                        </p>
                    </div>

                    <div>
                        <p class="text-xs text-gray-500">
                            Age
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${worker.age ?? "Not provided"}
                        </p>
                    </div>

                    <div>
                        <p class="text-xs text-gray-500">
                            Gender
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.gender) || "Not provided"}
                        </p>
                    </div>

                </div>

            </div>


            <div class="border-t border-gray-800 pt-6">

                <h3 class="text-lg font-semibold mb-4">
                    Address
                </h3>

                <div class="space-y-3">

                    <div>
                        <p class="text-xs text-gray-500">
                            Address
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.addressLine1)}
                        </p>
                    </div>

                    ${worker.addressLine2 ? `
                    <div>
                        <p class="text-xs text-gray-500">
                            Address Line 2
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.addressLine2)}
                        </p>
                    </div>
                    ` : ""}

                    ${worker.landmark ? `
                    <div>
                        <p class="text-xs text-gray-500">
                            Landmark
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.landmark)}
                        </p>
                    </div>
                    ` : ""}

                    <div class="grid gap-4 sm:grid-cols-3">

                        <div>
                            <p class="text-xs text-gray-500">
                                City
                            </p>

                            <p class="mt-1 text-gray-200">
                                ${escapeHtml(worker.city)}
                            </p>
                        </div>

                        <div>
                            <p class="text-xs text-gray-500">
                                State
                            </p>

                            <p class="mt-1 text-gray-200">
                                ${escapeHtml(worker.state)}
                            </p>
                        </div>

                        <div>
                            <p class="text-xs text-gray-500">
                                PIN Code
                            </p>

                            <p class="mt-1 text-gray-200">
                                ${escapeHtml(worker.pinCode)}
                            </p>
                        </div>

                    </div>

                </div>

            </div>


            <div class="border-t border-gray-800 pt-6">

                <h3 class="text-lg font-semibold mb-4">
                    Professional Information
                </h3>

                <div class="grid gap-4 sm:grid-cols-2">

                    <div>
                        <p class="text-xs text-gray-500">
                            Experience
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${worker.experienceYears} years
                        </p>
                    </div>

                    <div>
                        <p class="text-xs text-gray-500">
                            Qualification
                        </p>

                        <p class="mt-1 text-gray-200">
                            ${escapeHtml(worker.qualification)}
                        </p>
                    </div>

                </div>

                <div class="mt-4">

                    <p class="text-xs text-gray-500">
                        Bio
                    </p>

                    <p class="mt-1 text-gray-300 leading-relaxed">
                        ${escapeHtml(worker.bio) || "No bio provided."}
                    </p>

                </div>

            </div>


            <div class="border-t border-gray-800 pt-6">

                <h3 class="text-lg font-semibold mb-4">
                    Services
                </h3>

                <div class="flex flex-wrap gap-2">

                    ${
                        worker.services && worker.services.length
                        ? worker.services.map(service => `
                            <span class="px-3 py-1.5 rounded-lg
                                         bg-violet-600/10
                                         text-violet-400 text-sm">

                                ${escapeHtml(service)}

                            </span>
                        `).join("")
                        : `
                            <span class="text-gray-500 text-sm">
                                No services assigned.
                            </span>
                        `
                    }

                </div>

            </div>


            <div class="border-t border-gray-800 pt-6">

                <h3 class="text-lg font-semibold mb-4">
                    Status
                </h3>

                <div class="flex flex-wrap gap-3">

                    <span class="px-3 py-1.5 rounded-lg
                                 ${getStatusClass(worker.verificationStatus)}
                                 text-sm">

                        ${formatStatus(worker.verificationStatus)}

                    </span>

                    <span class="px-3 py-1.5 rounded-lg
                                 bg-gray-500/10 text-gray-400
                                 text-sm">

                        Availability:
                        ${formatStatus(worker.availabilityStatus)}

                    </span>

                </div>

            </div>

        </div>
    `;
}


function closeWorkerDetailsModal() {

    workerDetailsModal.classList.add("hidden");

}


closeWorkerDetails.addEventListener(
    "click",
    closeWorkerDetailsModal
);


workerDetailsOverlay.addEventListener(
    "click",
    closeWorkerDetailsModal
);


loadWorkers("Pending");