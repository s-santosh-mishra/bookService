const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";

const adminSidebarHTML = `
<aside id="sidebar"
       class="fixed top-0 left-0 z-40
              w-64 h-full h-screen
              bg-gray-900 border-r border-gray-800
              transform -translate-x-full lg:translate-x-0
              transition-transform duration-300
              flex flex-col"
       style="height: 100dvh;">

    <div class="h-20 shrink-0 flex items-center px-6 border-b border-gray-800">

        <div class="w-10 h-10 rounded-lg bg-violet-600
                    flex items-center justify-center">

            <i class="fa-solid fa-screwdriver-wrench text-white"></i>

        </div>

        <div class="ml-3">
            <h1 class="text-lg font-bold">
                Service<span class="text-violet-400">Hub</span>
            </h1>

            <p class="text-xs text-gray-500">
                Administration
            </p>
        </div>

    </div>

    <nav class="flex-1 overflow-y-auto p-4 space-y-2 overscroll-contain">

        <a href="/Admin/AdminDashboard.html"
           data-page="AdminDashboard.html"
           class="admin-nav-link flex items-center gap-3 px-4 py-3
                  text-gray-400 hover:text-white hover:bg-gray-800
                  rounded-lg transition-colors">

            <i class="fa-solid fa-chart-line w-5"></i>
            <span>Dashboard</span>

        </a>

        <a href="/Admin/BookingManagement.html"
           data-page="BookingManagement.html"
           class="admin-nav-link flex items-center gap-3 px-4 py-3
                  text-gray-400 hover:text-white hover:bg-gray-800
                  rounded-lg transition-colors">

            <i class="fa-solid fa-calendar-check w-5"></i>
            <span>Booking Management</span>

        </a>

        <a href="/Admin/PaymentManagement.html"
           data-page="PaymentManagement.html"
           class="admin-nav-link flex items-center gap-3 px-4 py-3
                  text-gray-400 hover:text-white hover:bg-gray-800
                  rounded-lg transition-colors">

            <i class="fa-solid fa-credit-card w-5"></i>
            <span>Payment Management</span>

        </a>

        <a href="/Admin/WorkerSettlementManagement.html"
           data-page="WorkerSettlementManagement.html"
           class="admin-nav-link flex items-center gap-3 px-4 py-3
                  text-gray-400 hover:text-white hover:bg-gray-800
                  rounded-lg transition-colors">

            <i class="fa-solid fa-money-bill-transfer w-5"></i>
            <span>Settlement Management</span>

        </a>

        <a href="/Admin/CustomerManagement.html"
           data-page="CustomerManagement.html"
           class="admin-nav-link flex items-center gap-3 px-4 py-3
                  text-gray-400 hover:text-white hover:bg-gray-800
                  rounded-lg transition-colors">

            <i class="fa-solid fa-users w-5"></i>
            <span>Customer Management</span>

        </a>

        <a href="/Admin/WorkerManagement.html"
           data-page="WorkerManagement.html"
           class="admin-nav-link flex items-center gap-3 px-4 py-3
                  text-gray-400 hover:text-white hover:bg-gray-800
                  rounded-lg transition-colors">

            <i class="fa-solid fa-user-gear w-5"></i>
            <span>Worker Management</span>

        </a>

        <a href="/Admin/SystemManagement.html"
           data-page="SystemManagement.html"
           class="admin-nav-link flex items-center gap-3 px-4 py-3
                  text-gray-400 hover:text-white hover:bg-gray-800
                  rounded-lg transition-colors">

            <i class="fa-solid fa-sliders w-5"></i>
            <span>System Management</span>

        </a>

    </nav>

    <div class="shrink-0 p-4 border-t border-gray-800 bg-gray-900">

        <button id="logoutButton"
                class="w-full flex items-center gap-3 px-4 py-3
                       text-gray-400 hover:text-red-400
                       hover:bg-gray-800 rounded-lg
                       transition-colors duration-200">

            <i class="fa-solid fa-right-from-bracket w-5"></i>
            <span>Logout</span>

        </button>

    </div>

</aside>

<div id="sidebarOverlay"
     class="fixed inset-0 z-30 bg-black/60 hidden lg:hidden">
</div>
`;


