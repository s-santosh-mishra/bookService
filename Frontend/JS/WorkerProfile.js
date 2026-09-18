const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";
const USER_NAME_KEY = "servicehub_user_name";

const API_BASE_URL = "http://localhost:8080";


// EDITABLE FIELDS

const editableFields = [
    "fullName",
    "age",
    "gender",
    "phone",
    "addressLine1",
    "addressLine2",
    "landmark",
    "city",
    "state",
    "pinCode",
    "experienceYears",
    "qualification",
    "bio"
];


// STATE

let isEditMode = false;
let originalProfile = null;


// DOM

const profileForm = document.getElementById("profileForm");
const editProfileBtn = document.getElementById("editProfileBtn");
const cancelBtn = document.getElementById("cancelBtn");
const formActions = document.getElementById("formActions");
const profileMessage = document.getElementById("profileMessage");


// AUTH GUARD

function checkAuthentication() {

    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const role = localStorage.getItem(ROLE_KEY);

    if (!token || role !== "WORKER") {
        window.location.href = "../HTML/login.html";
        return false;
    }

    return true;
}


// MESSAGE

function showMessage(message, type = "success") {

    if (!profileMessage) return;

    profileMessage.textContent = message;

    profileMessage.classList.remove(
        "hidden",
        "bg-green-900/30",
        "text-green-400",
        "border-green-700/30",
        "bg-red-900/30",
        "text-red-400",
        "border-red-700/30"
    );

    if (type === "success") {

        profileMessage.classList.add(
            "bg-green-900/30",
            "text-green-400",
            "border",
            "border-green-700/30"
        );

    } else {

        profileMessage.classList.add(
            "bg-red-900/30",
            "text-red-400",
            "border",
            "border-red-700/30"
        );
    }

    profileMessage.classList.remove("hidden");

    setTimeout(() => {
        profileMessage.classList.add("hidden");
    }, 4000);
}


// ESCAPE HTML

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


// FORMAT TEXT

function formatStatus(status) {

    if (!status) {
        return "—";
    }

    return status
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase());
}


// STATUS BADGE

function createStatusBadge(status, type) {

    const formattedStatus = formatStatus(status);

    let classes = "";
    let icon = "";

    if (type === "verification") {

        if (status === "VERIFIED") {

            classes =
                "bg-green-900/30 text-green-400 border-green-700/30";

            icon = "fa-circle-check";

        } else if (status === "PENDING") {

            classes =
                "bg-yellow-900/30 text-yellow-400 border-yellow-700/30";

            icon = "fa-clock";

        } else if (status === "REJECTED" || status === "SUSPENDED") {

            classes =
                "bg-red-900/30 text-red-400 border-red-700/30";

            icon = "fa-circle-xmark";

        }

    } else {

        if (status === "AVAILABLE") {

            classes =
                "bg-green-900/30 text-green-400 border-green-700/30";

            icon = "fa-circle-check";

        } else if (status === "BUSY") {

            classes =
                "bg-yellow-900/30 text-yellow-400 border-yellow-700/30";

            icon = "fa-clock";

        } else {

            classes =
                "bg-gray-800 text-gray-400 border-gray-700";

            icon = "fa-circle-minus";
        }
    }

    return `
        <span class="inline-flex items-center gap-2
                     px-3 py-1.5 rounded-full text-sm
                     border ${classes}">

            <i class="fa-solid ${icon}"></i>

            ${escapeHtml(formattedStatus)}

        </span>
    `;
}


// API REQUEST

async function workerApiRequest(url, options = {}) {

    const token = localStorage.getItem(ACCESS_TOKEN_KEY);

    const response = await fetch(`${API_BASE_URL}${url}`, {
        ...options,

        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {}),
            "Authorization": `Bearer ${token}`
        }
    });


    // Only authentication failure should redirect.
    if (response.status === 401) {

        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(ROLE_KEY);
        localStorage.removeItem(USER_NAME_KEY);

        window.location.href =
            "../HTML/login.html";

        throw new Error("Unauthorized");
    }


    const contentType = response.headers.get("content-type") || "";

    let data = null;

    if (contentType.includes("application/json")) {

        data = await response.json();

    } else {

        data = await response.text();
    }


    if (!response.ok) {

        let message = "Something went wrong.";

        if (typeof data === "string" && data.trim()) {

            message = data;

        } else if (data?.message) {

            message = data.message;

        } else if (data?.error) {

            message = data.error;
        }

        throw new Error(message);
    }

    return data;
}


