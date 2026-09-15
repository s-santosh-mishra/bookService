/*   Authentication Guard*/

const accessToken = localStorage.getItem(
    "servicehub_access_token"
);

const userId = localStorage.getItem(
    "servicehub_user_id"
);

const userRole = localStorage.getItem(
    "servicehub_user_role"
);

if (!accessToken || !userId || userRole !== "USER") {

    window.location.href = "login.html";

}

/*   Dashboard Elements*/

const welcomeMessage =
    document.getElementById("welcomeMessage");

const serviceSearch =
    document.getElementById("serviceSearch");

const categoryContainer =
    document.getElementById("categoryContainer");

const popularServicesContainer =
    document.getElementById(
        "popularServicesContainer"
    );


/*   Active Booking Elements*/

const activeBookingCard =
    document.getElementById(
        "activeBookingCard"
    );

const noActiveBooking =
    document.getElementById(
        "noActiveBooking"
    );

const activeBookingService =
    document.getElementById(
        "activeBookingService"
    );

const activeBookingStatus =
    document.getElementById(
        "activeBookingStatus"
    );

const activeBookingDescription =
    document.getElementById(
        "activeBookingDescription"
    );

const activeBookingWorker =
    document.getElementById(
        "activeBookingWorker"
    );

const activeBookingDate =
    document.getElementById(
        "activeBookingDate"
    );

const activeBookingTime =
    document.getElementById(
        "activeBookingTime"
    );

const viewActiveBookingButton =
    document.getElementById(
        "viewActiveBookingButton"
    );


/*   API*/

const API_BASE_URL =
    "http://localhost:8080/api/customer";


/*   Dashboard Data*/

let customerServices = [];
let selectedCategoryId = null;


/*   Category Icons*/

const categoryIconMap = {

    plumbing: "fa-faucet-drip",

    electrical: "fa-bolt",

    cleaning: "fa-broom",

    repair: "fa-screwdriver-wrench",

    "ac service": "fa-snowflake",

    "home services": "fa-house"

};


/*   Service Icons*/

const serviceIconMap = {

    pipe: "fa-faucet",

    plumbing: "fa-faucet-drip",

    fan: "fa-fan",

    ac: "fa-snowflake",

    electrical: "fa-bolt",

    cleaning: "fa-broom",

    repair: "fa-screwdriver-wrench"

};


/*   Get Category Icon*/

function getCategoryIcon(categoryName) {

    const name =
        categoryName.toLowerCase().trim();

    return categoryIconMap[name]
        || "fa-house";

}


/*   Get Service Icon*/

function getServiceIcon(serviceName) {

    const name =
        serviceName.toLowerCase();

    for (const keyword in serviceIconMap) {

        if (name.includes(keyword)) {
            return serviceIconMap[keyword];
        }

    }

    return "fa-screwdriver-wrench";

}


/*   Load Customer Name*/

function loadCustomerName() {

    const customerName =
        localStorage.getItem(
            "servicehub_user_name"
        );

    if (customerName) {

        welcomeMessage.textContent =
            `Welcome back, ${customerName}!`;

    } else {

        welcomeMessage.textContent =
            "Welcome back!";

    }

}


/*   Load Categories*/

async function loadCustomerCategories() {

    try {

        const response = await fetch(
            `${API_BASE_URL}/categories`,
            {
                method: "GET",

                headers: {
                    "Authorization":
                        `Bearer ${accessToken}`
                }
            }
        );


        if (response.status === 401) {

            handleUnauthorized();
            return;

        }


        if (!response.ok) {

            throw new Error(
                "Failed to load categories."
            );

        }


        const categories =
            await response.json();


        renderCategories(categories);


    } catch (error) {

        console.error(
            "Category loading error:",
            error
        );

    }

}


/*   Render Categories*/