const adminHeaderHTML = `
<header class="sticky top-0 z-20 bg-gray-950/90
               backdrop-blur-md border-b border-gray-800">

    <div class="px-4 sm:px-6 lg:px-8 py-4 sm:py-5">

        <div class="flex items-center justify-between gap-4">

            <div class="flex items-center gap-3 sm:gap-4 min-w-0">

                <button id="mobileMenuButton"
                        class="lg:hidden text-gray-400
                               hover:text-white transition-colors p-1"
                        aria-label="Toggle navigation menu">

                    <i class="fa-solid fa-bars text-xl"></i>

                </button>

                <div class="min-w-0">

                    <h1 id="adminPageTitle"
                        class="text-lg sm:text-xl lg:text-2xl font-semibold truncate">
                        Dashboard
                    </h1>

                    <p id="adminPageSubtitle"
                       class="text-xs sm:text-sm text-gray-500 mt-0.5 truncate hidden sm:block">
                        ServiceHub administration overview
                    </p>

                </div>

            </div>

            <div class="flex items-center gap-3 shrink-0">

                <div class="text-right">

                    <p class="text-sm font-semibold">
                        Administrator
                    </p>

                    <p id="adminEmail" class="text-xs text-gray-500 truncate max-w-[140px] sm:max-w-[220px] md:max-w-none">
                        admin@servicehub.com
                    </p>

                </div>

                <div class="w-10 h-10 sm:w-11 sm:h-11 rounded-full bg-violet-600/20
                            border border-violet-500/20
                            flex items-center justify-center shrink-0">

                    <i class="fa-solid fa-user-shield text-violet-400 text-sm sm:text-base"></i>

                </div>

            </div>

        </div>

    </div>

</header>
`;


function checkAdminAuthentication() {

    const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
    const role = sessionStorage.getItem(ROLE_KEY);

    if (!token || role !== "ADMIN") {
        window.location.href = "/Admin/adminLogin.html";
        return false;
    }

    return true;
}


function loadAdminComponents() {

    const sidebarContainer = document.getElementById("adminSidebar");
    const headerContainer = document.getElementById("adminHeader");

    if (sidebarContainer) {
        sidebarContainer.innerHTML = adminSidebarHTML;
    }

    if (headerContainer) {
        headerContainer.innerHTML = adminHeaderHTML;

        const adminEmail = headerContainer.querySelector("#adminEmail");

        if (adminEmail) {
            const email = sessionStorage.getItem("servicehub_user_email") ||
                          localStorage.getItem("servicehub_user_email") ||
                          "admin@servicehub.com";
            adminEmail.textContent = email;
            adminEmail.title = email;
        }
    }

    initializeAdminLayout();
}


function handleAdminLogout() {
    let confirm_logout = confirm("Do you want to end your administrator session?");
    if (!confirm_logout) return;
    sessionStorage.removeItem("servicehub_access_token");
    sessionStorage.removeItem("servicehub_user_id");
    sessionStorage.removeItem("servicehub_user_email");
    sessionStorage.removeItem("servicehub_user_role");

    window.location.href = "/Admin/adminLogin.html";
}


function initializeAdminLayout() {

    const sidebar = document.getElementById("sidebar");
    const mobileMenuButton = document.getElementById("mobileMenuButton");
    const sidebarOverlay = document.getElementById("sidebarOverlay");
    const logoutButton = document.getElementById("logoutButton");

    if (mobileMenuButton && sidebar && sidebarOverlay) {

        mobileMenuButton.addEventListener("click", () => {

            sidebar.classList.toggle("-translate-x-full");
            sidebarOverlay.classList.toggle("hidden");

        });

        sidebarOverlay.addEventListener("click", () => {

            sidebar.classList.add("-translate-x-full");
            sidebarOverlay.classList.add("hidden");

        });

    }


    if (logoutButton) {
        logoutButton.addEventListener("click", handleAdminLogout);
    }


    // Ensure page always starts at top upon navigation
    if ("scrollRestoration" in history) {
        history.scrollRestoration = "manual";
    }
    window.scrollTo(0, 0);

    // On navigation link click, scroll to top and close mobile/tablet drawer
    document.querySelectorAll(".admin-nav-link").forEach(link => {
        link.addEventListener("click", () => {
            window.scrollTo(0, 0);
            if (sidebar && !sidebar.classList.contains("-translate-x-full")) {
                sidebar.classList.add("-translate-x-full");
            }
            if (sidebarOverlay && !sidebarOverlay.classList.contains("hidden")) {
                sidebarOverlay.classList.add("hidden");
            }
        });
    });


    setActiveNavigation();
}


function setActiveNavigation() {

    const currentPage = window.location.pathname.split("/").pop();

    document.querySelectorAll(".admin-nav-link").forEach(link => {

        if (link.dataset.page === currentPage) {

            link.classList.add(
                "text-white",
                "bg-violet-600/15",
                "border",
                "border-violet-500/20"
            );

            link.classList.remove("text-gray-400");

            const icon = link.querySelector("i");

            if (icon) {
                icon.classList.add("text-violet-400");
            }

        }

    });

}


if (checkAdminAuthentication()) {
    loadAdminComponents();
}