// LOAD PROFILE

async function loadProfile() {

    try {

        const profile = await workerApiRequest(
            "/api/worker/profile",
            {
                method: "GET"
            }
        );

        originalProfile = JSON.parse(
            JSON.stringify(profile)
        );

        populateProfile(profile);

        setEditMode(false);

    } catch (error) {

        console.error("Failed to load worker profile:", error);

        if (error.message !== "Unauthorized") {

            showMessage(
                error.message || "Failed to load profile.",
                "error"
            );
        }
    }
}


// POPULATE PROFILE

function populateProfile(profile) {

    setValue("fullName", profile.fullName);
    setValue("email", profile.email);
    setValue("age", profile.age);
    setValue("gender", profile.gender);
    setValue("phone", profile.phone);

    setValue("addressLine1", profile.addressLine1);
    setValue("addressLine2", profile.addressLine2);
    setValue("landmark", profile.landmark);
    setValue("city", profile.city);
    setValue("state", profile.state);
    setValue("pinCode", profile.pinCode);

    setValue("experienceYears", profile.experienceYears);
    setValue("qualification", profile.qualification);
    setValue("bio", profile.bio);


    // Statuses
    renderWorkerStatus(
        profile.verificationStatus,
        profile.availabilityStatus
    );


    // Services
    renderProvidingServices(profile.services);


    // Hide empty optional fields in view mode.
    updateOptionalFieldVisibility(
        "addressLine2Container",
        profile.addressLine2
    );

    updateOptionalFieldVisibility(
        "landmarkContainer",
        profile.landmark
    );

    updateOptionalFieldVisibility(
        "bioContainer",
        profile.bio
    );
}


// SET VALUE

function setValue(id, value) {

    const element = document.getElementById(id);

    if (!element) return;

    if (element.tagName === "TEXTAREA" ||
        element.tagName === "INPUT" ||
        element.tagName === "SELECT") {

        element.value =
            value !== null && value !== undefined
                ? value
                : "";

    } else {

        element.textContent =
            value !== null && value !== undefined
                ? value
                : "—";
    }
}


// OPTIONAL FIELD VISIBILITY

function updateOptionalFieldVisibility(containerId, value) {

    const container =
        document.getElementById(containerId);

    if (!container) return;


    // During edit mode, always show optional fields.
    if (isEditMode) {

        container.classList.remove("hidden");
        return;
    }


    // During view mode, hide empty optional fields.
    if (
        value === null ||
        value === undefined ||
        String(value).trim() === ""
    ) {

        container.classList.add("hidden");

    } else {

        container.classList.remove("hidden");
    }
}


// RENDER WORKER STATUS

function renderWorkerStatus(
    verificationStatus,
    availabilityStatus
) {

    const verificationElement =
        document.getElementById("verificationStatus");

    const availabilityElement =
        document.getElementById("availabilityStatus");


    if (verificationElement) {

        verificationElement.innerHTML =
            createStatusBadge(
                verificationStatus,
                "verification"
            );
    }


    if (availabilityElement) {

        availabilityElement.innerHTML =
            createStatusBadge(
                availabilityStatus,
                "availability"
            );
    }
}


// RENDER PROVIDING SERVICES

function renderProvidingServices(services) {

    const container =
        document.getElementById("providingServices");

    if (!container) return;


    if (!services || services.length === 0) {

        container.innerHTML = `
            <div class="col-span-full
                        text-sm text-gray-500
                        py-2">

                You are not currently providing any services.

            </div>
        `;

        return;
    }


    container.innerHTML = services.map(service => {

        return `
            <div class="flex items-center gap-3
                        bg-gray-800/60
                        border border-gray-700
                        rounded-xl
                        px-4 py-3">

                <div class="w-8 h-8
                            rounded-lg
                            bg-green-900/30
                            flex items-center justify-center
                            flex-shrink-0">

                    <i class="fa-solid fa-check
                              text-green-400
                              text-sm">
                    </i>

                </div>

                <span class="text-sm
                             font-medium
                             text-gray-200">

                    ${escapeHtml(service)}

                </span>

            </div>
        `;

    }).join("");
}


