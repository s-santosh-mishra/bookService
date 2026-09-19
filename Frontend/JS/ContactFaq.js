const role = localStorage.getItem("servicehub_user_role");

const faqContainer = document.getElementById("faqContainer");

const faqSubtitle = document.getElementById("faqSubtitle");

// ROLE-BASED HEADER

if (role === "USER") {
  document.getElementById("customerHeader").classList.remove("hidden");
} else if (role === "WORKER") {
  document.getElementById("workerHeader").classList.remove("hidden");
} else {
  window.location.href = "login.html";
}

const customerFaqs = [
  {
    question: "How do I book a service?",
    answer:
      "Open the Customer Dashboard, choose the service you need, provide the required service details and location, and create a booking request. You do not need to choose a worker yourself.",
  },

  {
    question: "Can I choose a specific worker?",
    answer:
      "No. ServiceHub matches your request with workers who provide the requested service and are currently available. A suitable worker can then accept the request.",
  },

  {
    question: "What happens after I create a booking?",
    answer:
      "Your booking starts as Pending. Suitable available workers can review the request and decide whether to accept or reject it. The booking remains Pending until a worker accepts it or the 30-minute no-worker timeout is reached.",
  },

  {
    question: "What happens if no worker accepts my booking?",
    answer:
      "If no suitable worker accepts the request within 30 minutes of its creation, the booking becomes No Worker. You can then create a new booking if you still need the service.",
  },

  {
    question: "How many active bookings can I have?",
    answer:
      "A customer can have up to 3 active bookings at a time. Active bookings are Pending, Accepted, and In Progress.",
  },

  {
    question:
      "Can I book the same service again while I already have an active booking?",
    answer:
      "No. You cannot create another active booking for the same service while an existing booking for that service is still active.",
  },

  {
    question: "Can I cancel a booking?",
    answer:
      "You can cancel your booking while it is Pending without a cancellation charge. Once a worker has accepted the booking, contact ServiceHub support if you need to cancel it. Cancellation charges may apply depending on the situation.",
  },

  {
    question: "What happens if I cancel after a worker accepts?",
    answer:
      "If a worker has already accepted the booking, ServiceHub may apply a cancellation fee based on the worker's situation and distance from the service location. Contact ServiceHub support to request the cancellation.",
  },

  {
    question: "What happens if I cancel after the service has started?",
    answer:
      "If the service has already started, you may be charged for the actual labour performed, applicable travel charges, and approved parts used. ServiceHub support can review the situation before the booking is finalized.",
  },

  {
    question: "What does Accepted mean?",
    answer:
      "Accepted means a worker has accepted responsibility for your booking. The worker is assigned to the booking and the service is waiting to be started.",
  },

  {
    question: "What does In Progress mean?",
    answer:
      "In Progress means the assigned worker has started the service. The booking remains in this state while the service is being carried out.",
  },

  {
    question: "How is my service price calculated?",
    answer:
      "The final bill can include labour, travel charges, approved parts, and applicable fees. Labour is calculated using the service's base hourly rate and the actual billable service time.",
  },

  {
    question: "How is labour time calculated?",
    answer:
      "Billable labour time is based on the actual time between when the worker starts the service and when the service is completed or cancelled. Labour is calculated using 15-minute billing blocks, subject to the applicable minimum billable duration.",
  },

  {
    question: "How does worker rating affect the labour price?",
    answer:
      "Worker rating can adjust the labour component of the price. Ratings from 3.5 to 5.0 use the normal base price. Ratings from 2.0 to below 3.5 receive a 5% reduction, and ratings from 0.5 to below 2.0 receive a 10% reduction. A rating below 0.5 may be reviewed by ServiceHub administration.",
  },

  {
    question: "Is travel included in the service price?",
    answer:
      "The first 3 km of travel is included. Travel beyond the included distance may be charged on a per-kilometre basis.",
  },

  {
    question: "How is travel distance calculated?",
    answer:
      "ServiceHub uses the worker's location and the service location to determine the applicable travel distance. The current system uses straight-line distance calculations rather than road-routing distance.",
  },

  {
    question: "How are parts charged?",
    answer:
      "Approved parts used during the service are added to the booking and included in the final bill according to the applicable part price and quantity.",
  },

  {
    question:
      "Can a worker use a part that is not in the ServiceHub catalogue?",
    answer:
      "Yes. An unlisted part can be added with its relevant details and cost, but customer approval is required before it is included as an approved part for the service.",
  },

  {
    question: "What happens if I do not approve a required part?",
    answer:
      "If the required part is not approved and the worker cannot continue the service without it, the worker may be unable to complete the service.",
  },

  {
    question: "What happens if the worker cannot complete my service?",
    answer:
      "If the worker starts the service but cannot complete it because the problem requires different expertise, equipment, parts, or another valid reason, the booking may become Worker Cannot Complete. In this situation, the labour component receives a 30% reduction. Travel and approved parts are not reduced.",
  },

  {
    question: "What happens if the worker cancels before starting the service?",
    answer:
      "The booking may be marked Worker Cancelled. The worker cancellation and the reason are recorded by ServiceHub. If you still require the service, contact ServiceHub or create a new booking when appropriate.",
  },

  {
    question: "How is a service completed?",
    answer:
      "The worker marks the service as completed after finishing the work. You then have 30 minutes to confirm the completion. If you confirm within that period, the booking becomes Completed.",
  },

  {
    question: "What happens if I do not confirm completion?",
    answer:
      "If you do not confirm completion within 30 minutes after the worker marks the service complete, the booking may become Auto Completed. An applicable auto-completion fee may be added because the worker remains unavailable while waiting for your confirmation.",
  },

  {
    question:
      "What if there is a problem with the service after the worker marks it complete?",
    answer:
      "Do not ignore the issue. Contact ServiceHub support before the 30-minute confirmation period ends so the situation can be reviewed and appropriate action can be taken.",
  },

  {
    question: "Can I contact ServiceHub before auto-completion?",
    answer:
      "Yes. If there is a disagreement, unfinished work, or another issue, contact ServiceHub support before the 30-minute confirmation period expires. This allows the situation to be reviewed before auto-completion.",
  },

  {
    question: "Can I rate the worker after the service?",
    answer:
      "Yes. Feedback is available after a booking reaches Completed or Auto Completed status.",
  },

  {
    question: "What does worker feedback contain?",
    answer:
      "You can provide a rating from 1 to 5 stars and select applicable predefined feedback labels such as Professional, Punctual, Good Communication, Quality Work, Clean and Tidy, and Friendly.",
  },

  {
    question: "Can I give feedback for a cancelled booking?",
    answer:
      "No. Feedback is available only for Completed and Auto Completed bookings.",
  },

  {
    question: "How are payments handled?",
    answer:
      "Payment is handled separately from the booking status. A payment can be Pending, Paid, Failed, or Refunded. A payment failure does not automatically change what happened to the booking.",
  },

  {
    question: "What happens if my payment fails?",
    answer:
      "The payment is marked as Failed. The booking status is not automatically changed because of the payment failure, and the payment can be retried through the available payment process.",
  },

  {
    question: "Where can I see my previous bookings?",
    answer:
      "Use the Customer Bookings section to view your current and previous bookings along with their booking status.",
  },

  {
    question: "Can I edit my profile information?",
    answer:
      "Yes. You can update your personal information, phone number, and address information from your Customer Profile. Your registered email address is not editable from the profile.",
  },

  {
    question: "What should I do if there is a problem with my booking?",
    answer:
      "Contact ServiceHub support and provide the relevant booking ID, service details, and a clear description of the problem. This helps the support team review the situation efficiently.",
  },
];

