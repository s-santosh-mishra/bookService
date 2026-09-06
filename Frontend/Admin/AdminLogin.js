// ServiceHub Admin Login
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

// Password Visibility

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

// Admin Login

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
                "Please enter your email."
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

        // Backend Connection

        adminLoginButton.disabled = true;

        adminLoginButton.innerHTML = `
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


            const result = await response.json();

            // Backend Error

            if (!response.ok) {

                showMessage(
                    result.message || "Login failed.",
                    "error"
                );

                return;
            }
            // Check Admin Role

            if (result.role !== "ADMIN") {

                showMessage(
                    "Access denied. This account is not an administrator.",
                    "error"
                );

                return;
            }

            // Successful Admin Login

            showMessage(
                "Admin login successful!",
                "success"
            );


            adminLoginForm.reset();


            /*
             * Dashboard will be connected later.
             *
             * For now, we just confirm
             * that admin authentication works.
             */

            // window.location.href =
            //     "/bookService/Frontend/Admin/admin.html";


        } catch (error) {

            console.error(
                "Admin login error:",
                error
            );

            showMessage(
                "Unable to connect to the server. Please try again.",
                "error"
            );

        } finally {

            adminLoginButton.disabled = false;

            adminLoginButton.innerHTML = `
                <i class="fa-solid fa-right-to-bracket"></i>
                <span>Admin Login</span>
            `;

        }

    }
);

    // Helpers

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