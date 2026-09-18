// ServiceHub Customer Registration

const registerForm = document.getElementById("registerForm");

const fullName = document.getElementById("fullName");
const age = document.getElementById("age");
const gender = document.getElementById("gender");
const email = document.getElementById("email");
const phone = document.getElementById("phone");

const addressLine1 = document.getElementById("addressLine1");
const addressLine2 = document.getElementById("addressLine2");
const landmark = document.getElementById("landmark");
const city = document.getElementById("city");
const state = document.getElementById("state");
const pincode = document.getElementById("pincode");

const password = document.getElementById("password");
const confirmPassword = document.getElementById("confirmPassword");

const terms = document.getElementById("terms");

const togglePassword =
    document.getElementById("togglePassword");

const toggleConfirmPassword =
    document.getElementById("toggleConfirmPassword");

const registerButton =
    document.getElementById("registerButton");

const registerMessage =
    document.getElementById("registerMessage");


// Password Visibility

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


// Input Restrictions

phone.addEventListener("input", () => {

    phone.value =
        phone.value.replace(/\D/g, "").slice(0, 10);

});


pincode.addEventListener("input", () => {

    pincode.value =
        pincode.value.replace(/\D/g, "").slice(0, 6);

});


age.addEventListener("input", () => {

    age.value =
        age.value.replace(/\D/g, "").slice(0, 3);

});


// Form Submission

registerForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    clearErrors();

    if (!validateForm()) {
        return;
    }


    const formData = {

        fullName:
            fullName.value.trim(),

        age:
            Number(age.value),

        gender:
            gender.value.toUpperCase(),

        email:
            email.value.trim(),

        phone:
            phone.value.trim(),

        addressLine1:
            addressLine1.value.trim(),

        addressLine2:
            addressLine2.value.trim() || null,

        landmark:
            landmark.value.trim() || null,

        city:
            city.value.trim(),

        state:
            state.value,

        pinCode:
            pincode.value.trim(),

        password:
            password.value,

        confirmPassword:
            confirmPassword.value,

        termsAccepted:
            terms.checked
    };


    // Loading State

    setLoading(true);


    try {

        const response = await fetch(
            "http://localhost:8080/api/user/register",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify(formData)
            }
        );


        const result =
            await readResponse(response);


        if (!response.ok) {

            showMessage(
                getBackendError(result),
                "error"
            );

            return;
        }


        // Success

        registerForm.reset();

        showSuccess();


    } catch (error) {

        console.error(
            "Registration error:",
            error
        );

        showMessage(
            "Unable to connect to the server. Please make sure the backend is running.",
            "error"
        );

    } finally {

        setLoading(false);

    }

});


// Validation

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

    const ageValue =
        Number(age.value);

    if (!age.value) {

        showError(
            age,
            "ageError",
            "Please enter your age."
        );

        valid = false;

    } else if (
        ageValue < 18 ||
        ageValue > 100
    ) {

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


    // Email

    if (!email.value.trim()) {

        showError(
            email,
            "emailError",
            "Please enter your email address."
        );

        valid = false;

    } else if (
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

    } else if (
        !/^[6-9]\d{9}$/.test(
            phone.value.trim()
        )
    ) {

        showError(
            phone,
            "phoneError",
            "Please enter a valid 10-digit mobile number."
        );

        valid = false;

    }


    // Address

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

    } else if (
        !/^\d{6}$/.test(
            pincode.value.trim()
        )
    ) {

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

    } else if (
        password.value.length < 8
    ) {

        showError(
            password,
            "passwordError",
            "Password must contain at least 8 characters."
        );

        valid = false;

    }


    // Confirm Password

    if (!confirmPassword.value) {

        showError(
            confirmPassword,
            "confirmPasswordError",
            "Please confirm your password."
        );

        valid = false;

    } else if (
        password.value !==
        confirmPassword.value
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

        const error =
            document.getElementById("termsError");

        error.textContent =
            "Please accept the Terms & Conditions to continue.";

        error.classList.remove("hidden");

        valid = false;

    }


    return valid;

}


// Loading

function setLoading(loading) {

    registerButton.disabled = loading;


    if (loading) {

        registerButton.innerHTML = `
            <i class="fa-solid fa-spinner fa-spin"></i>
            <span>Creating account...</span>
        `;

    } else {

        registerButton.innerHTML = `
            <i class="fa-solid fa-user-plus"></i>
            <span>Create Account</span>
        `;

    }

}


// Success Message

function showSuccess() {

    let countdown = 7;


    registerMessage.className =
        "mt-6 rounded-lg border border-green-500/30 " +
        "bg-green-500/10 px-4 py-3 text-sm text-green-400";


    registerMessage.innerHTML = `
        <div class="text-center">

            <p class="font-medium">
                Registration successful!
            </p>

            <p class="mt-1">
                You can now
                <a
                    href="../HTML/login.html"
                    class="text-purple-400 hover:text-purple-300 font-medium"
                >
                    Login
                </a>
                to your account.
            </p>

            <p class="mt-2 text-gray-400">

                Redirecting to login in
                <span id="countdown">
                    ${countdown}
                </span>
                seconds...

            </p>

        </div>
    `;


    const countdownElement =
        document.getElementById("countdown");


    const countdownTimer =
        setInterval(() => {

            countdown--;

            countdownElement.textContent =
                countdown;


            if (countdown <= 0) {

                clearInterval(countdownTimer);

                window.location.href =
                    "../HTML/login.html";

            }

        }, 1000);

}


// Error / Message Helpers

function showError(
    input,
    errorId,
    message
) {

    input.classList.add("input-error");

    const error =
        document.getElementById(errorId);

    error.textContent =
        message;

    error.classList.remove("hidden");

}


function clearErrors() {

    registerForm
        .querySelectorAll(".form-input")
        .forEach(input => {

            input.classList.remove(
                "input-error"
            );

        });


    registerForm
        .querySelectorAll(".error-message")
        .forEach(error => {

            error.classList.add("hidden");

            error.textContent = "";

        });


    registerMessage.classList.add("hidden");

    registerMessage.textContent = "";

}


function showMessage(
    message,
    type
) {

    if (type === "error") {

        registerMessage.className =
            "mt-6 rounded-lg border border-red-500/30 " +
            "bg-red-500/10 px-4 py-3 text-sm text-red-400";

    }

    registerMessage.textContent =
        message;

}


async function readResponse(response) {

    const contentType =
        response.headers.get("content-type");


    if (
        contentType &&
        contentType.includes("application/json")
    ) {

        return await response.json();

    }


    return await response.text();

}


function getBackendError(result) {

    if (
        typeof result === "object" &&
        result !== null
    ) {

        return (
            result.error ||
            result.message ||
            "Registration failed. Please try again."
        );

    }


    return (
        result ||
        "Registration failed. Please try again."
    );

}


function isValidEmail(value) {

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

}