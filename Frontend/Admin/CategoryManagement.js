const API_BASE_URL = "http://localhost:8080/api/admin/categories";
const CATEGORY_ACCESS_TOKEN_KEY = "servicehub_access_token";

const categoryTableBody = document.getElementById("categoryTableBody");
const categoryCount = document.getElementById("categoryCount");

const addCategoryButton = document.getElementById("addCategoryButton");
const addCategoryModal = document.getElementById("addCategoryModal");
const addCategoryOverlay = document.getElementById("addCategoryOverlay");
const addCategoryForm = document.getElementById("addCategoryForm");
const categoryNameInput = document.getElementById("categoryName");
const closeAddCategory = document.getElementById("closeAddCategory");
const cancelAddCategory = document.getElementById("cancelAddCategory");

let categories = [];


async function loadCategories() {

    categoryTableBody.innerHTML = `
        <tr>
            <td colspan="4" class="px-6 py-12 text-center text-gray-500">
                Loading categories...
            </td>
        </tr>
    `;

    const token =
        sessionStorage.getItem(CATEGORY_ACCESS_TOKEN_KEY);

    try {

        const response = await fetch(API_BASE_URL, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {

            if (response.status === 401 ||
                response.status === 403) {

                window.location.href = "AdminLogin.html";
                return;
            }

            throw new Error("Failed to load categories.");
        }

        categories = await response.json();

        categoryCount.textContent = categories.length;

        renderCategories(categories);

    } catch (error) {

        console.error(error);

        categoryTableBody.innerHTML = `
            <tr>
                <td colspan="4" class="px-6 py-12 text-center text-red-400">
                    Failed to load categories.
                </td>
            </tr>
        `;
    }
}


function renderCategories(categories) {

    if (categories.length === 0) {

        categoryTableBody.innerHTML = `
            <tr>
                <td colspan="4" class="px-6 py-16 text-center">

                    <div class="flex flex-col items-center">

                        <div class="w-12 h-12 rounded-full bg-gray-800
                                    flex items-center justify-center mb-4">

                            <i class="fa-solid fa-layer-group
                                      text-gray-600 text-lg"></i>

                        </div>

                        <p class="text-gray-400">
                            No categories found
                        </p>

                        <p class="text-gray-600 text-sm mt-1">
                            Add a category to get started.
                        </p>

                    </div>

                </td>
            </tr>
        `;

        return;
    }


    categoryTableBody.innerHTML = categories.map(category => `

        <tr class="border-b border-gray-800 last:border-0
                   hover:bg-gray-800/40 transition-colors">


            <td class="px-6 py-4">

                <div class="flex items-center gap-3">

                    <div class="w-10 h-10 rounded-full
                                bg-violet-600/15
                                flex items-center justify-center
                                text-violet-400">

                        <i class="fa-solid fa-layer-group"></i>

                    </div>

                    <div>

                        <p class="font-medium text-white">
                            ${escapeHtml(category.categoryName)}
                        </p>

                        <p class="text-xs text-gray-500">
                            ${escapeHtml(category.categoryId)}
                        </p>

                    </div>

                </div>

            </td>


            <td class="px-6 py-4">

                <span class="text-sm text-gray-300">
                    ${category.serviceCount}
                    ${category.serviceCount === 1 ? "service" : "services"}
                </span>

            </td>


            <td class="px-6 py-4">

                <span class="inline-flex px-2.5 py-1 rounded-full
                             text-xs font-medium
                             ${getStatusClass(category.active)}">

                    ${category.active ? "Active" : "Inactive"}

                </span>

            </td>


            <td class="px-6 py-4 text-right">

                <button
                    class="category-status-button px-3 py-2
                           text-sm
                           ${category.active
                        ? "text-red-400 hover:bg-red-600/10"
                        : "text-green-400 hover:bg-green-600/10"}
                           bg-gray-800
                           rounded-lg
                           transition-colors"
                    title="${category.active
                        ? "Deactivate category"
                        : "Activate category"}"
                    data-category-id="${escapeHtml(category.categoryId)}"
                    data-action="${category.active
                        ? "deactivate"
                        : "activate"}">

                    <i class="fa-solid
                              ${category.active
                        ? "fa-eye-slash"
                        : "fa-eye"}"></i>

                </button>

            </td>

        </tr>

    `).join("");
}


function getStatusClass(active) {

    if (active) {
        return "bg-green-500/10 text-green-400";
    }

    return "bg-red-500/10 text-red-400";
}


categoryTableBody.addEventListener("click", event => {

    const button =
        event.target.closest(".category-status-button");

    if (!button) {
        return;
    }

    const categoryId =
        button.dataset.categoryId;

    const action =
        button.dataset.action;

    updateCategoryStatus(categoryId, action);
});


async function updateCategoryStatus(categoryId, action) {

    const actionText =
        action === "activate"
            ? "activate"
            : "deactivate";

    const confirmed = confirm(
        `Are you sure you want to ${actionText} this category?`
    );

    if (!confirmed) {
        return;
    }

    const token =
        sessionStorage.getItem(CATEGORY_ACCESS_TOKEN_KEY);

    try {

        const response = await fetch(
            `${API_BASE_URL}/${categoryId}/${action}`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {

            if (response.status === 401 ||
                response.status === 403) {

                window.location.href = "AdminLogin.html";
                return;
            }

            throw new Error(
                `Failed to ${actionText} category.`
            );
        }

        await loadCategories();

        alert(
            `Category ${actionText}d successfully.`
        );

    } catch (error) {

        console.error(error);

        alert(
            `Failed to ${actionText} category.`
        );
    }
}


// ==================== ADD CATEGORY ====================

addCategoryButton.addEventListener("click", () => {

    openAddCategoryModal();

});


addCategoryForm.addEventListener("submit", async event => {

    event.preventDefault();

    const categoryName =
        categoryNameInput.value.trim();

    if (!categoryName) {
        return;
    }

    const token =
        sessionStorage.getItem(CATEGORY_ACCESS_TOKEN_KEY);

    const submitButton =
        addCategoryForm.querySelector(
            'button[type="submit"]'
        );

    submitButton.disabled = true;

    try {

        const response = await fetch(
            `${API_BASE_URL}?categoryName=${encodeURIComponent(categoryName)}`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {

            if (response.status === 401 ||
                response.status === 403) {

                window.location.href = "AdminLogin.html";
                return;
            }

            const errorData =
                await response.json().catch(() => null);

            throw new Error(
                errorData?.message ||
                "Failed to add category."
            );
        }

        closeAddCategoryModal();

        categoryNameInput.value = "";

        await loadCategories();

        alert("Category added successfully.");

    } catch (error) {

        console.error(error);

        alert(error.message);

    } finally {

        submitButton.disabled = false;

    }
});


function openAddCategoryModal() {

    addCategoryModal.classList.remove("hidden");

    categoryNameInput.focus();
}


function closeAddCategoryModal() {

    addCategoryModal.classList.add("hidden");

    categoryNameInput.value = "";
}


closeAddCategory.addEventListener(
    "click",
    closeAddCategoryModal
);


cancelAddCategory.addEventListener(
    "click",
    closeAddCategoryModal
);


addCategoryOverlay.addEventListener(
    "click",
    closeAddCategoryModal
);


function escapeHtml(value) {

    if (value === null || value === undefined) {
        return "";
    }

    const div = document.createElement("div");

    div.textContent = value;

    return div.innerHTML;
}


loadCategories();