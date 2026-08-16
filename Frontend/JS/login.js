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

    idLabel.textContent = "Customer ID";

    userId.placeholder = "Enter your customer ID";

    clearValidation();

});


workerRole.addEventListener("click", () => {

    selectedRole = "worker";

    workerRole.classList.add("active");
    customerRole.classList.remove("active");

    idLabel.textContent = "Worker ID";

    userId.placeholder = "Enter your worker ID";

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
            `${selectedRole === "customer"
                ? "Customer ID"
                : "Worker ID"} is required.`
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


    /*
        Backend connection will be added here.

        Example:

        const response = await fetch(
            "http://localhost:5000/api/auth/login",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    role: selectedRole,
                    userId: idValue,
                    password: passwordValue
                })
            }
        );

    */


    loginButton.disabled = true;

    loginButton.innerHTML = `
        <i class="fa-solid fa-spinner fa-spin"></i>
        <span>Signing in...</span>
    `;


    /*
        Temporary simulation.

        REMOVE this when backend is connected.
    */

    setTimeout(() => {

        showMessage(
            "Login is ready. Backend authentication will be connected next.",
            "success"
        );

        loginButton.disabled = false;

        loginButton.innerHTML = `
            <i class="fa-solid fa-right-to-bracket"></i>
            <span>Login</span>
        `;

    }, 800);

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