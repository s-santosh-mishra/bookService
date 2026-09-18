const role = localStorage.getItem("servicehub_user_role");

const faqContainer =
    document.getElementById("faqContainer");

const faqSubtitle =
    document.getElementById("faqSubtitle");


// ROLE-BASED HEADER

if (role === "USER") {

    document
        .getElementById("customerHeader")
        .classList.remove("hidden");

}
else if (role === "WORKER") {

    document
        .getElementById("workerHeader")
        .classList.remove("hidden");

}
else {

    window.location.href = "login.html";

}

// CUSTOMER FAQS

const customerFaqs = [

    {
        question: "How do I book a service?",
        answer: "Open the Customer Dashboard, choose the service you need, and create a booking request. You can include a note describing the problem or any useful information for the worker. You do not need to choose a worker yourself."
    },

    {
        question: "Can I choose a specific worker?",
        answer: "No. ServiceHub matches your request with workers who provide the requested service and are currently available. A suitable worker can then accept the request."
    },

    {
        question: "What happens after I create a booking?",
        answer: "Your booking starts as Pending. Suitable available workers can see the request and decide whether to accept or reject it. It remains pending until a worker accepts it or the request reaches the no-worker timeout."
    },

    {
        question: "What happens if no worker accepts my booking?",
        answer: "If no worker accepts the request within 30 minutes from when it was created, the booking becomes No Worker. You can then try booking the service again."
    },

    {
        question: "Can I cancel a booking?",
        answer: "You can cancel your own booking while it is Pending. Once a worker has accepted the booking, contact ServiceHub support if you need to cancel the service."
    },

    {
        question: "What does Accepted mean?",
        answer: "Accepted means a worker has taken responsibility for your booking. The worker is assigned to the booking and the service is waiting to be started."
    },

    {
        question: "What does In Progress mean?",
        answer: "In Progress means the assigned worker has started the service. The booking stays in this state while the service is being carried out."
    },

    {
        question: "How is a service marked as completed?",
        answer: "Completion requires confirmation from both sides. The worker confirms that the service is complete and you also confirm completion. Once both confirmations are received, the booking becomes Completed."
    },

    {
        question: "What if the service is not actually finished?",
        answer: "Do not confirm completion until the service is actually complete. If there is a problem with the work or the service is not finished, contact ServiceHub support."
    },

    {
        question: "How many active bookings can I have?",
        answer: "A customer can have up to 3 active bookings at a time. Active bookings are Pending, Accepted, and In Progress."
    },

    {
        question: "Can I book the same service again while I already have an active booking for it?",
        answer: "No. You cannot create another active booking for the same service while an existing booking for that service is still active."
    },

    {
        question: "Where can I see my previous bookings?",
        answer: "Use the Customer Bookings section to view your bookings and their current or completed status."
    },

    {
        question: "Can I edit my profile information?",
        answer: "Yes. You can update your personal information, phone number, and address information from your Customer Profile. Your registered email address is not editable from the profile."
    },

    {
        question: "What should I do if there is a problem with my booking?",
        answer: "Contact ServiceHub support and provide the relevant booking and service details. A clear description of the problem helps the support team understand the situation and assist you."
    },

    {
        question: "How are payments handled?",
        answer: "Payment information is associated with completed service transactions. Detailed payment functionality will be available as the payment feature is implemented."
    }

];


// WORKER FAQS

