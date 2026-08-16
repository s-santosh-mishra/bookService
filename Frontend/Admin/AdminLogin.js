/* =================================
   ServiceHub Admin Login
================================= */

const adminLoginForm =
    document.getElementById("adminLoginForm");

const adminId =
    document.getElementById("adminId");

const adminPassword =
    document.getElementById("adminPassword");

const togglePassword =
    document.getElementById("togglePassword");

const adminIdError =
    document.getElementById("adminIdError");

const passwordError =
    document.getElementById("passwordError");

const adminMessage =
    document.getElementById("adminMessage");

const adminLoginButton =
    document.getElementById("adminLoginButton");



/* =================================
   Password Visibility
================================= */

togglePassword.addEventListener("click", () => {

    const isPassword =
        adminPassword.type === "password";


    adminPassword.type =
        isPassword ? "text" : "password";


    togglePassword.innerHTML =
        isPassword
            ? '<i class="fa-solid fa-eye-slash"></i>'
            : '<i class="fa-solid fa-eye"></i>';

});



/* =================================
   Admin Login
================================= */

adminLoginForm.addEventListener(
    "submit",
    async (event) => {

        event.preventDefault();

        clearErrors();


        const idValue =
            adminId.value.trim();

        const passwordValue =
            adminPassword.value;


        let isValid = true;


        /* Admin ID */

        if (!idValue) {

            showError(
                adminId,
                adminIdError,
                "Please enter your admin ID."
            );

            isValid = false;
        }


        /* Password */

        if (!passwordValue) {

            showError(
                adminPassword,
                passwordError,
                "Please enter your password."
            );

            isValid = false;
        }


        if (!isValid) {
            return;
        }



        /*
        =================================
        BACKEND CONNECTION - LATER
        =================================

        const response = await fetch(
            "http://localhost:5000/api/admin/login",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    adminId: idValue,
                    password: passwordValue
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            showMessage(result.message, "error");
            return;
        }

        window.location.href = "admin-dashboard.html";

        */


        /* Temporary frontend simulation */

        adminLoginButton.disabled = true;

        adminLoginButton.innerHTML = `
            <i class="fa-solid fa-spinner fa-spin"></i>
            <span>Signing in...</span>
        `;


        setTimeout(() => {

            showMessage(
                "Admin login is ready. Backend authentication will be connected next.",
                "success"
            );


            adminLoginButton.disabled = false;

            adminLoginButton.innerHTML = `
                <i class="fa-solid fa-right-to-bracket"></i>
                <span>Admin Login</span>
            `;

        }, 800);

    }
);



/* =================================
   Helpers
================================= */

function showError(
    input,
    errorElement,
    message
) {

    input.classList.add("input-error");

    errorElement.textContent =
        message;

    errorElement.classList.remove("hidden");

}


function clearErrors() {

    adminId.classList.remove("input-error");

    adminPassword.classList.remove("input-error");

    adminIdError.classList.add("hidden");

    passwordError.classList.add("hidden");

    adminMessage.classList.add("hidden");

}


function showMessage(
    message,
    type
) {

    adminMessage.textContent =
        message;

    adminMessage.className =
        `mb-5 rounded-lg border px-4 py-3 text-sm ${type}`;

}