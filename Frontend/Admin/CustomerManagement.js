const API_BASE_URL = "http://localhost:8080/api/admin/customers";
const CUSTOMER_ACCESS_TOKEN_KEY = "servicehub_access_token";

let customers = [];
let currentCustomer = null;
let currentStatusFilter = "all";


// ==================== INITIALIZATION ====================

document.addEventListener("DOMContentLoaded", () => {
    loadCustomers();
});


// ==================== API ====================

async function loadCustomers() {
    try {
        const token = sessionStorage.getItem(CUSTOMER_ACCESS_TOKEN_KEY);

        const response = await fetch(API_BASE_URL, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        });

        if (response.status === 401 || response.status === 403) {
            window.location.href = "AdminLogin.html";
            return;
        }

        if (!response.ok) {
            throw new Error("Failed to load customers.");
        }

        customers = await response.json();

        applyFilters();

    } catch (error) {
        console.error("Error loading customers:", error);
        showError("Unable to load customers.");
    }
}


async function viewCustomer(customerId) {
    try {
        const token = sessionStorage.getItem(CUSTOMER_ACCESS_TOKEN_KEY);

        const response = await fetch(`${API_BASE_URL}/${customerId}`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        });

        if (response.status === 401 || response.status === 403) {
            window.location.href = "AdminLogin.html";
            return;
        }

        if (!response.ok) {
            throw new Error("Failed to load customer details.");
        }

        currentCustomer = await response.json();

        showCustomerModal(currentCustomer);

    } catch (error) {
        console.error("Error loading customer details:", error);
        showError("Unable to load customer details.");
    }
}


// ==================== CUSTOMER STATUS ====================

async function changeCustomerStatus(customerId, action) {

    const actionText = action === "activate"
        ? "activate"
        : "deactivate";

    const confirmed = confirm(
        `Are you sure you want to ${actionText} this customer?`
    );

    if (!confirmed) {
        return;
    }

    try {
        const token = sessionStorage.getItem(CUSTOMER_ACCESS_TOKEN_KEY);

        const response = await fetch(
            `${API_BASE_URL}/${customerId}/${action}`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            }
        );

        if (response.status === 401 || response.status === 403) {
            window.location.href = "AdminLogin.html";
            return;
        }

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);

            throw new Error(
                errorData?.message ||
                `Failed to ${actionText} customer.`
            );
        }

        await loadCustomers();

        if (
            currentCustomer &&
            currentCustomer.customerId === customerId
        ) {
            closeCustomerModal();
            currentCustomer = null;
        }

        alert(`Customer ${actionText}d successfully.`);

    } catch (error) {
        console.error(
            `Error trying to ${actionText} customer:`,
            error
        );

        showError(error.message);
    }
}


// ==================== FILTERS ====================

function filterCustomers(status) {

    currentStatusFilter = status;

    updateFilterButtons();

    applyFilters();
}


function applyFilters() {

    const searchInput =
        document.getElementById("customerSearch");

    const searchValue = searchInput
        ? searchInput.value.trim().toLowerCase()
        : "";

    let filteredCustomers = customers.filter(customer => {

        // Status filter
        if (currentStatusFilter === "active" &&
            !customer.active) {
            return false;
        }

        if (currentStatusFilter === "inactive" &&
            customer.active) {
            return false;
        }

        // Search filter
        if (searchValue) {

            const name =
                customer.fullName?.toLowerCase() || "";

            const email =
                customer.email?.toLowerCase() || "";

            const phone =
                customer.phone?.toLowerCase() || "";

            return (
                name.includes(searchValue) ||
                email.includes(searchValue) ||
                phone.includes(searchValue)
            );
        }

        return true;
    });

    renderCustomers(filteredCustomers);
}


function searchCustomers() {
    applyFilters();
}


