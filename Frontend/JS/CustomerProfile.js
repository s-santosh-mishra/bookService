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
    "pinCode"
];


// STATE

let isEditMode = false;
let originalProfile = null;


// DOM

const profileForm =
    document.getElementById("profileForm");

const editProfileBtn =
    document.getElementById("editProfileBtn");

const cancelBtn =
    document.getElementById("cancelBtn");

const formActions =
    document.getElementById("formActions");

const profileMessage =
    document.getElementById("profileMessage");


// AUTH GUARD

function checkAuthentication() {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const role =
        localStorage.getItem(ROLE_KEY);

    if (!token || role !== "USER") {

        window.location.href =
            "/bookService/Frontend/HTML/Login.html";

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


// API REQUEST

async function customerApiRequest(url, options = {}) {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);


    const response = await fetch(
        `${API_BASE_URL}${url}`,
        {
            ...options,

            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {}),
                "Authorization": `Bearer ${token}`
            }
        }
    );


    // Only authentication failure redirects.
    if (response.status === 401) {

        localStorage.removeItem(
            ACCESS_TOKEN_KEY
        );

        localStorage.removeItem(
            ROLE_KEY
        );

        localStorage.removeItem(
            USER_NAME_KEY
        );


        window.location.href =
            "/bookService/Frontend/HTML/Login.html";


        throw new Error("Unauthorized");
    }


    const contentType =
        response.headers.get("content-type") || "";


    let data = null;


    if (contentType.includes("application/json")) {

        data = await response.json();

    } else {

        data = await response.text();
    }


    if (!response.ok) {

        let message =
            "Something went wrong.";


        if (
            typeof data === "string" &&
            data.trim()
        ) {

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

        const profile =
            await customerApiRequest(
                "/api/customer/profile",
                {
                    method: "GET"
                }
            );


        originalProfile =
            JSON.parse(
                JSON.stringify(profile)
            );


        populateProfile(profile);

        setEditMode(false);


    } catch (error) {

        console.error(
            "Failed to load customer profile:",
            error
        );


        if (error.message !== "Unauthorized") {

            showMessage(
                error.message ||
                "Failed to load profile.",
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

    setValue(
        "addressLine1",
        profile.addressLine1
    );

    setValue(
        "addressLine2",
        profile.addressLine2
    );

    setValue(
        "landmark",
        profile.landmark
    );

    setValue(
        "city",
        profile.city
    );

    setValue(
        "state",
        profile.state
    );

    setValue(
        "pinCode",
        profile.pinCode
    );


    // Hide empty optional fields
    // when not editing.

    updateOptionalFieldVisibility(
        "addressLine2Container",
        profile.addressLine2
    );

    updateOptionalFieldVisibility(
        "landmarkContainer",
        profile.landmark
    );
}


// SET VALUE

function setValue(id, value) {

    const element =
        document.getElementById(id);

    if (!element) return;


    if (
        element.tagName === "INPUT" ||
        element.tagName === "SELECT" ||
        element.tagName === "TEXTAREA"
    ) {

        element.value =
            value !== null &&
            value !== undefined
                ? value
                : "";

    } else {

        element.textContent =
            value !== null &&
            value !== undefined
                ? value
                : "—";
    }
}


// OPTIONAL FIELD VISIBILITY

function updateOptionalFieldVisibility(
    containerId,
    value
) {

    const container =
        document.getElementById(containerId);

    if (!container) return;


    // Always show while editing.
    if (isEditMode) {

        container.classList.remove("hidden");

        return;
    }


    // Hide when empty.
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


// GET FORM DATA

function getFormData() {

    return {

        fullName:
            getStringValue("fullName"),

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
            getStringValue("pinCode")
    };
}


// STRING VALUE

function getStringValue(id) {

    const element =
        document.getElementById(id);

    if (!element) return null;


    const value =
        element.value.trim();


    return value === ""
        ? null
        : value;
}


// NUMBER VALUE

function getNumberValue(id) {

    const element =
        document.getElementById(id);

    if (!element) return null;


    const value =
        element.value.trim();


    if (value === "") {
        return null;
    }


    return Number(value);
}


// SET EDIT MODE

function setEditMode(editing) {

    isEditMode = editing;


    // Enable / disable editable fields.
    editableFields.forEach(id => {

        const element =
            document.getElementById(id);

        if (!element) return;


        if (editing) {

            element.disabled = false;

            element.classList.add(
                "profile-input-editing"
            );

        } else {

            element.disabled = true;

            element.classList.remove(
                "profile-input-editing"
            );
        }
    });


    // Optional fields.
    if (originalProfile) {

        updateOptionalFieldVisibility(
            "addressLine2Container",
            originalProfile.addressLine2
        );

        updateOptionalFieldVisibility(
            "landmarkContainer",
            originalProfile.landmark
        );
    }


    // Edit button.
    if (editProfileBtn) {

        if (editing) {

            editProfileBtn.classList.add(
                "hidden"
            );

        } else {

            editProfileBtn.classList.remove(
                "hidden"
            );
        }
    }


    // Save / Cancel.
    if (formActions) {

        if (editing) {

            formActions.classList.remove(
                "hidden"
            );

        } else {

            formActions.classList.add(
                "hidden"
            );
        }
    }
}


// CANCEL EDIT

function cancelEdit() {

    if (!originalProfile) return;


    populateProfile(
        originalProfile
    );


    setEditMode(false);
}


// SAVE PROFILE

async function saveProfile(event) {

    event.preventDefault();


    if (!isEditMode) {
        return;
    }


    const data =
        getFormData();


    try {

        const updatedProfile =
            await customerApiRequest(
                "/api/customer/profile",
                {
                    method: "PUT",

                    body:
                        JSON.stringify(data)
                }
            );


        originalProfile =
            JSON.parse(
                JSON.stringify(updatedProfile)
            );


        // Keep dashboard name synchronized.
        if (updatedProfile.fullName) {

            localStorage.setItem(
                USER_NAME_KEY,
                updatedProfile.fullName
            );
        }


        populateProfile(
            updatedProfile
        );


        setEditMode(false);


        showMessage(
            "Profile updated successfully.",
            "success"
        );


    } catch (error) {

        console.error(
            "Failed to update customer profile:",
            error
        );


        if (error.message !== "Unauthorized") {

            showMessage(
                error.message ||
                "Failed to update profile.",
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