// WORKER FAQS

const workerFaqs = [
  {
    question: "How do I become a ServiceHub worker?",
    answer:
      "Register as a worker and provide the required personal and professional information. Your account must be verified by ServiceHub before you can receive and accept service requests.",
  },

  {
    question: "What happens after I register?",
    answer:
      "Your worker account goes through the verification process. Once an administrator verifies your account, you can become available to receive suitable service requests.",
  },

  {
    question: "Why do I need to be verified?",
    answer:
      "Worker verification is required before you can receive and accept service requests through ServiceHub.",
  },

  {
    question: "How do I add the services I provide?",
    answer:
      "Use the My Services section of the Worker Dashboard to manage the services you provide. Only services you provide can be used for matching suitable booking requests.",
  },

  {
    question: "Can I manage my services from my Worker Profile?",
    answer:
      "No. Service management is handled separately through the My Services section.",
  },

  {
    question: "How does worker availability work?",
    answer:
      "A verified worker who is Available can receive suitable booking requests. A worker who is Unavailable does not receive new requests.",
  },

  {
    question: "Can I manually change my availability?",
    answer:
      "Availability is controlled by the ServiceHub workflow. Accepting a booking makes the worker Busy, and completing or otherwise resolving the active booking can make the worker Available again.",
  },

  {
    question: "Why am I not receiving booking requests?",
    answer:
      "You must be verified, available, and provide the requested service. If you already have an Accepted or In Progress booking, you are busy and cannot accept another active service.",
  },

  {
    question: "What happens when I receive a booking request?",
    answer:
      "The request appears in your Booking Requests section with the relevant customer and service information. You can review the request and either accept or reject it.",
  },

  {
    question: "What happens if I reject a booking request?",
    answer:
      "Rejecting a request does not cancel the customer's booking. The booking remains Pending so another suitable worker can accept it.",
  },

  {
    question: "What happens when I accept a booking?",
    answer:
      "The booking becomes Accepted and you become the assigned worker. Your availability becomes Busy and you cannot accept another active booking until the current service is resolved.",
  },

  {
    question: "Can I accept multiple active services?",
    answer:
      "No. A worker can have only one active service at a time. An Accepted or In Progress booking keeps you occupied.",
  },

  {
    question: "When should I start a service?",
    answer:
      "After accepting the booking, use Start Service when you have reached the service location and actually begin the work. This changes the booking from Accepted to In Progress.",
  },

  {
    question: "How do I complete a service?",
    answer:
      "After the service is actually finished, use Complete Service. Your completion confirmation is recorded and the customer receives the opportunity to confirm the completion.",
  },

  {
    question: "What happens after I mark a service as completed?",
    answer:
      "The customer has 30 minutes to confirm completion. During this period you remain Busy because the booking is waiting for the customer's confirmation.",
  },

  {
    question: "What happens if the customer does not confirm completion?",
    answer:
      "If the customer does not confirm within 30 minutes, the booking may become Auto Completed. An applicable auto-completion fee may be charged because you remained unavailable while waiting for confirmation.",
  },

  {
    question: "When do I become available again?",
    answer:
      "After the booking reaches Completed or Auto Completed status, or is otherwise resolved according to the ServiceHub workflow, your availability can return to Available.",
  },

  {
    question: "Can I cancel an accepted booking?",
    answer:
      "Yes, when there is a valid reason. If you cancel after accepting but before starting the service, the booking is recorded as Worker Cancelled and you must provide a cancellation reason.",
  },

  {
    question: "What reasons can I give when cancelling before starting?",
    answer:
      "Available reasons include an emergency or personal issue, vehicle or transport problem, inability to reach the customer, incorrect booking or service information, or another valid reason.",
  },

  {
    question:
      "What if I have already started the service but cannot complete it?",
    answer:
      "If you cannot complete a service after starting it, you can use the worker cancellation process and provide the applicable reason. The booking may become Worker Cannot Complete.",
  },

  {
    question: "What happens to the price if I cannot complete a service?",
    answer:
      "For a Worker Cannot Complete outcome, the labour component receives a 30% reduction. Applicable travel charges and approved parts are not reduced.",
  },

  {
    question: "What if I need a part before I can continue the service?",
    answer:
      "If you can continue the service while arranging the required part, the booking remains In Progress and the service time continues to be billable. If the required part is unavailable or cannot be approved and you cannot continue, the service may become Worker Cannot Complete.",
  },

  {
    question: "How are parts added to a booking?",
    answer:
      "Use the available parts process to add the required part and quantity. Parts from the ServiceHub catalogue use their applicable prices. Unlisted parts require customer approval before being included as approved parts.",
  },

  {
    question: "How is my service work time calculated?",
    answer:
      "Billable labour time is based on the actual time from Start Service until the service is completed or cancelled. Labour uses 15-minute billing blocks, subject to the applicable minimum billable duration.",
  },

  {
    question: "How does my rating affect service pricing?",
    answer:
      "Worker ratings can affect the labour price used for future bookings. A rating from 3.5 to 5.0 uses the normal base price, 2.0 to below 3.5 receives a 5% reduction, and 0.5 to below 2.0 receives a 10% reduction. A rating below 0.5 may be reviewed by ServiceHub administration.",
  },

  {
    question: "How do customers rate my work?",
    answer:
      "After a booking reaches Completed or Auto Completed, the customer can provide a 1–5 star rating and select predefined feedback labels.",
  },

  {
    question: "Where can I see my completed services?",
    answer:
      "Use the Completed Work section to review services that have reached their completed state.",
  },

  {
    question: "Where can I see payment information?",
    answer:
      "Payment and billing information is associated with the completed service transaction. The final billed amount includes the applicable labour, travel, approved parts, and fees.",
  },

  {
    question: "Can I edit my worker profile?",
    answer:
      "Yes. You can update your personal, contact, address, and professional information from your Worker Profile. Your email address, verification status, and availability status are not editable there.",
  },

  {
    question:
      "What should I do if there is a problem with a customer or service?",
    answer:
      "Contact ServiceHub support and provide the relevant booking details and a clear description of the situation. Do not provide false service or completion information.",
  },

  {
    question: "What happens if a customer disputes the service?",
    answer:
      "Contact ServiceHub support and provide the relevant booking information. ServiceHub may review the booking, service details, completion information, billing information, and other relevant records before taking appropriate action.",
  },
];

