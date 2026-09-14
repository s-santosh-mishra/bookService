const API_BASE_URL = "http://localhost:8080/api/admin/services";
const CATEGORY_API_URL = "http://localhost:8080/api/admin/categories";
const SERVICE_ACCESS_TOKEN_KEY = "servicehub_access_token";

const serviceTableBody = document.getElementById("serviceTableBody");
const serviceCount = document.getElementById("serviceCount");

const addServiceButton = document.getElementById("addServiceButton");
const addServiceModal = document.getElementById("addServiceModal");
const addServiceOverlay = document.getElementById("addServiceOverlay");
const addServiceForm = document.getElementById("addServiceForm");
const serviceNameInput = document.getElementById("serviceName");
const categoryIdInput = document.getElementById("categoryId");
const closeAddService = document.getElementById("closeAddService");
const cancelAddService = document.getElementById("cancelAddService");
const categoryFilter = document.getElementById("categoryFilter");

let services = [];
let categories = [];


async function loadServices() {

    serviceTableBody.innerHTML = `
        <tr>
            <td colspan="5"
                class="px-6 py-12 text-center text-gray-500">
                Loading services...
            </td>
        </tr>
    `;

    const token = sessionStorage.getItem(
        SERVICE_ACCESS_TOKEN_KEY
    );

    try {

        const response = await fetch(API_BASE_URL, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                window.location.href = "AdminLogin.html";
                return;
            }

            throw new Error("Failed to load services.");
        }

        services = await response.json();

        serviceCount.textContent = services.length;

        await loadWorkerCounts();

        populateCategoryFilter();

        renderServices(services);

    } catch (error) {

        console.error(error);

        serviceTableBody.innerHTML = `
            <tr>
                <td colspan="5"
                    class="px-6 py-12 text-center text-red-400">
                    Failed to load services.
                </td>
            </tr>
        `;
    }
}


async function loadWorkerCounts() {

    const token = sessionStorage.getItem(
        SERVICE_ACCESS_TOKEN_KEY
    );

    await Promise.all(
        services.map(async service => {

            try {

                const response = await fetch(
                    `${API_BASE_URL}/${service.serviceId}/workers`,
                    {
                        method: "GET",
                        headers: {
                            "Authorization": `Bearer ${token}`
                        }
                    }
                );

                if (
                    response.status === 401 ||
                    response.status === 403
                ) {
                    window.location.href = "AdminLogin.html";
                    return;
                }

                if (!response.ok) {
                    throw new Error(
                        `Failed to load workers for ${service.serviceName}.`
                    );
                }

                const workers = await response.json();

                service.workerCount = workers.length;

            } catch (error) {

                console.error(error);

                service.workerCount = 0;
            }
        })
    );
}


function renderServices(servicesToRender) {

    if (servicesToRender.length === 0) {

        serviceTableBody.innerHTML = `
            <tr>
                <td colspan="5"
                    class="px-6 py-16 text-center">

                    <div class="flex flex-col items-center">

                        <div class="w-12 h-12 rounded-full
                                    bg-gray-800
                                    flex items-center justify-center
                                    mb-4">

                            <i class="fa-solid fa-screwdriver-wrench
                                      text-gray-600 text-lg"></i>

                        </div>

                        <p class="text-gray-400">
                            No services found
                        </p>

                        <p class="text-gray-600 text-sm mt-1">
                            Add a service to get started.
                        </p>

                    </div>

                </td>
            </tr>
        `;

        return;
    }

    serviceTableBody.innerHTML = servicesToRender.map(service => `
        <tr class="border-b border-gray-800 last:border-0
                   hover:bg-gray-800/40 transition-colors">

            <td class="px-6 py-4">

                <div>

                        <p class="font-medium text-white">
                            ${escapeHtml(service.serviceName)}
                        </p>

                        <p class="text-xs text-gray-500">
                            ${escapeHtml(service.serviceId)}
                        </p>

                    </div>

            </td>

            <td class="px-6 py-4">

                <div>

                    <p class="text-sm text-gray-300">
                        ${escapeHtml(service.categoryName)}
                    </p>

                    <p class="text-xs text-gray-600 mt-1">
                        ${escapeHtml(service.categoryId)}
                    </p>

                </div>

            </td>

            <td class="px-6 py-4 text-center">

                <span class="text-sm text-gray-300">
                    ${service.workerCount ?? 0}
                </span>

            </td>

            <td class="px-6 py-4">

                <span class="inline-flex px-2.5 py-1
                             rounded-full
                             text-xs font-medium
                             ${getStatusClass(service.active)}">

                    ${service.active ? "Active" : "Inactive"}

                </span>

            </td>

            <td class="px-6 py-4 text-right">

                <button
                    type="button"
                    class="service-status-button
                           px-3 py-2
                           text-sm
                           ${service.active
                        ? "text-red-400 hover:bg-red-600/10"
                        : "text-green-400 hover:bg-green-600/10"}
                           bg-gray-800
                           rounded-lg
                           transition-colors"
                    title="${service.active
                        ? "Deactivate service"
                        : "Activate service"}"
                    data-service-id="${escapeHtml(service.serviceId)}"
                    data-action="${service.active
                        ? "deactivate"
                        : "activate"}">

                    <i class="fa-solid
                              ${service.active
                        ? "fa-eye-slash"
                        : "fa-eye"}"></i>

                </button>

            </td>

        </tr>
    `).join("");
}


