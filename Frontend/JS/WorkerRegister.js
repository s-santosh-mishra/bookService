// ServiceHub Worker Registration

const API_BASE = "http://localhost:8080";

const registerForm = document.getElementById("workerRegisterForm");

const fullName = document.getElementById("fullName");

const age = document.getElementById("age");

const gender = document.getElementById("gender");

const email = document.getElementById("email");

const phone = document.getElementById("phone");

const addressLine1 = document.getElementById("addressLine1");

const addressLine2 = document.getElementById("addressLine2");

const landmark = document.getElementById("landmark");

const city = document.getElementById("city");

const state = document.getElementById("state");

const pincode = document.getElementById("pincode");

const experienceYears = document.getElementById("experienceYears");

const qualification = document.getElementById("qualification");

const bio = document.getElementById("bio");

const password = document.getElementById("password");

const confirmPassword = document.getElementById("confirmPassword");

const terms = document.getElementById("terms");

const togglePassword = document.getElementById("togglePassword");

const toggleConfirmPassword = document.getElementById("toggleConfirmPassword");

const registerButton = document.getElementById("registerButton");

const registerMessage = document.getElementById("registerMessage");

const categoriesContainer = document.getElementById("categoriesContainer");

const categoryLimitMessage = document.getElementById("categoryLimitMessage");

const servicesError = document.getElementById("servicesError");

let categories = [];

// Load Categories

async function loadCategories() {
  try {
    const response = await fetch(`${API_BASE}/api/categories`);

    if (!response.ok) {
      throw new Error("Unable to load services.");
    }

    categories = await response.json();

    renderCategories();
  } catch (error) {
    console.error("Category loading error:", error);

    categoriesContainer.innerHTML = `
            <p class="text-sm text-red-400">
                Unable to load services.
                Please refresh the page.
            </p>
        `;
  }
}

// Render Categories

function renderCategories() {
  categoriesContainer.innerHTML = "";

  categories.forEach((category) => {
    const categoryCard = document.createElement("div");

    categoryCard.className =
      "border border-white/10 rounded-xl " + "bg-gray-950/60 overflow-hidden";

    // Category Header

    const categoryHeader = document.createElement("div");

    categoryHeader.className =
      "flex items-center gap-3 px-5 py-4 " + "border-b border-white/10";

    const categoryCheckbox = document.createElement("input");

    categoryCheckbox.type = "checkbox";

    categoryCheckbox.className = "category-checkbox h-4 w-4 accent-purple-500";

    categoryCheckbox.dataset.categoryId = category.categoryId;

    const categoryLabel = document.createElement("label");

    categoryLabel.className = "text-base font-semibold text-white";

    categoryLabel.textContent = category.categoryName;

    categoryHeader.appendChild(categoryCheckbox);

    categoryHeader.appendChild(categoryLabel);

    // Services

    const servicesContainer = document.createElement("div");

    servicesContainer.className =
      "px-5 py-4 grid grid-cols-1 md:grid-cols-2 gap-3";

    category.services.forEach((service) => {
      const serviceLabel = document.createElement("label");

      serviceLabel.className =
        "flex items-center gap-3 p-3 rounded-lg " +
        "border border-white/5 bg-gray-900/60 " +
        "hover:border-purple-500/40 transition cursor-pointer";

      const serviceCheckbox = document.createElement("input");

      serviceCheckbox.type = "checkbox";

      serviceCheckbox.className = "service-checkbox h-4 w-4 accent-purple-500";

      serviceCheckbox.dataset.categoryId = category.categoryId;

      serviceCheckbox.dataset.serviceId = service.serviceId;

      const serviceText = document.createElement("span");

      serviceText.className = "text-sm text-gray-300";

      serviceText.textContent = service.serviceName;

      serviceLabel.appendChild(serviceCheckbox);

      serviceLabel.appendChild(serviceText);

      servicesContainer.appendChild(serviceLabel);
    });

    categoryCard.appendChild(categoryHeader);

    categoryCard.appendChild(servicesContainer);

    categoriesContainer.appendChild(categoryCard);
  });

  addServiceListeners();

  updateCategoryLimit();
}