let currentFaqs = customerFaqs;

const faqSearch = document.getElementById("faqSearch");

faqSearch.addEventListener("input", () => {
  const searchText = faqSearch.value.toLowerCase().trim();

  const faqItems = faqContainer.children;

  Array.from(faqItems).forEach((item, index) => {
    const faq = currentFaqs[index];

    const matches =
      faq.question.toLowerCase().includes(searchText) ||
      faq.answer.toLowerCase().includes(searchText);

    item.classList.toggle("hidden", !matches);
  });
});

// HTML ESCAPING

function escapeHtml(value) {
  const div = document.createElement("div");

  div.textContent = value ?? "";

  return div.innerHTML;
}

// RENDER FAQS

function renderFaqs(faqs) {
  currentFaqs = faqs;
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

  faqContainer.querySelectorAll(".faq-question").forEach((button) => {
    button.addEventListener("click", () => {
      const answer = document.getElementById(
        button.getAttribute("aria-controls"),
      );

      const icon = button.querySelector(".faq-icon");

      const isOpen = button.getAttribute("aria-expanded") === "true";

      button.setAttribute("aria-expanded", String(!isOpen));

      answer.style.maxHeight = isOpen ? "0px" : `${answer.scrollHeight}px`;

      answer.classList.toggle("opacity-0", isOpen);

      answer.classList.toggle("opacity-100", !isOpen);

      icon.classList.toggle("rotate-45", !isOpen);
    });
  });
}

// INITIALIZE

if (role === "USER") {
  currentFaqs = customerFaqs;
  faqSubtitle.textContent =
    "Find answers about booking services, managing bookings, profiles, payments, and getting support.";

  renderFaqs(customerFaqs);
} else if (role === "WORKER") {
  currentFaqs = workerFaqs;
  faqSubtitle.textContent =
    "Find answers about verification, services, booking requests, availability, completed work, and support.";

  renderFaqs(workerFaqs);
}
