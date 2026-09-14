const sidebar = document.getElementById("sidebar");
const sidebarOverlay = document.getElementById("sidebarOverlay");
const menuButton = document.getElementById("menuButton");
const menuIcon = document.getElementById("menuIcon");


function openSidebar() {

    sidebar.classList.remove("-translate-x-full");
    sidebarOverlay.classList.remove("hidden");

    menuIcon.classList.remove("fa-bars");
    menuIcon.classList.add("fa-xmark");
}


function closeSidebar() {

    sidebar.classList.add("-translate-x-full");
    sidebarOverlay.classList.add("hidden");

    menuIcon.classList.remove("fa-xmark");
    menuIcon.classList.add("fa-bars");
}


menuButton.addEventListener("click", () => {

    const isClosed =
        sidebar.classList.contains("-translate-x-full");

    if (isClosed) {
        openSidebar();
    } else {
        closeSidebar();
    }

});


sidebarOverlay.addEventListener("click", closeSidebar);


// Close sidebar when clicking a navigation link on mobile
sidebar.querySelectorAll("nav a").forEach(link => {

    link.addEventListener("click", () => {

        if (window.innerWidth < 1024) {
            closeSidebar();
        }

    });

});


// Reset sidebar when switching between mobile and desktop
window.addEventListener("resize", () => {

    if (window.innerWidth >= 1024) {

        sidebar.classList.remove("-translate-x-full");
        sidebarOverlay.classList.add("hidden");

        menuIcon.classList.remove("fa-xmark");
        menuIcon.classList.add("fa-bars");

    } else {

        sidebar.classList.add("-translate-x-full");

    }

});


// ================================
// ServiceHub Admin Dashboard
// Authentication Guard
// ================================

const ACCESS_TOKEN_KEY = "servicehub_access_token";
const ROLE_KEY = "servicehub_user_role";


// --------------------------------
// Check Admin Authentication
// --------------------------------

function checkAdminAuthentication() {

    const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
    const role = sessionStorage.getItem(ROLE_KEY);

    // No login session
    if (!token || role !== "ADMIN") {
        window.location.href = "AdminLogin.html";
        return false;
    }

    return true;
}


// --------------------------------
// Logout
// --------------------------------

function logoutAdmin() {

    sessionStorage.removeItem("servicehub_access_token");
    sessionStorage.removeItem("servicehub_user_id");
    sessionStorage.removeItem("servicehub_user_email");
    sessionStorage.removeItem("servicehub_user_role");

    window.location.href = "AdminLogin.html";
}


// --------------------------------
// Run Authentication Check
// --------------------------------

checkAdminAuthentication();