// Service Listeners

function addServiceListeners() {
  document.querySelectorAll(".service-checkbox").forEach((checkbox) => {
    checkbox.addEventListener("change", () => {
      updateCategoryState(checkbox.dataset.categoryId);
    });
  });

  document.querySelectorAll(".category-checkbox").forEach((checkbox) => {
    checkbox.addEventListener("change", () => {
      const categoryId = checkbox.dataset.categoryId;

      /*
       * If a category is manually
       * unchecked, remove all its services.
       */

      if (!checkbox.checked) {
        document
          .querySelectorAll(
            `.service-checkbox[data-category-id="${categoryId}"]`,
          )
          .forEach((service) => {
            service.checked = false;
          });
      }

      updateCategoryState(categoryId);
    });
  });
}

// Update Category State

function updateCategoryState(categoryId) {
  const categoryCheckbox = document.querySelector(
    `.category-checkbox[data-category-id="${categoryId}"]`,
  );

  const serviceCheckboxes = document.querySelectorAll(
    `.service-checkbox[data-category-id="${categoryId}"]`,
  );

  const hasSelectedService = [...serviceCheckboxes].some(
    (checkbox) => checkbox.checked,
  );

  categoryCheckbox.checked = hasSelectedService;

  updateCategoryLimit();
}

// Get Selected Categories

function getSelectedCategoryIds() {
  const selectedCategories = new Set();

  document.querySelectorAll(".service-checkbox:checked").forEach((checkbox) => {
    selectedCategories.add(checkbox.dataset.categoryId);
  });

  return selectedCategories;
}

// Maximum 3 Categories

function updateCategoryLimit() {
  const selectedCategories = getSelectedCategoryIds();

  const count = selectedCategories.size;

  if (count >= 3) {
    categoryLimitMessage.classList.remove("hidden");
  } else {
    categoryLimitMessage.classList.add("hidden");
  }

  /*
   * Selected categories remain enabled.
   * Other categories become disabled.
   */

  document.querySelectorAll(".category-checkbox").forEach((checkbox) => {
    const categoryId = checkbox.dataset.categoryId;

    checkbox.disabled = count >= 3 && !selectedCategories.has(categoryId);
  });

  /*
   * Services in selected categories
   * remain usable.
   */

  document.querySelectorAll(".service-checkbox").forEach((checkbox) => {
    const categoryId = checkbox.dataset.categoryId;

    checkbox.disabled = count >= 3 && !selectedCategories.has(categoryId);
  });
}

// Password Visibility

togglePassword.addEventListener("click", () => {
  togglePasswordVisibility(password, togglePassword);
});

toggleConfirmPassword.addEventListener("click", () => {
  togglePasswordVisibility(confirmPassword, toggleConfirmPassword);
});

function togglePasswordVisibility(input, button) {
  const isPassword = input.type === "password";

  input.type = isPassword ? "text" : "password";

  button.innerHTML = isPassword
    ? '<i class="fa-solid fa-eye-slash"></i>'
    : '<i class="fa-solid fa-eye"></i>';
}

// Input Restrictions

phone.addEventListener("input", () => {
  phone.value = phone.value.replace(/\D/g, "").slice(0, 10);
});

pincode.addEventListener("input", () => {
  pincode.value = pincode.value.replace(/\D/g, "").slice(0, 6);
});

age.addEventListener("input", () => {
  age.value = age.value.replace(/\D/g, "").slice(0, 3);
});

experienceYears.addEventListener("input", () => {
  experienceYears.value = experienceYears.value.replace(/\D/g, "").slice(0, 2);
});

// Form Submission

registerForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  clearErrors();

  if (!validateForm()) {
    return;
  }

  const selectedServiceIds = [
    ...document.querySelectorAll(".service-checkbox:checked"),
  ].map((checkbox) => checkbox.dataset.serviceId);

  // At least one service

  if (selectedServiceIds.length === 0) {
    showServiceError("Please select at least one service.");

    return;
  }

  const selectedCategoryIds = getSelectedCategoryIds();

  if (selectedCategoryIds.size > 3) {
    showServiceError("You can select services from a maximum of 3 categories.");

    return;
  }

  // Request Data

  const formData = {
    fullName: fullName.value.trim(),

    age: Number(age.value),

    gender: gender.value.toUpperCase(),

    email: email.value.trim(),

    phone: phone.value.trim(),

    addressLine1: addressLine1.value.trim(),

    addressLine2: addressLine2.value.trim() || null,

    landmark: landmark.value.trim() || null,

    city: city.value.trim(),

    state: state.value,

    pinCode: pincode.value.trim(),

    password: password.value,

    confirmPassword: confirmPassword.value,

    experienceYears: Number(experienceYears.value),

    qualification: qualification.value.trim(),

    bio: bio.value.trim() || null,

    serviceIds: selectedServiceIds,

    termsAccepted: terms.checked,
  };

  // Loading

  setLoading(true);

  try {
    const response = await fetch(`${API_BASE}/api/worker/register`, {
      method: "POST",

      headers: {
        "Content-Type": "application/json",
      },

      body: JSON.stringify(formData),
    });

    const result = await readResponse(response);

    if (!response.ok) {
      showMessage(getBackendError(result), "error");

      return;
    }

    // Success

    registerForm.reset();

    document.querySelectorAll(".category-checkbox").forEach((checkbox) => {
      checkbox.checked = false;
      checkbox.disabled = false;
    });

    document.querySelectorAll(".service-checkbox").forEach((checkbox) => {
      checkbox.checked = false;
      checkbox.disabled = false;
    });

    categoryLimitMessage.classList.add("hidden");

    showSuccess();
  } catch (error) {
    console.error("Worker registration error:", error);

    showMessage(
      "Unable to connect to the server. Please make sure the backend is running.",
      "error",
    );
  } finally {
    setLoading(false);
  }
});

// Validation

function validateForm() {
  let valid = true;

  // Full Name

  if (!fullName.value.trim()) {
    showError(fullName, "fullNameError", "Please enter your full name.");

    valid = false;
  }

  // Age

  const ageValue = Number(age.value);

  if (!age.value) {
    showError(age, "ageError", "Please enter your age.");

    valid = false;
  } else if (ageValue < 18 || ageValue > 100) {
    showError(age, "ageError", "Age must be between 18 and 100.");

    valid = false;
  }

  // Gender

  if (!gender.value) {
    showError(gender, "genderError", "Please select your gender.");

    valid = false;
  }

  // Email

  if (!email.value.trim()) {
    showError(email, "emailError", "Please enter your email address.");

    valid = false;
  } else if (!isValidEmail(email.value.trim())) {
    showError(email, "emailError", "Please enter a valid email address.");

    valid = false;
  }

  // Phone

  if (!phone.value.trim()) {
    showError(phone, "phoneError", "Please enter your phone number.");

    valid = false;
  } else if (!/^[6-9]\d{9}$/.test(phone.value.trim())) {
    showError(
      phone,
      "phoneError",
      "Please enter a valid 10-digit mobile number.",
    );

    valid = false;
  }

  // Address

  if (!addressLine1.value.trim()) {
    showError(addressLine1, "addressLine1Error", "Please enter your address.");

    valid = false;
  }

  // City

  if (!city.value.trim()) {
    showError(city, "cityError", "Please enter your city.");

    valid = false;
  }

  // State

  if (!state.value) {
    showError(state, "stateError", "Please select your state.");

    valid = false;
  }

  // PIN

  if (!pincode.value.trim()) {
    showError(pincode, "pincodeError", "Please enter your PIN code.");

    valid = false;
  } else if (!/^\d{6}$/.test(pincode.value.trim())) {
    showError(
      pincode,
      "pincodeError",
      "Please enter a valid 6-digit PIN code.",
    );

    valid = false;
  }

  // Experience

  if (!experienceYears.value) {
    showError(
      experienceYears,
      "experienceYearsError",
      "Please enter your years of experience.",
    );

    valid = false;
  }

  // Qualification

  if (!qualification.value.trim()) {
    showError(
      qualification,
      "qualificationError",
      "Please enter your qualification.",
    );

    valid = false;
  }

  // Password

  if (!password.value) {
    showError(password, "passwordError", "Please create a password.");

    valid = false;
  } else if (password.value.length < 8) {
    showError(
      password,
      "passwordError",
      "Password must contain at least 8 characters.",
    );

    valid = false;
  }

  // Confirm Password

  if (!confirmPassword.value) {
    showError(
      confirmPassword,
      "confirmPasswordError",
      "Please confirm your password.",
    );

    valid = false;
  } else if (password.value !== confirmPassword.value) {
    showError(
      confirmPassword,
      "confirmPasswordError",
      "Passwords do not match.",
    );

    valid = false;
  }

  // Terms

  if (!terms.checked) {
    const error = document.getElementById("termsError");

    error.textContent = "Please accept the Terms & Conditions to continue.";

    error.classList.remove("hidden");

    valid = false;
  }

  return valid;
}