function renderCategories(categories) {

    categoryContainer.innerHTML = "";


    if (categories.length === 0) {

        categoryContainer.innerHTML = `
            <p class="col-span-full
                      text-center
                      text-gray-500
                      py-8">
                No services are currently available.
            </p>
        `;

        return;

    }


    categories.forEach(category => {

        const categoryButton =
            document.createElement("button");


        categoryButton.className =
            "category-card bg-gray-900 " +
            "border border-gray-800 " +
            "rounded-2xl p-5 text-center " +
            "hover:border-violet-500/50 " +
            "hover:bg-gray-900/80 " +
            "transition";


        categoryButton.dataset.categoryId =
            category.categoryId;


        categoryButton.innerHTML = `

            <i class="fa-solid
                      ${getCategoryIcon(
                          category.categoryName
                      )}
                      text-2xl
                      text-violet-400
                      mb-3"></i>

            <p class="font-medium">
                ${escapeHtml(
                    category.categoryName
                )}
            </p>

        `;


        categoryButton.addEventListener(
            "click",
            () => {

                if (
                    selectedCategoryId ===
                    category.categoryId
                ) {

                    selectedCategoryId = null;

                    categoryButton.classList.remove(
                        "border-violet-500",
                        "bg-violet-900/20"
                    );

                } else {

                    selectedCategoryId =
                        category.categoryId;


                    document
                        .querySelectorAll(
                            ".category-card"
                        )
                        .forEach(button => {

                            button.classList.remove(
                                "border-violet-500",
                                "bg-violet-900/20"
                            );

                        });


                    categoryButton.classList.add(
                        "border-violet-500",
                        "bg-violet-900/20"
                    );

                }


                renderPopularServices();

            }
        );


        categoryContainer.appendChild(
            categoryButton
        );

    });

}


/*   Load Services*/

async function loadCustomerServices() {

    try {

        const response = await fetch(
            `${API_BASE_URL}/services`,
            {
                method: "GET",

                headers: {
                    "Authorization":
                        `Bearer ${accessToken}`
                }
            }
        );


        if (response.status === 401) {

            handleUnauthorized();
            return;

        }


        if (!response.ok) {

            throw new Error(
                "Failed to load services."
            );

        }


        customerServices =
            await response.json();


        renderPopularServices();


    } catch (error) {

        console.error(
            "Service loading error:",
            error
        );

    }

}


/*   Get Random 6 Services*/

function getRandomServices(services) {

    const shuffled =
        [...services];


    for (
        let i = shuffled.length - 1;
        i > 0;
        i--
    ) {

        const randomIndex =
            Math.floor(
                Math.random() * (i + 1)
            );


        [
            shuffled[i],
            shuffled[randomIndex]
        ] = [
            shuffled[randomIndex],
            shuffled[i]
        ];

    }


    return shuffled.slice(0, 6);

}


/*   Render Popular Services*/

function renderPopularServices() {

    popularServicesContainer.innerHTML =
        "";


    let services =
        customerServices;


    /* Category filter */

    if (selectedCategoryId) {

        services =
            services.filter(service =>
                service.categoryId ===
                selectedCategoryId
            );

    }


    if (services.length === 0) {

        popularServicesContainer.innerHTML = `
            <p class="col-span-full
                      text-center
                      text-gray-500
                      py-8">
                No services are currently available
                in this category.
            </p>
        `;

        return;

    }


    /*
       Randomly select up to 6 services.
       Later this can be replaced with
       actual popularity from booking data.
    */

    const displayedServices =
        getRandomServices(services);


    displayedServices.forEach(service => {

        const serviceCard =
            document.createElement("div");


        serviceCard.className =
            "service-card bg-gray-900 " +
            "border border-gray-800 " +
            "rounded-2xl p-6 " +
            "hover:border-violet-500/50 " +
            "transition";


        serviceCard.dataset.serviceName =
            service.serviceName;

        serviceCard.dataset.categoryId =
            service.categoryId;


        serviceCard.innerHTML = `

            <div class="w-12 h-12
                        rounded-xl
                        bg-violet-900/30
                        flex items-center
                        justify-center
                        mb-5">

                <i class="fa-solid
                          ${getServiceIcon(
                              service.serviceName
                          )}
                          text-violet-400">
                </i>

            </div>


            <p class="text-xs
                      text-violet-400
                      mb-2">

                ${escapeHtml(
                    service.categoryName
                )}

            </p>


            <h4 class="text-lg
                       font-semibold
                       mb-2">

                ${escapeHtml(
                    service.serviceName
                )}

            </h4>


            <p class="text-sm
                      text-gray-400
                      mb-5">

                Professional service from
                trusted workers.

            </p>


            <button
                class="book-service-button
                       text-violet-400
                       text-sm
                       font-medium
                       hover:text-violet-300
                       transition"
                data-service-id="${service.serviceId}">

                Book Service

                <i class="fa-solid
                          fa-arrow-right
                          ml-2"></i>

            </button>

        `;


        popularServicesContainer.appendChild(
            serviceCard
        );

    });


    applyServiceSearch();

}


