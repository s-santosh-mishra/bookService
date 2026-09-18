/*   ServiceHub Worker Header
*/

const workerHeader = `
<header class="sticky top-0 z-50 bg-gray-950/90 backdrop-blur-md border-b border-gray-800">

    <nav class="max-w-7xl mx-auto px-6 py-4">

        <div class="flex items-center justify-between">


            <!-- Logo + Company -->

            <a href="WorkerDashboard.html"
               class="flex items-center gap-3">

                <div class="w-12 h-12 rounded-xl border-2 border-violet-500
                            flex items-center justify-center overflow-hidden">

                    <span class="text-purple-400">

                        <i class="fa-solid fa-screwdriver-wrench text-3xl"></i>

                    </span>

                </div>


                <div>

                    <h1 class="text-xl font-bold tracking-tight">
                        ServiceHub
                    </h1>

                    <p class="text-xs text-gray-400">
                        Trusted Services, Anytime Anywhere
                    </p>

                </div>

            </a>



            <!-- Desktop Navigation -->

            <div class="hidden lg:flex items-center gap-3">

                <a href="WorkerDashboard.html"
                   class="worker-nav-btn">

                    Home

                </a>


                <a href="CompletedWork.html"
                   class="worker-nav-btn">

                    Completed Work

                </a>


                <a href="WorkerProfile.html"
                   class="worker-nav-btn">

                    Profile

                </a>

                <a href="ContactFaq.html"
                   class="worker-nav-btn">
                    Contact & FAQ
                </a>


                <!-- Logout -->

                <button id="workerLogoutButton"
                        class="worker-nav-btn items-center">

                    <i class="fa-solid fa-arrow-right-from-bracket mr-2"></i>

                    Logout

                </button>

            </div>



            <!-- Mobile Menu Button -->

            <button id="workerMobileMenuButton"
                    class="lg:hidden border border-gray-700 rounded-full
                           px-4 py-2 text-gray-200 hover:bg-gray-800">

                ☰

            </button>

        </div>



        <!-- Mobile Navigation -->

        <div id="workerMobileMenu"
             class="lg:hidden mt-4 pb-3 space-y-2 max-h-0 opacity-0
                    overflow-hidden transition-all duration-300
                    ease-in-out">

            <a href="WorkerDashboard.html"
               class="worker-mobile-nav-btn">
                Home
            </a>


            <a href="CompletedWork.html"
               class="worker-mobile-nav-btn">
                Completed Work
            </a>


            <a href="WorkerProfile.html"
               class="worker-mobile-nav-btn">
                Profile
            </a>

            <a href="ContactFaq.html"
               class="worker-mobile-nav-btn">
                Contact & FAQ
            </a>

            <button id="workerMobileLogoutButton"
                    class="worker-mobile-nav-btn text-left flex items-center">

                <i class="fa-solid fa-arrow-right-from-bracket mr-2"></i>

                Logout

            </button>

        </div>

    </nav>

</header>
`;



/*   Insert Header
*/

const workerHeaderContainer =
    document.getElementById("workerHeader");

if (workerHeaderContainer) {

    workerHeaderContainer.innerHTML =
        workerHeader;

}



/*   Highlight Current Page
*/

const currentWorkerPage =
    window.location.pathname.split("/").pop();

document
    .querySelectorAll(".worker-nav-btn, .worker-mobile-nav-btn")
    .forEach(link => {

        const linkPage =
            link.getAttribute("href");

        if (linkPage === currentWorkerPage) {

            link.classList.add("active");

        }

    });



/*   Mobile Menu
*/

const workerMobileMenuButton =
    document.getElementById("workerMobileMenuButton");

const workerMobileMenu =
    document.getElementById("workerMobileMenu");


if (workerMobileMenuButton && workerMobileMenu) {

    workerMobileMenuButton.addEventListener("click", () => {

        const isOpen =
            workerMobileMenu.classList.contains("max-h-96");


        if (isOpen) {

            workerMobileMenu.classList.remove(
                "max-h-96",
                "opacity-100"
            );

            workerMobileMenu.classList.add(
                "max-h-0",
                "opacity-0"
            );

        } else {

            workerMobileMenu.classList.remove(
                "max-h-0",
                "opacity-0"
            );

            workerMobileMenu.classList.add(
                "max-h-96",
                "opacity-100"
            );

        }

    });

}



/*   Worker Logout
*/

function workerLogout() {

    const confirmed =
        confirm("Are you sure you want to log out?");

    if (!confirmed) {
        return;
    }


    localStorage.removeItem("servicehub_access_token");
    localStorage.removeItem("servicehub_user_id");
    localStorage.removeItem("servicehub_user_email");
    localStorage.removeItem("servicehub_user_role");
    localStorage.removeItem("servicehub_user_name");


    window.location.href =
        "login.html";

}



const workerLogoutButton =
    document.getElementById("workerLogoutButton");

const workerMobileLogoutButton =
    document.getElementById("workerMobileLogoutButton");


if (workerLogoutButton) {

    workerLogoutButton.addEventListener(
        "click",
        workerLogout
    );

}


if (workerMobileLogoutButton) {

    workerMobileLogoutButton.addEventListener(
        "click",
        workerLogout
    );

}