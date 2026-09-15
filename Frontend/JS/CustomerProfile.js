const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";
const USER_NAME_KEY = "servicehub_user_name";

const API_BASE_URL = "http://localhost:8080";


// Authentication

function checkCustomerLogin() {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const role =
        localStorage.getItem(ROLE_KEY);

    if (!token || role !== "USER") {

        window.location.href =
            "/bookService/Frontend/HTML/login.html";

        return false;
    }

    return true;
}


// DOM Elements

const profileForm =
    document.getElementById("profileForm");

const editProfileBtn =
    document.getElementById("editProfileBtn");

const cancelProfileBtn =
    document.getElementById("cancelProfileBtn");

const profileActions =
    document.getElementById("profileActions");

const profileMessage =
    document.getElementById("profileMessage");


// Form Fields

const fields = [
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


// Message

function showMessage(message, type) {

    profileMessage.textContent = message;

    profileMessage.classList.remove(
        "hidden",
        "bg-red-900/40",
        "text-red-300",
        "border-red-800",
        "bg-green-900/40",
        "text-green-300",
        "border-green-800"
    );

    if (type === "success") {

        profileMessage.classList.add(
            "bg-green-900/40",
            "text-green-300",
            "border",
            "border-green-800"
        );

    } else {

        profileMessage.classList.add(
            "bg-red-900/40",
            "text-red-300",
            "border",
            "border-red-800"
        );
    }
}


// Enable / Disable Editing

function setEditMode(enabled) {

    fields.forEach(id => {

        const field =
            document.getElementById(id);

        field.disabled = !enabled;
    });

    editProfileBtn.classList.toggle(
        "hidden",
        enabled
    );

    profileActions.classList.toggle(
        "hidden",
        !enabled
    );
}


// Load Profile

async function loadProfile() {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    try {

        const response = await fetch(
            `${API_BASE_URL}/api/customer/profile`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (response.status === 401 ||
            response.status === 403) {

            localStorage.removeItem(
                ACCESS_TOKEN_KEY
            );

            window.location.href =
                "/bookService/Frontend/HTML/login.html";

            return;
        }

        if (!response.ok) {
            throw new Error(
                "Unable to load profile."
            );
        }

        const profile =
            await response.json();

        populateProfile(profile);

    } catch (error) {

        console.error(
            "Profile loading error:",
            error
        );

        showMessage(
            "Unable to load your profile. Please try again.",
            "error"
        );
    }
}


// Populate Profile

function populateProfile(profile) {

    document.getElementById("fullName").value =
        profile.fullName || "";

    document.getElementById("email").value =
        profile.email || "";

    document.getElementById("age").value =
        profile.age ?? "";

    document.getElementById("gender").value =
        profile.gender || "";

    document.getElementById("phone").value =
        profile.phone || "";

    document.getElementById("addressLine1").value =
        profile.addressLine1 || "";

    document.getElementById("addressLine2").value =
        profile.addressLine2 || "";

    document.getElementById("landmark").value =
        profile.landmark || "";

    document.getElementById("city").value =
        profile.city || "";

    document.getElementById("state").value =
        profile.state || "";

    document.getElementById("pinCode").value =
        profile.pinCode || "";
}


// Collect Form Data

function getFormData() {

    return {

        fullName:
            document.getElementById("fullName").value.trim(),

        age:
            Number(
                document.getElementById("age").value
            ),

        gender:
            document.getElementById("gender").value,

        phone:
            document.getElementById("phone").value.trim(),

        addressLine1:
            document.getElementById("addressLine1").value.trim(),

        addressLine2:
            document.getElementById("addressLine2").value.trim(),

        landmark:
            document.getElementById("landmark").value.trim(),

        city:
            document.getElementById("city").value.trim(),

        state:
            document.getElementById("state").value.trim(),

        pinCode:
            document.getElementById("pinCode").value.trim()
    };
}


// Save Profile

async function saveProfile() {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const profileData =
        getFormData();

    try {

        const response = await fetch(
            `${API_BASE_URL}/api/customer/profile`,
            {
                method: "PUT",

                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },

                body:
                    JSON.stringify(profileData)
            }
        );

        if (response.status === 401 ||
            response.status === 403) {

            localStorage.removeItem(
                ACCESS_TOKEN_KEY
            );

            window.location.href =
                "/bookService/Frontend/HTML/login.html";

            return;
        }

        if (!response.ok) {

            throw new Error(
                "Unable to update profile."
            );
        }

        const updatedProfile =
            await response.json();

        populateProfile(updatedProfile);

        setEditMode(false);

        // Keep dashboard welcome name updated
        if (updatedProfile.fullName) {

            localStorage.setItem(
                USER_NAME_KEY,
                updatedProfile.fullName
            );
        }

        showMessage(
            "Profile updated successfully.",
            "success"
        );

    } catch (error) {

        console.error(
            "Profile update error:",
            error
        );

        showMessage(
            "Unable to update your profile. Please try again.",
            "error"
        );
    }
}


// Edit

editProfileBtn.addEventListener(
    "click",
    () => {

        profileMessage.classList.add(
            "hidden"
        );

        setEditMode(true);
    }
);


// Cancel

cancelProfileBtn.addEventListener(
    "click",
    () => {

        setEditMode(false);

        profileMessage.classList.add(
            "hidden"
        );

        loadProfile();
    }
);


// Submit

profileForm.addEventListener(
    "submit",
    event => {

        event.preventDefault();

        saveProfile();
    }
);


// Initialisation

if (checkCustomerLogin()) {

    loadProfile();
}