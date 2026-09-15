const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";
const USER_NAME_KEY = "servicehub_user_name";


// Authentication

function checkWorkerLogin() {

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const role =
        localStorage.getItem(ROLE_KEY);


    if (!token || role !== "WORKER") {

        window.location.href =
            "/bookService/Frontend/HTML/login.html";

        return false;

    }

    return true;
}


// Worker Information

function loadWorkerInformation() {

    const workerName =
        localStorage.getItem(USER_NAME_KEY) || "Worker";

    const welcomeMessage =
        document.getElementById("welcomeMessage");

    if (welcomeMessage) {

        welcomeMessage.textContent =
            `Welcome back, ${workerName}!`;

    }

}

// Dashboard Initialisation

if (checkWorkerLogin()) {

    loadWorkerInformation();

}