const workerFaqs = [

    {
        question: "How do I become a ServiceHub worker?",
        answer: "Register as a worker and provide the required personal and professional information. Your account then goes through verification before you can receive service requests."
    },

    {
        question: "What happens after I register?",
        answer: "Your worker account starts in the verification process. Once an administrator verifies your account, you can become available to receive suitable service requests."
    },

    {
        question: "Why do I need to be verified?",
        answer: "Worker verification is required before you can receive and accept service requests."
    },

    {
        question: "How do I add the services I provide?",
        answer: "Use the My Services section of the worker dashboard to manage the services you provide. Services shown in your profile are informational only."
    },

    {
        question: "Can I change my services from my Worker Profile?",
        answer: "No. Service management is handled separately through My Services."
    },

    {
        question: "How does worker availability work?",
        answer: "A verified worker who is Available can receive suitable requests. A worker who is Unavailable does not receive new requests."
    },

    {
        question: "Can I manually change my availability?",
        answer: "No. Availability is managed by the ServiceHub workflow. Accepting a service makes you Busy, and completing the active service makes you Available again."
    },

    {
        question: "Why am I not receiving booking requests?",
        answer: "You must be verified, available, and provide the requested service. If you are handling an active service, you are busy and cannot accept another active service."
    },

    {
        question: "What happens when I receive a booking request?",
        answer: "The request appears in your Booking Requests section with relevant customer and service information. You can review the request and either accept or reject it."
    },

    {
        question: "What happens if I reject a booking request?",
        answer: "Rejecting a request does not cancel the customer's booking. The booking remains Pending so another suitable worker can accept it."
    },

    {
        question: "What happens when I accept a booking?",
        answer: "The booking becomes Accepted and you become the assigned worker. You are then busy with that service and cannot accept another active service."
    },

    {
        question: "Can I accept multiple active services at the same time?",
        answer: "No. A worker can have only one active service at a time. An Accepted or In Progress booking keeps you occupied until the current service is completed."
    },

    {
        question: "When should I start a service?",
        answer: "After accepting the booking and beginning the actual service, use Start Service. This changes the booking from Accepted to In Progress."
    },

    {
        question: "How do I complete a service?",
        answer: "After the service is actually finished, use Complete Service. Your completion confirmation is recorded first. The booking becomes Completed after the customer also confirms completion."
    },

    {
        question: "What happens if the customer does not confirm completion?",
        answer: "Your completion confirmation is recorded, but the booking remains In Progress until the customer also confirms completion. Contact support if there is a disagreement or problem."
    },

    {
        question: "When do I become available again?",
        answer: "After both you and the customer confirm that the service is complete, the booking becomes Completed and your availability returns to Available."
    },

    {
        question: "Can I edit my worker profile?",
        answer: "Yes. You can update your personal, contact, address, and professional information from your Worker Profile. Your email address, verification status, and availability status are not editable there."
    },

    {
        question: "What should I do if I have a problem with a customer or service?",
        answer: "Contact ServiceHub support and provide the relevant booking details and a clear description of the situation."
    },

    {
        question: "What if I cannot complete an accepted service?",
        answer: "Contact ServiceHub support as soon as possible and provide the relevant booking details. Do not provide false completion confirmation."
    },

    {
        question: "Where can I see my completed services?",
        answer: "Use the Completed Work section to review services that have reached Completed status."
    },

    {
        question: "Where can I see payment information?",
        answer: "The Completed Work section currently provides a payment information placeholder. Detailed payment functionality will be available as the payment system is implemented."
    }

];


// HTML ESCAPING

function escapeHtml(value) {

    const div = document.createElement("div");

    div.textContent = value ?? "";

    return div.innerHTML;
}


// RENDER FAQS

function renderFaqs(faqs) {

    faqContainer.innerHTML = "";

    faqs.forEach((faq, index) => {

        const item = document.createElement("article");

        item.className =
            "bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden";

        item.innerHTML = `

            <button
                type="button"
                class="faq-question w-full flex items-center justify-between gap-6 text-left p-5 md:p-6 hover:bg-gray-900/70 transition duration-200"
                aria-expanded="false"
                aria-controls="faq-answer-${index}"
            >

                <span class="font-medium text-gray-200">
                    ${escapeHtml(faq.question)}
                </span>

                <span
                    class="faq-icon shrink-0 w-8 h-8 rounded-full border border-gray-700 flex items-center justify-center text-gray-400 transition-transform duration-200"
                >
                    <i class="fa-solid fa-plus text-sm"></i>
                </span>

            </button>


            <div
                id="faq-answer-${index}"
                class="max-h-0 overflow-hidden border-t border-gray-800 opacity-0 transition-all duration-300 ease-in-out"
            >

                <div class="p-5 md:p-6 pt-4 md:pt-5">

                    <p class="text-sm text-gray-400 leading-relaxed">
                        ${escapeHtml(faq.answer)}
                    </p>

                </div>

            </div>

        `;

        faqContainer.appendChild(item);
    });



    // ACCORDION


    faqContainer
        .querySelectorAll(".faq-question")
        .forEach(button => {

            button.addEventListener("click", () => {

                const answer =
                    document.getElementById(
                        button.getAttribute("aria-controls")
                    );

                const icon =
                    button.querySelector(".faq-icon");

                const isOpen =
                    button.getAttribute("aria-expanded") === "true";


                button.setAttribute(
                    "aria-expanded",
                    String(!isOpen)
                );

                answer.style.maxHeight =
                    isOpen
                        ? "0px"
                        : `${answer.scrollHeight}px`;

                answer.classList.toggle(
                    "opacity-0",
                    isOpen
                );

                answer.classList.toggle(
                    "opacity-100",
                    !isOpen
                );

                icon.classList.toggle(
                    "rotate-45",
                    !isOpen
                );

            });

        });

}


// INITIALIZE

if (role === "USER") {

    faqSubtitle.textContent =
        "Find answers about booking services, managing bookings, profiles, payments, and getting support.";

    renderFaqs(customerFaqs);

}
else if (role === "WORKER") {

    faqSubtitle.textContent =
        "Find answers about verification, services, booking requests, availability, completed work, and support.";

    renderFaqs(workerFaqs);

}