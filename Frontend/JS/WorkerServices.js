const ACCESS_TOKEN_KEY = "servicehub_access_token";
const USER_ID_KEY = "servicehub_user_id";
const ROLE_KEY = "servicehub_user_role";

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

// Load Worker Services
async function loadWorkerServices() {

    const workerId =
        localStorage.getItem(USER_ID_KEY);

    const token =
        localStorage.getItem(ACCESS_TOKEN_KEY);

    const container =
        document.getElementById("myServicesContainer");


    if (!workerId || !token) {
        return;
    }


    try {

        const response = await fetch(
            `http://localhost:8080/api/worker/services/${workerId}`,
            {
                method: "GET",

                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );


        if (response.status === 401 ||
            response.status === 403) {

            localStorage.removeItem(ACCESS_TOKEN_KEY);
            localStorage.removeItem(USER_ID_KEY);
            localStorage.removeItem(ROLE_KEY);

            window.location.href =
                "/bookService/Frontend/HTML/login.html";

            return;
        }


        if (!response.ok) {

            throw new Error(
                "Failed to load worker services"
            );

        }


        const services =
            await response.json();


        displayWorkerServices(services);

    }

    catch (error) {

        console.error(
            "Error loading worker services:",
            error
        );

        container.innerHTML = `

            <div class="col-span-full
                        bg-gray-900
                        border border-gray-800
                        rounded-2xl
                        p-10 text-center">

                <div class="w-16 h-16 mx-auto mb-5
                            rounded-full bg-gray-800
                            flex items-center justify-center">

                    <i class="fa-solid fa-triangle-exclamation
                              text-gray-500 text-2xl"></i>

                </div>

                <h4 class="text-lg font-semibold mb-2">
                    Unable to load services
                </h4>

                <p class="text-gray-400 text-sm">
                    Please try again later.
                </p>

            </div>

        `;
    }
}

// Display Services
function displayWorkerServices(services) {

    const container =
        document.getElementById("myServicesContainer");


    container.innerHTML = "";


    if (!services || services.length === 0) {

        container.innerHTML = `

            <div class="col-span-full
                        bg-gray-900
                        border border-gray-800
                        rounded-2xl
                        p-10 text-center">

                <div class="w-16 h-16 mx-auto mb-5
                            rounded-full bg-gray-800
                            flex items-center justify-center">

                    <i class="fa-solid fa-screwdriver-wrench
                              text-gray-500 text-2xl"></i>

                </div>

                <h4 class="text-lg font-semibold mb-2">
                    No services assigned
                </h4>

                <p class="text-gray-400 text-sm">
                    Your assigned services will appear here.
                </p>

            </div>

        `;

        return;
    }


    services.forEach(service => {

        const card = document.createElement("div");

        card.className = "worker-service-card";


        card.innerHTML = `

            <div class="worker-service-icon">

                <i class="fa-solid fa-screwdriver-wrench"></i>

            </div>


            <div>

                <h4 class="text-lg font-semibold">
                    ${service.serviceName}
                </h4>

                <p class="text-sm text-gray-400 mt-1">
                    Service available for customer bookings
                </p>

            </div>

        `;


        container.appendChild(card);

    });
}

// Page Initialisation
if (checkWorkerLogin()) {

    loadWorkerServices();

}