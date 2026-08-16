/* =================================
   ServiceHub Customer Registration
================================= */

const registerForm = document.getElementById("registerForm");

const fullName = document.getElementById("fullName");
const age = document.getElementById("age");
const gender = document.getElementById("gender");
const email = document.getElementById("email");
const phone = document.getElementById("phone");

const addressLine1 = document.getElementById("addressLine1");
const city = document.getElementById("city");
const state = document.getElementById("state");
const pincode = document.getElementById("pincode");

const password = document.getElementById("password");
const confirmPassword = document.getElementById("confirmPassword");

const terms = document.getElementById("terms");

const togglePassword = document.getElementById("togglePassword");
const toggleConfirmPassword =
    document.getElementById("toggleConfirmPassword");

const registerButton =
    document.getElementById("registerButton");

const registerMessage =
    document.getElementById("registerMessage");



/* =================================
   Password Visibility
================================= */

togglePassword.addEventListener("click", () => {

    togglePasswordVisibility(
        password,
        togglePassword
    );

});


toggleConfirmPassword.addEventListener("click", () => {

    togglePasswordVisibility(
        confirmPassword,
        toggleConfirmPassword
    );

});


function togglePasswordVisibility(input, button) {

    const isPassword =
        input.type === "password";

    input.type =
        isPassword ? "text" : "password";

    button.innerHTML =
        isPassword
            ? '<i class="fa-solid fa-eye-slash"></i>'
            : '<i class="fa-solid fa-eye"></i>';

}



/* =================================
   Phone / PIN Input
================================= */

phone.addEventListener("input", () => {

    phone.value =
        phone.value.replace(/\D/g, "").slice(0, 10);

});


pincode.addEventListener("input", () => {

    pincode.value =
        pincode.value.replace(/\D/g, "").slice(0, 6);

});


age.addEventListener("input", () => {

    if (age.value.length > 3) {
        age.value = age.value.slice(0, 3);
    }

});



/* =================================
   Registration
================================= */

registerForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    clearErrors();

    const isValid = validateForm();

    if (!isValid) {
        return;
    }


    const formData = {

        name: fullName.value.trim(),

        age: Number(age.value),

        gender: gender.value,

        email: email.value.trim() || null,

        phone: phone.value.trim(),

        address: {

            line1: addressLine1.value.trim(),

            line2:
                document.getElementById("addressLine2")
                    .value.trim() || null,

            landmark:
                document.getElementById("landmark")
                    .value.trim() || null,

            city: city.value.trim(),

            state: state.value,

            pincode: pincode.value.trim()

        },

        password: password.value

    };


    /*
        =================================
        BACKEND CONNECTION - LATER
        =================================

        Example:

        const response = await fetch(
            "http://localhost:5000/api/auth/register",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify(formData)
            }
        );

        const result = await response.json();

        if (!response.ok) {
            showMessage(result.message, "error");
            return;
        }

        window.location.href = "login.html";

    */


    registerButton.disabled = true;

    registerButton.innerHTML = `
        <i class="fa-solid fa-spinner fa-spin"></i>
        <span>Creating account...</span>
    `;


    /*
        Temporary frontend simulation.
        Remove when backend is connected.
    */

    setTimeout(() => {

        showMessage(
            "Registration form is ready. Backend connection will be added next.",
            "success"
        );

        registerButton.disabled = false;

        registerButton.innerHTML = `
            <i class="fa-solid fa-user-plus"></i>
            <span>Create Account</span>
        `;

    }, 800);

});



/* =================================
   Validation
================================= */

function validateForm() {

    let valid = true;


    // Full Name

    if (!fullName.value.trim()) {

        showError(
            fullName,
            "fullNameError",
            "Please enter your full name."
        );

        valid = false;
    }


    // Age

    const ageValue = Number(age.value);

    if (!age.value) {

        showError(
            age,
            "ageError",
            "Please enter your age."
        );

        valid = false;

    } else if (ageValue < 18 || ageValue > 100) {

        showError(
            age,
            "ageError",
            "Age must be between 18 and 100."
        );

        valid = false;
    }


    // Gender

    if (!gender.value) {

        showError(
            gender,
            "genderError",
            "Please select your gender."
        );

        valid = false;
    }


    // Email - optional

    if (
        email.value.trim() &&
        !isValidEmail(email.value.trim())
    ) {

        showError(
            email,
            "emailError",
            "Please enter a valid email address."
        );

        valid = false;
    }


    // Phone

    if (!phone.value.trim()) {

        showError(
            phone,
            "phoneError",
            "Please enter your phone number."
        );

        valid = false;

    } else if (!/^[6-9]\d{9}$/.test(phone.value.trim())) {

        showError(
            phone,
            "phoneError",
            "Please enter a valid 10-digit mobile number."
        );

        valid = false;
    }


    // Address Line 1

    if (!addressLine1.value.trim()) {

        showError(
            addressLine1,
            "addressLine1Error",
            "Please enter your address."
        );

        valid = false;
    }


    // City

    if (!city.value.trim()) {

        showError(
            city,
            "cityError",
            "Please enter your city."
        );

        valid = false;
    }


    // State

    if (!state.value) {

        showError(
            state,
            "stateError",
            "Please select your state."
        );

        valid = false;
    }


    // PIN

    if (!pincode.value.trim()) {

        showError(
            pincode,
            "pincodeError",
            "Please enter your PIN code."
        );

        valid = false;

    } else if (!/^\d{6}$/.test(pincode.value.trim())) {

        showError(
            pincode,
            "pincodeError",
            "Please enter a valid 6-digit PIN code."
        );

        valid = false;
    }


    // Password

    if (!password.value) {

        showError(
            password,
            "passwordError",
            "Please create a password."
        );

        valid = false;

    } else if (password.value.length < 8) {

        showError(
            password,
            "passwordError",
            "Password must contain at least 8 characters."
        );

        valid = false;
    }


    // Confirm password

    if (!confirmPassword.value) {

        showError(
            confirmPassword,
            "confirmPasswordError",
            "Please confirm your password."
        );

        valid = false;

    } else if (
        password.value !== confirmPassword.value
    ) {

        showError(
            confirmPassword,
            "confirmPasswordError",
            "Passwords do not match."
        );

        valid = false;
    }


    // Terms

    if (!terms.checked) {

        const termsError =
            document.getElementById("termsError");

        termsError.textContent =
            "Please accept the Terms & Conditions to continue.";

        termsError.classList.remove("hidden");

        valid = false;
    }


    return valid;

}



/* =================================
   Helper Functions
================================= */

function showError(input, errorId, message) {

    input.classList.add("input-error");

    const error =
        document.getElementById(errorId);

    error.textContent = message;

    error.classList.remove("hidden");

}


function clearErrors() {

    const inputs =
        registerForm.querySelectorAll(
            ".form-input"
        );

    inputs.forEach(input => {
        input.classList.remove("input-error");
    });


    const errors =
        registerForm.querySelectorAll(
            ".error-message"
        );

    errors.forEach(error => {
        error.classList.add("hidden");
        error.textContent = "";
    });


    registerMessage.classList.add("hidden");

}


function isValidEmail(value) {

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

}


function showMessage(message, type) {

    registerMessage.textContent = message;

    registerMessage.className =
        `mt-6 rounded-lg border px-4 py-3 text-sm ${type}`;

}