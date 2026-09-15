/*   ServiceHub Customer Header*/

const customerHeader = `
<header class="sticky top-0 z-50 bg-gray-950/90 backdrop-blur-md border-b border-gray-800">
    <nav class="max-w-7xl mx-auto px-6 py-4">

        <div class="flex items-center justify-between">

            <!-- Logo + Company -->
            <a href="CustomerDashboard.html" class="flex items-center gap-3">

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

                <a href="CustomerDashboard.html"
                   class="nav-btn">
                    Home
                </a>

                <a href="CustomerBookings.html"
                   class="nav-btn">
                    My Bookings
                </a>

                <a href="CustomerProfile.html"
                   class="nav-btn">
                    Profile
                </a>

                <button id="logoutButton"
                    class="nav-btn items-center">
                    <i class="fa-solid fa-arrow-right-from-bracket mr-2"></i>
                    Logout
                </button>

            </div>


            <!-- Mobile Menu Button -->
            <button id="mobileMenuButton"
                    class="lg:hidden border border-gray-700 rounded-full
                           px-4 py-2 text-gray-200 hover:bg-gray-800">
                ☰
            </button>

        </div>


        <!-- Mobile Navigation -->
        <div id="mobileMenu"
             class="lg:hidden mt-4 pb-3 space-y-2 max-h-0 opacity-0
                    overflow-hidden transition-all duration-300
                    ease-in-out">

            <a href="CustomerDashboard.html"
               class="mobile-nav-btn">
                Home
            </a>

            <a href="CustomerBookings.html"
               class="mobile-nav-btn">
                My Bookings
            </a>

            <a href="CustomerProfile.html"
               class="mobile-nav-btn">
                Profile
            </a>

            <button id="mobileLogoutButton"
                    class="mobile-nav-btn text-left flex items-center">
                <i class="fa-solid fa-arrow-right-from-bracket mr-2"></i>
                Logout
            </button>

        </div>

    </nav>
</header>
`;


/*   Insert Header*/

const headerContainer = document.getElementById("customerHeader");

if (headerContainer) {
    headerContainer.innerHTML = customerHeader;
}


/*   Highlight Current Page*/

const currentPage = window.location.pathname.split("/").pop();

document.querySelectorAll(".nav-btn, .mobile-nav-btn").forEach(link => {

    const linkPage = link.getAttribute("href");

    if (linkPage === currentPage) {
        link.classList.add("active");
    }

});


/*   Mobile Menu*/

const mobileMenuButton = document.getElementById("mobileMenuButton");
const mobileMenu = document.getElementById("mobileMenu");

if (mobileMenuButton && mobileMenu) {

    mobileMenuButton.addEventListener("click", () => {

        const isOpen = mobileMenu.classList.contains("max-h-96");

        if (isOpen) {

            mobileMenu.classList.remove("max-h-96", "opacity-100");
            mobileMenu.classList.add("max-h-0", "opacity-0");

        } else {

            mobileMenu.classList.remove("max-h-0", "opacity-0");
            mobileMenu.classList.add("max-h-96", "opacity-100");

        }

    });

}


/*   Customer Logout*/

function customerLogout() {

    const confirmed = confirm("Are you sure you want to log out?");

    if (!confirmed) {
        return;
    }

    localStorage.removeItem("servicehub_access_token");
    localStorage.removeItem("servicehub_user_id");
    localStorage.removeItem("servicehub_user_email");
    localStorage.removeItem("servicehub_user_role");

    window.location.href = "login.html";
}


const logoutButton = document.getElementById("logoutButton");
const mobileLogoutButton = document.getElementById("mobileLogoutButton");

if (logoutButton) {
    logoutButton.addEventListener("click", customerLogout);
}

if (mobileLogoutButton) {
    mobileLogoutButton.addEventListener("click", customerLogout);
}