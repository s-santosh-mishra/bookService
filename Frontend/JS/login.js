/* ================================
   ServiceHub Login
================================ */

const customerRole = document.getElementById("customerRole");
const workerRole = document.getElementById("workerRole");

const loginForm = document.getElementById("loginForm");

const idLabel = document.getElementById("idLabel");
const userId = document.getElementById("userId");

const password = document.getElementById("password");
const togglePassword = document.getElementById("togglePassword");

const idError = document.getElementById("idError");
const passwordError = document.getElementById("passwordError");

const loginMessage = document.getElementById("loginMessage");
const loginButton = document.getElementById("loginButton");


// Current selected role
let selectedRole = "customer";



/* =================================
   Role Selection
================================= */

customerRole.addEventListener("click", () => {

    selectedRole = "customer";

    customerRole.classList.add("active");
    workerRole.classList.remove("active");

    idLabel.textContent = "Email ID";

    userId.placeholder = "Enter your Email ID";

    clearValidation();

});


workerRole.addEventListener("click", () => {

    selectedRole = "worker";

    workerRole.classList.add("active");
    customerRole.classList.remove("active");

    idLabel.textContent = "Email ID";

    userId.placeholder = "Enter your Email ID";

    clearValidation();

});



/* =================================
   Password Visibility
================================= */

togglePassword.addEventListener("click", () => {

    const isPassword =
        password.type === "password";

    password.type =
        isPassword ? "text" : "password";


    togglePassword.innerHTML =
        isPassword
            ? '<i class="fa-solid fa-eye-slash"></i>'
            : '<i class="fa-solid fa-eye"></i>';

});



/* =================================
   Login Form
================================= */

loginForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    clearValidation();

    const idValue = userId.value.trim();
    const passwordValue = password.value.trim();

    let isValid = true;


    /* ID validation */

    if (!idValue) {

        showError(
            userId,
            idError,
            "Email ID is required."
        );

        isValid = false;
    }


    /* Password validation */

    if (!passwordValue) {

        showError(
            password,
            passwordError,
            "Password is required."
        );

        isValid = false;
    }


    if (!isValid) {
        return;
    }


    loginButton.disabled = true;

    loginButton.innerHTML = `
        <i class="fa-solid fa-spinner fa-spin"></i>
        <span>Signing in...</span>
    `;


    try {

        const response = await fetch(
            "http://localhost:8080/api/auth/login",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: idValue,
                    password: passwordValue
                })
            }
        );

        let result;

        const contentType = response.headers.get("content-type");

        if (contentType && contentType.includes("application/json")) {
            result = await response.json();
        } else {
            result = await response.text();
        }


        if (!response.ok) {

            const errorMessage =
                typeof result === "object" && result !== null
                    ? result.error ||
                    result.message ||
                    "Login failed. Please try again."
                    : result;

            showMessage(
                errorMessage,
                "error"
            );

            return;
        }

        alert("Login successful!");
        loginForm.reset();

    } catch (error) {

        console.error("Login error:", error);

        showMessage(
            "Unable to connect to the server. Please make sure the backend is running.",
            "error"
        );

    } finally {

        loginButton.disabled = false;

        loginButton.innerHTML = `
        <i class="fa-solid fa-right-to-bracket"></i>
        <span>Login</span>
    `;

    }

});



/* =================================
   Validation Functions
================================= */

function showError(input, errorElement, message) {

    input.classList.add("input-error");

    errorElement.textContent = message;

    errorElement.classList.remove("hidden");

}


function clearValidation() {

    userId.classList.remove("input-error");
    password.classList.remove("input-error");

    idError.classList.add("hidden");
    passwordError.classList.add("hidden");

    loginMessage.classList.add("hidden");

}



function showMessage(message, type) {

    loginMessage.textContent = message;

    loginMessage.className =
        `mb-5 rounded-lg border px-4 py-3 text-sm ${type}`;

}