/*   Service Search*/

if (serviceSearch) {

    serviceSearch.addEventListener(
        "input",
        applyServiceSearch
    );

}


function applyServiceSearch() {

    const searchValue =
        serviceSearch.value
            .trim()
            .toLowerCase();


    document
        .querySelectorAll(".service-card")
        .forEach(card => {

            const serviceName =
                card.dataset.serviceName
                    .toLowerCase();


            const categoryName =
                card.textContent
                    .toLowerCase();


            if (
                serviceName.includes(
                    searchValue
                )
                ||
                categoryName.includes(
                    searchValue
                )
            ) {

                card.classList.remove(
                    "hidden"
                );

            } else {

                card.classList.add(
                    "hidden"
                );

            }

        });

}


/*   Active Booking*/

/*
   Booking API will be connected here
   when the booking module is created.

   Expected booking structure:

   {
       serviceName,
       status,
       workerName,
       scheduledDate,
       scheduledTime,
       description
   }
*/

async function loadActiveBooking() {

    /*
       No booking system exists yet,
       so there is currently no active booking.
    */

    const activeBooking = null;


    renderActiveBooking(
        activeBooking
    );

}


/*   Render Active Booking*/

function renderActiveBooking(booking) {

    if (!booking) {

        activeBookingCard.classList.add(
            "hidden"
        );

        noActiveBooking.classList.remove(
            "hidden"
        );

        return;

    }


    noActiveBooking.classList.add(
        "hidden"
    );

    activeBookingCard.classList.remove(
        "hidden"
    );


    activeBookingService.textContent =
        booking.serviceName;


    activeBookingStatus.textContent =
        booking.status;


    activeBookingDescription.textContent =
        booking.description ||
        "Your service request is currently being handled.";


    activeBookingWorker.textContent =
        booking.workerName ||
        "Worker not assigned";


    activeBookingDate.textContent =
        booking.scheduledDate ||
        "Date not scheduled";


    activeBookingTime.textContent =
        booking.scheduledTime ||
        "Time not scheduled";

}


/*   Active Booking Details*/

if (viewActiveBookingButton) {

    viewActiveBookingButton.addEventListener(
        "click",
        () => {

            console.log(
                "Active booking details clicked."
            );

            /*
               Booking details dialog/page
               will be implemented with
               the booking module.
            */

        }
    );

}


/*   Book Service*/

document.addEventListener(
    "click",
    event => {

        const button =
            event.target.closest(
                ".book-service-button"
            );


        if (!button) {
            return;
        }


        const serviceId =
            button.dataset.serviceId;


        console.log(
            "Selected service:",
            serviceId
        );


        /*
           Booking flow will be connected
           here later.
        */

    }
);


/*   Authentication Error*/

function handleUnauthorized() {

    localStorage.removeItem(
        "servicehub_access_token"
    );

    localStorage.removeItem(
        "servicehub_user_id"
    );

    localStorage.removeItem(
        "servicehub_user_name"
    );

    localStorage.removeItem(
        "servicehub_user_email"
    );

    localStorage.removeItem(
        "servicehub_user_role"
    );


    window.location.href =
        "login.html";

}


/*   HTML Escape Helper*/

function escapeHtml(value) {

    const div =
        document.createElement("div");

    div.textContent =
        value ?? "";

    return div.innerHTML;

}


/*   Initialize Dashboard*/

loadCustomerName();

loadCustomerCategories();

loadCustomerServices();

loadActiveBooking();