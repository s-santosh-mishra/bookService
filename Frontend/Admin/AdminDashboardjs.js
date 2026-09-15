// Get Logged-in Admin Email
const API_BASE_URL = "http://localhost:8080/api";
function loadAdminEmail() {

    const email = sessionStorage.getItem("servicehub_user_email");

    const adminEmailElement =
        document.getElementById("adminEmail");

    if (adminEmailElement) {

        adminEmailElement.textContent =
            email || "Admin account";

    }
}


// Update Status Display

function updateStatus(elementId, dotId, status, successStatus) {

    const statusElement =
        document.getElementById(elementId);

    const dotElement =
        document.getElementById(dotId);

    if (!statusElement || !dotElement) {
        return;
    }


    statusElement.textContent = status;


    if (status === successStatus) {

        statusElement.classList.remove(
            "text-gray-400",
            "text-red-400"
        );

        statusElement.classList.add(
            "text-green-400"
        );

        dotElement.classList.remove(
            "bg-gray-500",
            "bg-red-400"
        );

        dotElement.classList.add(
            "bg-green-400"
        );

    } else {

        statusElement.classList.remove(
            "text-gray-400",
            "text-green-400"
        );

        statusElement.classList.add(
            "text-red-400"
        );

        dotElement.classList.remove(
            "bg-gray-500",
            "bg-green-400"
        );

        dotElement.classList.add(
            "bg-red-400"
        );
    }
}


// Load Dashboard Data

async function loadDashboardData() {

    const token =
    sessionStorage.getItem("servicehub_access_token");


    if (!token) {

        window.location.href = "AdminLogin.html";
        return;

    }


    try {

        const response = await fetch(
            `${API_BASE_URL}/admin/dashboard`,
            {
                method: "GET",

                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );


    
        // Session / Authorization failure
    

        if (response.status === 401 ||
            response.status === 403) {

            sessionStorage.clear();

            window.location.href = "AdminLogin.html";

            return;
        }


        if (!response.ok) {

            throw new Error(
                "Failed to load dashboard data."
            );
        }


        const data = await response.json();


    
        // Update Counts
    

        document.getElementById("totalCustomers")
            .textContent = data.totalCustomers;

        document.getElementById("totalWorkers")
            .textContent = data.totalWorkers;

        document.getElementById("pendingWorkers")
            .textContent = data.pendingWorkers;

        document.getElementById("verifiedWorkers")
            .textContent = data.verifiedWorkers;

        document.getElementById("totalCategories")
            .textContent = data.totalCategories;

        document.getElementById("totalServices")
            .textContent = data.totalServices;


    
        // Update System Status
    

        updateStatus(
            "backendStatus",
            "backendStatusDot",
            data.backendStatus,
            "Online"
        );


        updateStatus(
            "databaseStatus",
            "databaseStatusDot",
            data.databaseStatus,
            "Connected"
        );


        updateStatus(
            "authenticationStatus",
            "authenticationStatusDot",
            data.authenticationStatus,
            "Active"
        );


    } catch (error) {

        console.error(
            "Dashboard error:",
            error
        );


        updateStatus(
            "backendStatus",
            "backendStatusDot",
            "Offline",
            "Online"
        );


        updateStatus(
            "databaseStatus",
            "databaseStatusDot",
            "Unavailable",
            "Connected"
        );


        updateStatus(
            "authenticationStatus",
            "authenticationStatusDot",
            "Unavailable",
            "Active"
        );
    }
}


// Initialize Dashboard

loadAdminEmail();
loadDashboardData();