function updateFilterButtons() {

    const activeButton =
        document.getElementById("activeFilter");

    const inactiveButton =
        document.getElementById("inactiveFilter");


    if (!activeButton || !inactiveButton) {
        return;
    }


    // Reset both buttons
    activeButton.className =
        `px-5 py-3
         bg-gray-900
         border border-gray-800
         text-gray-400
         rounded-lg
         hover:bg-gray-800
         hover:text-white
         transition-colors`;

    inactiveButton.className =
        `px-5 py-3
         bg-gray-900
         border border-gray-800
         text-gray-400
         rounded-lg
         hover:bg-gray-800
         hover:text-white
         transition-colors`;


    // Highlight selected filter
    if (currentStatusFilter === "active") {

        activeButton.className =
            `px-5 py-3
             bg-gray-800
             border border-gray-700
             text-white
             rounded-lg
             hover:bg-gray-700
             transition-colors`;

    } else if (currentStatusFilter === "inactive") {

        inactiveButton.className =
            `px-5 py-3
             bg-gray-800
             border border-gray-700
             text-white
             rounded-lg
             hover:bg-gray-700
             transition-colors`;
    }
}


// ==================== RENDER CUSTOMERS ====================

function renderCustomers(customerList) {

    const tableBody =
        document.getElementById("customerTableBody");

    if (!tableBody) {
        return;
    }


    if (customerList.length === 0) {

        tableBody.innerHTML = `
            <tr>
                <td colspan="7"
                    class="px-6 py-10
                           text-center
                           text-gray-500">

                    No customers found.

                </td>
            </tr>
        `;

        return;
    }


    tableBody.innerHTML = customerList.map(customer => `

        <tr class="border-b border-gray-800
                   hover:bg-gray-900/60
                   transition-colors">

            <td class="px-6 py-4">
                <div class="font-medium text-white">
                    ${escapeHtml(customer.fullName)}
                </div>
            </td>


            <td class="px-6 py-4 text-gray-400">
                ${escapeHtml(customer.email)}
            </td>


            <td class="px-6 py-4 text-gray-400">
                ${escapeHtml(customer.phone)}
            </td>


            <td class="px-6 py-4 text-gray-400">
                ${escapeHtml(customer.city)}
            </td>


            <td class="px-6 py-4">
                ${getStatusBadge(customer.active)}
            </td>


            <td class="px-6 py-4 text-gray-400">
                ${formatDate(customer.createdAt)}
            </td>


            <td class="px-6 py-4">

                <div class="flex items-center gap-2">

                    <!-- View -->
                    <button
                        onclick="viewCustomer(
                            '${customer.customerId}'
                        )"
                        class="px-3 py-2
                               text-sm
                               text-gray-300
                               bg-gray-800
                               hover:bg-gray-700
                               rounded-lg
                               transition-colors"
                        title="View customer">

                        <i class="fa-solid fa-eye"></i>

                    </button>


                    <!-- Activate / Deactivate -->
                    ${
                        customer.active
                        ? `
                            <button
                                onclick="changeCustomerStatus(
                                    '${customer.customerId}',
                                    'deactivate'
                                )"
                                class="px-3 py-2
                                       text-sm
                                       text-red-400
                                       bg-gray-800
                                       hover:bg-red-900/30
                                       rounded-lg
                                       transition-colors"
                                title="Deactivate customer">

                                <i class="fa-solid fa-user-slash"></i>

                            </button>
                          `
                        : `
                            <button
                                onclick="changeCustomerStatus(
                                    '${customer.customerId}',
                                    'activate'
                                )"
                                class="px-3 py-2
                                       text-sm
                                       text-green-400
                                       bg-gray-800
                                       hover:bg-green-900/30
                                       rounded-lg
                                       transition-colors"
                                title="Activate customer">

                                <i class="fa-solid fa-user-check"></i>

                            </button>
                          `
                    }

                </div>

            </td>

        </tr>

    `).join("");
}


// ==================== STATUS BADGE ====================