function populateCategoryFilter() {

    const uniqueCategories = new Map();

    services.forEach(service => {

        uniqueCategories.set(
            service.categoryId,
            service.categoryName
        );

    });

    categoryFilter.innerHTML = `
        <option value="">
            All Categories
        </option>

        ${Array.from(uniqueCategories.entries())
            .map(([categoryId, categoryName]) => `
                <option value="${escapeHtml(categoryId)}">
                    ${escapeHtml(categoryName)}
                </option>
            `)
            .join("")}
    `;
}


categoryFilter.addEventListener("change", () => {

    const selectedCategoryId = categoryFilter.value;

    if (!selectedCategoryId) {

        renderServices(services);

        return;
    }

    const filteredServices = services.filter(
        service =>
            service.categoryId === selectedCategoryId
    );

    renderServices(filteredServices);
});


function getStatusClass(active) {

    if (active) {
        return "bg-green-500/10 text-green-400";
    }

    return "bg-red-500/10 text-red-400";
}


serviceTableBody.addEventListener("click", event => {

    const button = event.target.closest(
        ".service-status-button"
    );

    if (!button) {
        return;
    }

    const serviceId = button.dataset.serviceId;
    const action = button.dataset.action;

    updateServiceStatus(serviceId, action);
});


async function updateServiceStatus(serviceId, action) {

    const actionText =
        action === "activate"
            ? "activate"
            : "deactivate";

    const confirmed = confirm(
        `Are you sure you want to ${actionText} this service?`
    );

    if (!confirmed) {
        return;
    }

    const token = sessionStorage.getItem(
        SERVICE_ACCESS_TOKEN_KEY
    );

    try {

        const response = await fetch(
            `${API_BASE_URL}/${serviceId}/${action}`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                window.location.href = "AdminLogin.html";
                return;
            }

            const errorData =
                await response.json().catch(() => null);

            throw new Error(
                errorData?.message ||
                `Failed to ${actionText} service.`
            );
        }

        await loadServices();

        alert(
            `Service ${actionText}d successfully.`
        );

    } catch (error) {

        console.error(error);

        alert(error.message);
    }
}


async function loadCategories() {

    const token = sessionStorage.getItem(
        SERVICE_ACCESS_TOKEN_KEY
    );

    categoryIdInput.innerHTML = `
        <option value="">
            Loading categories...
        </option>
    `;

    try {

        const response = await fetch(
            CATEGORY_API_URL,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                window.location.href = "AdminLogin.html";
                return;
            }

            throw new Error(
                "Failed to load categories."
            );
        }

        categories = await response.json();

        const activeCategories =
            categories.filter(category => category.active);

        if (activeCategories.length === 0) {

            categoryIdInput.innerHTML = `
                <option value="">
                    No active categories available
                </option>
            `;

            return;
        }

        categoryIdInput.innerHTML = `
            <option value="">
                Select a category
            </option>

            ${activeCategories.map(category => `
                <option value="${escapeHtml(category.categoryId)}">
                    ${escapeHtml(category.categoryName)}
                </option>
            `).join("")}
        `;

    } catch (error) {

        console.error(error);

        categoryIdInput.innerHTML = `
            <option value="">
                Failed to load categories
            </option>
        `;
    }
}


addServiceButton.addEventListener("click", async () => {

    await loadCategories();

    openAddServiceModal();
});


addServiceForm.addEventListener("submit", async event => {

    event.preventDefault();

    const serviceName =
        serviceNameInput.value.trim();

    const categoryId =
        categoryIdInput.value;

    if (!serviceName || !categoryId) {
        return;
    }

    const token = sessionStorage.getItem(
        SERVICE_ACCESS_TOKEN_KEY
    );

    const submitButton =
        addServiceForm.querySelector(
            'button[type="submit"]'
        );

    submitButton.disabled = true;

    try {

        const response = await fetch(
            `${API_BASE_URL}?serviceName=${encodeURIComponent(serviceName)}&categoryId=${encodeURIComponent(categoryId)}`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                window.location.href = "AdminLogin.html";
                return;
            }

            const errorData =
                await response.json().catch(() => null);

            throw new Error(
                errorData?.message ||
                "Failed to add service."
            );
        }

        closeAddServiceModal();

        serviceNameInput.value = "";
        categoryIdInput.value = "";

        await loadServices();

        alert("Service added successfully.");

    } catch (error) {

        console.error(error);

        alert(error.message);

    } finally {

        submitButton.disabled = false;
    }
});


function openAddServiceModal() {

    addServiceModal.classList.remove("hidden");

    serviceNameInput.focus();
}


function closeAddServiceModal() {

    addServiceModal.classList.add("hidden");

    serviceNameInput.value = "";
    categoryIdInput.value = "";
}


closeAddService.addEventListener(
    "click",
    closeAddServiceModal
);


cancelAddService.addEventListener(
    "click",
    closeAddServiceModal
);


addServiceOverlay.addEventListener(
    "click",
    closeAddServiceModal
);


function escapeHtml(value) {

    if (
        value === null ||
        value === undefined
    ) {
        return "";
    }

    const div = document.createElement("div");

    div.textContent = value;

    return div.innerHTML;
}


loadServices();