// Loading State

function setLoading(loading) {
  registerButton.disabled = loading;

  if (loading) {
    registerButton.innerHTML = `
            <i class="fa-solid fa-spinner fa-spin"></i>
            <span>Submitting registration...</span>
        `;
  } else {
    registerButton.innerHTML = `
            <i class="fa-solid fa-user-plus"></i>
            <span>Submit Registration</span>
        `;
  }
}

// Success

function showSuccess() {
  let countdown = 7;

  registerMessage.className =
    "mt-6 rounded-lg border border-green-500/30 " +
    "bg-green-500/10 px-4 py-3 text-sm text-green-400";

  registerMessage.innerHTML = `
        <div class="text-center">

            <p class="font-medium">
                Registration submitted successfully!
            </p>

            <p class="mt-1">
                Your worker account is now waiting for verification.
            </p>

            <p class="mt-2 text-gray-400">
                You can login after your account is verified.
            </p>

            <p class="mt-2 text-gray-400">

                Redirecting to login in
                <span id="countdown">
                    ${countdown}
                </span>
                seconds...

            </p>

        </div>
    `;

  const countdownElement = document.getElementById("countdown");

  const countdownTimer = setInterval(() => {
    countdown--;

    countdownElement.textContent = countdown;

    if (countdown <= 0) {
      clearInterval(countdownTimer);

      window.location.href = "../HTML/login.html";
    }
  }, 1000);
}

// Error Helpers

function showError(input, errorId, message) {
  input.classList.add("input-error");

  const error = document.getElementById(errorId);

  error.textContent = message;

  error.classList.remove("hidden");
}

function showServiceError(message) {
  servicesError.textContent = message;

  servicesError.classList.remove("hidden");
}

function clearErrors() {
  registerForm.querySelectorAll(".form-input").forEach((input) => {
    input.classList.remove("input-error");
  });

  registerForm.querySelectorAll(".error-message").forEach((error) => {
    error.classList.add("hidden");

    error.textContent = "";
  });

  registerMessage.classList.add("hidden");

  registerMessage.textContent = "";
}

function showMessage(message, type) {
  if (type === "error") {
    registerMessage.className =
      "mt-6 rounded-lg border border-red-500/30 " +
      "bg-red-500/10 px-4 py-3 text-sm text-red-400";
  }

  registerMessage.textContent = message;
}

async function readResponse(response) {
  const contentType = response.headers.get("content-type");

  if (contentType && contentType.includes("application/json")) {
    return await response.json();
  }

  return await response.text();
}

function getBackendError(result) {
  if (typeof result === "object" && result !== null) {
    return (
      result.error || result.message || "Registration failed. Please try again."
    );
  }

  return result || "Registration failed. Please try again.";
}

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

// Initialize

loadCategories();