function getStatusBadge(active) {

    if (active) {

        return `
            <span class="inline-flex
                         items-center
                         gap-2
                         px-3 py-1
                         rounded-full
                         text-xs
                         font-medium
                         bg-green-900/30
                         text-green-400">

                <span class="w-2 h-2
                             rounded-full
                             bg-green-400">
                </span>

                Active

            </span>
        `;
    }


    return `
        <span class="inline-flex
                     items-center
                     gap-2
                     px-3 py-1
                     rounded-full
                     text-xs
                     font-medium
                     bg-red-900/30
                     text-red-400">

            <span class="w-2 h-2
                         rounded-full
                         bg-red-400">
            </span>

            Inactive

        </span>
    `;
}


// ==================== CUSTOMER MODAL ====================

function showCustomerModal(customer) {

    const modal =
        document.getElementById("customerModal");

    if (!modal) {
        return;
    }


    document.getElementById("customerModalName").textContent =
        customer.fullName || "N/A";


    document.getElementById("customerModalStatus").innerHTML =
        getStatusBadge(customer.active);


    document.getElementById("customerFullName").textContent =
        customer.fullName || "N/A";


    document.getElementById("customerEmail").textContent =
        customer.email || "N/A";


    document.getElementById("customerPhone").textContent =
        customer.phone || "N/A";


    document.getElementById("customerAge").textContent =
        customer.age ?? "N/A";


    document.getElementById("customerGender").textContent =
        customer.gender || "N/A";


    document.getElementById("customerAddressLine1").textContent =
        customer.addressLine1 || "N/A";


    document.getElementById("customerAddressLine2").textContent =
        customer.addressLine2 || "N/A";


    document.getElementById("customerLandmark").textContent =
        customer.landmark || "N/A";


    document.getElementById("customerCity").textContent =
        customer.city || "N/A";


    document.getElementById("customerState").textContent =
        customer.state || "N/A";


    document.getElementById("customerPinCode").textContent =
        customer.pinCode || "N/A";


    document.getElementById("customerTermsAccepted").textContent =
        customer.termsAccepted
            ? "Accepted"
            : "Not Accepted";


    document.getElementById("customerCreatedAt").textContent =
        formatDate(customer.createdAt);


    document.getElementById("customerUpdatedAt").textContent =
        formatDate(customer.updatedAt);


    const actionContainer =
        document.getElementById("customerModalActions");


    if (actionContainer) {

        if (customer.active) {

            actionContainer.innerHTML = `
                <button
                    onclick="changeCustomerStatus(
                        '${customer.customerId}',
                        'deactivate'
                    )"
                    class="px-4 py-2
                           rounded-lg
                           bg-red-600
                           hover:bg-red-700
                           text-white
                           transition-colors">

                    <i class="fa-solid fa-user-slash mr-2"></i>

                    Deactivate Customer

                </button>
            `;

        } else {

            actionContainer.innerHTML = `
                <button
                    onclick="changeCustomerStatus(
                        '${customer.customerId}',
                        'activate'
                    )"
                    class="px-4 py-2
                           rounded-lg
                           bg-green-600
                           hover:bg-green-700
                           text-white
                           transition-colors">

                    <i class="fa-solid fa-user-check mr-2"></i>

                    Activate Customer

                </button>
            `;
        }
    }


    modal.classList.remove("hidden");
    modal.classList.add("flex");
}


// ==================== CLOSE MODAL ====================

function closeCustomerModal() {

    const modal =
        document.getElementById("customerModal");

    if (modal) {
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }
}


// ==================== DATE FORMAT ====================

function formatDate(dateValue) {

    if (!dateValue) {
        return "N/A";
    }


    const date = new Date(dateValue);


    if (Number.isNaN(date.getTime())) {
        return "N/A";
    }


    return date.toLocaleString();
}


// ==================== HTML ESCAPING ====================

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


// ==================== ERROR ====================

function showError(message) {
    alert(message);
}