// GET FORM DATA

function getFormData() {

    return {

        fullName:
            document.getElementById("fullName").value.trim(),

        age:
            getNumberValue("age"),

        gender:
            getStringValue("gender"),

        phone:
            getStringValue("phone"),

        addressLine1:
            getStringValue("addressLine1"),

        addressLine2:
            getStringValue("addressLine2"),

        landmark:
            getStringValue("landmark"),

        city:
            getStringValue("city"),

        state:
            getStringValue("state"),

        pinCode:
            getStringValue("pinCode"),

        experienceYears:
            getNumberValue("experienceYears"),

        qualification:
            getStringValue("qualification"),

        bio:
            getStringValue("bio")
    };
}


// STRING VALUE

function getStringValue(id) {

    const element =
        document.getElementById(id);

    if (!element) return null;

    const value = element.value.trim();

    return value === "" ? null : value;
}


// NUMBER VALUE

function getNumberValue(id) {

    const element =
        document.getElementById(id);

    if (!element) return null;

    const value = element.value.trim();

    if (value === "") {
        return null;
    }

    return Number(value);
}


// SET EDIT MODE

function setEditMode(editing) {

    isEditMode = editing;


    // Editable inputs
    editableFields.forEach(id => {

        const element =
            document.getElementById(id);

        if (!element) return;


        if (editing) {

            element.disabled = false;

            element.classList.add("profile-input-editing");

        } else {

            element.disabled = true;

            element.classList.remove(
                "profile-input-editing"
            );
        }
    });


    // Optional fields
    if (originalProfile) {

        updateOptionalFieldVisibility(
            "addressLine2Container",
            originalProfile.addressLine2
        );

        updateOptionalFieldVisibility(
            "landmarkContainer",
            originalProfile.landmark
        );

        updateOptionalFieldVisibility(
            "bioContainer",
            originalProfile.bio
        );
    }


    // Edit button
    if (editProfileBtn) {

        if (editing) {

            editProfileBtn.classList.add("hidden");

        } else {

            editProfileBtn.classList.remove("hidden");
        }
    }


    // Save / Cancel buttons
    if (formActions) {

        if (editing) {

            formActions.classList.remove("hidden");

        } else {

            formActions.classList.add("hidden");
        }
    }
}


// CANCEL EDIT

function cancelEdit() {

    if (!originalProfile) return;

    populateProfile(originalProfile);

    setEditMode(false);
}


// SAVE PROFILE

async function saveProfile(event) {

    event.preventDefault();


    if (!isEditMode) {
        return;
    }


    const data = getFormData();


    try {

        const updatedProfile =
            await workerApiRequest(
                "/api/worker/profile",
                {
                    method: "PUT",
                    body: JSON.stringify(data)
                }
            );


        // Store latest profile locally.
        originalProfile = JSON.parse(
            JSON.stringify(updatedProfile)
        );


        // Update dashboard welcome name.
        if (updatedProfile.fullName) {

            localStorage.setItem(
                USER_NAME_KEY,
                updatedProfile.fullName
            );
        }


        populateProfile(updatedProfile);

        setEditMode(false);


        showMessage(
            "Profile updated successfully.",
            "success"
        );


    } catch (error) {

        console.error(
            "Failed to update worker profile:",
            error
        );


        if (error.message !== "Unauthorized") {

            showMessage(
                error.message || "Failed to update profile.",
                "error"
            );
        }
    }
}


// EVENT LISTENERS

if (editProfileBtn) {

    editProfileBtn.addEventListener(
        "click",
        () => setEditMode(true)
    );
}


if (cancelBtn) {

    cancelBtn.addEventListener(
        "click",
        cancelEdit
    );
}


if (profileForm) {

    profileForm.addEventListener(
        "submit",
        saveProfile
    );
}


// INITIALIZATION

async function initializeProfile() {

    if (!checkAuthentication()) {
        return;
    }

    await loadProfile();
}


initializeProfile();