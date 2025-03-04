let originalContent = "";
document.addEventListener("DOMContentLoaded", function () {
    const menuButton = document.getElementById("menuButton");
    const sidebar = document.querySelector(".sidebar");
    const contentWrapper = document.querySelector(".content-wrapper");
    const createContactButton = document.getElementById("createContactButton");
    const contentContainer = document.getElementById("contentContainer");
    const deleteButton = document.getElementById("deleteContactButton");
    const editButton = document.getElementById("editContactButton");
    const saveButton = document.getElementById("saveContactButton");
    const searchBox = document.querySelector(".search-box");
    const contactRows = document.querySelectorAll(".contact-row");

        // Toggle Sidebar
    menuButton?.addEventListener("click", function () {
        sidebar?.classList.toggle("open");
        contentWrapper?.classList.toggle("shifted");
    });

        // Load "Create Contact" Form
    createContactButton?.addEventListener("click", function () {
        fetch("/contacts/create-contact")
            .then(response => response.text())
            .then(html => {
                contentContainer.innerHTML = html;
                attachDynamicEventListeners(); // Attach event listeners after loading the form
            });
    });

    searchBox.addEventListener("input", function () {
        let searchTerm = searchBox.value.trim().toLowerCase();

        contactRows.forEach(row => {
            let name = row.querySelector("td:nth-child(1) span").textContent.toLowerCase();
            let email = row.querySelector("td:nth-child(2)").textContent.toLowerCase();
            let birthday = row.querySelector("td:nth-child(5)").textContent.toLowerCase();

            // Show row if any column contains the search term
            if (name.includes(searchTerm) || email.includes(searchTerm) || birthday.includes(searchTerm)) {
                row.style.display = "";
            } else {
                row.style.display = "none";
            }
        });
    });

    if (deleteButton) {
        deleteButton.addEventListener("click", function () {
            deleteContact();
        });
    }

    if (editButton) editButton.addEventListener("click", () => toggleEditMode(true));

    if (saveButton) {
        saveButton.replaceWith(saveButton.cloneNode(true));
        const newSaveButton = document.getElementById("saveButton");
        newSaveButton.addEventListener("click", function (event) {
            event.preventDefault();
            saveContactDetails();
        });
    }

    if (contentContainer) {
        originalContent = contentContainer.innerHTML; // Store original content
    }

    attachEditFunctionality();
});


// Function to attach event listeners for dynamically added elements
function attachDynamicEventListeners() {
    const backButton = document.querySelector(".back-button");
    const saveButton = document.getElementById("saveButton"); // Fixing selector
    const imageUpload = document.getElementById("imageUpload");

    // Back Button Event
    if (backButton) {
        backButton.addEventListener("click", function () {
            closeCreateContact();
        });
    }

    // Image Upload Event
    if (imageUpload) {
        imageUpload.addEventListener("change", function (event) {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function (e) {
                    document.getElementById("profileImage").src = e.target.result;
                    document.getElementById("profileImage").style.display = "block";
                    document.getElementById("avatarPlaceholder").style.display = "none";
                    document.getElementById("imageBase64").value = e.target.result; // Store Base64
                };
                reader.readAsDataURL(file);
            }
        });
    }

    // Enable Save Button on Input
    const inputs = document.querySelectorAll(".contact-form input");
    function checkInputs() {
        saveButton.disabled = !Array.from(inputs).every(input => input.value.trim() !== "");
    }
    inputs.forEach(input => input.addEventListener("input", checkInputs));

    // Form Submission
    saveButton?.addEventListener("click", function (event) {
        event.preventDefault(); // Prevent default form submission

        const contactData = {
            name: document.getElementById("contactName").value,
            email: document.getElementById("contactEmail").value,
            phone: document.getElementById("contactPhone").value,
            birthday: document.getElementById("contactBirthday").value,
            address: document.getElementById("contactAddress").value,
            photoUrl: document.getElementById("imageBase64").value, // Use stored Base64
        };

        fetch("/contacts/createContact", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(contactData),
        })
        .then(response => response.json())
        .then(data => {
            showModal("Contact created successfully!",null,false);
            closeCreateContact();
        })
        .catch(error => console.error("Error creating contact:", error));
    });
}


// Function to close the "Create Contact" form and return to the main contact list
function closeCreateContact() {
    location.reload();
}




// Fetch contacts data from the script tag
const contactsDataElement = document.getElementById("contactsData");
if (contactsDataElement) {
    try {
        contacts = JSON.parse(contactsDataElement.textContent);
    } catch (error) {
        console.error("Error parsing contacts data:", error);
    }
}

// Function to handle contact selection
function selectContact(row) {
    exitEditMode();
    document.querySelectorAll('.contact-row').forEach(r => r.classList.remove('selected'));
    row.classList.add('selected');
    const index = row.getAttribute('data-index');
    if (contacts.length > index) {
        showContactDetails(contacts[index]);
    } else {
        console.error("Invalid contact index:", index);
    }
}

// Function to show contact details in the panel
function showContactDetails(contact) {
    const panel = document.getElementById('contactDetailsPanel');
    const initial = document.getElementById('contactDetailInitial');
    const img = document.getElementById('contactDetailImg');
    const name = document.getElementById('contactDetailName');
    const email = document.getElementById('contactDetailEmail');
    const phone = document.getElementById('contactDetailPhone');
    const address = document.getElementById('contactDetailAddress');
    const birthday = document.getElementById('contactDetailBirthday');
    const contactIdInput = document.getElementById('contactDetailId');

    const displayName = contact?.names?.[0]?.displayName || 'Unknown';
    name.textContent = displayName;

    if (contact?.photos?.[0]?.url) {
        initial.style.display = 'none';
        img.style.display = 'block';
        img.src = contact.photos[0].url;
    } else {
        img.style.display = 'none';
        initial.style.display = 'flex';
        initial.textContent = displayName.charAt(0).toUpperCase();
    }

    email.textContent = contact?.emailAddresses?.[0]?.value || '-';
    phone.textContent = contact?.phoneNumbers?.[0]?.value || '-';
    address.textContent = contact?.addresses?.[0]?.formattedValue || '-';

    // Fix for birthday display
    if (contact?.birthdays?.[0]?.date) {
        const date = contact.birthdays[0].date;
        birthday.textContent = `${date.year}-${String(date.month).padStart(2, '0')}-${String(date.day).padStart(2, '0')}`;
    } else {
        birthday.textContent = '-';
    }

    contactIdInput.value = contact?.resourceName || "";

    panel.style.display = 'block';
}

// Function to close the contact details panel
function closeContactDetails() {
    document.getElementById('contactDetailsPanel').style.display = 'none';
    exitEditMode();
}

// Function to delete a contact
function deleteContact() {
    const selectedRow = document.querySelector(".contact-row.selected");
    if (!selectedRow) {
        showModal("Please select a contact first.",null,false);
        return;
    }

    const index = selectedRow.getAttribute("data-index");
    if (contacts.length <= index) {
        showModal("Invalid contact selection.".null,false);
        return;
    }

    const contactId = contacts[index].resourceName; // Google API uses resourceName as the ID
    if (!contactId) {
        showModal("Cannot delete contact without an ID.",showModal);
        return;
    }

    showModal("Are you sure you want to delete this contact?", function (confirmed) {
        if (confirmed) {
            fetch(`/contacts/delete?contactId=${encodeURIComponent(contactId)}`, { 
                method: "DELETE",
                headers: { "Content-Type": "application/json" }
            }).then(response => {
                if (response.ok) {
                    showModal("Contact deleted successfully.", () => location.reload());
                } else {
                    showModal("Failed to delete contact.");
                }
            }).catch(error => console.error("Error deleting contact:", error));
        } else {
            showModal("Deletion canceled.");
        }
    }, true);
}

// Function to enable editing contact details
function attachEditFunctionality() {
    const editButton = document.querySelector(".contact-action .contact-action-text");
    
    if (editButton && editButton.textContent === "Edit") {
        editButton.addEventListener("click", function () {
            toggleEditContactDetails();
        });
    }
}

// Function to toggle contact details edit mode
function toggleEditMode(editMode) {
    const nameDiv = document.getElementById("contactDetailName");
    const emailDiv = document.getElementById("contactDetailEmail");
    const phoneDiv = document.getElementById("contactDetailPhone");
    const addressDiv = document.getElementById("contactDetailAddress");
    const birthdayDiv = document.getElementById("contactDetailBirthday");

    const nameInput = document.getElementById("contactNameInput");
    const emailInput = document.getElementById("contactEmailInput");
    const phoneInput = document.getElementById("contactPhoneInput");
    const addressInput = document.getElementById("contactAddressInput");
    const birthdayInput = document.getElementById("contactBirthdayInput");

    if (editMode) {
        // Copy text from divs to inputs
        nameInput.value = nameDiv.textContent.trim();
        emailInput.value = emailDiv.textContent.trim();
        phoneInput.value = phoneDiv.textContent.trim();
        addressInput.value = addressDiv.textContent.trim();
        birthdayInput.value = birthdayDiv.textContent.trim();

        // Show inputs, hide divs
        nameDiv.style.display = "none";
        emailDiv.style.display = "none";
        phoneDiv.style.display = "none";
        addressDiv.style.display = "none";
        birthdayDiv.style.display = "none";

        nameInput.style.display = "block";
        emailInput.style.display = "block";
        phoneInput.style.display = "block";
        addressInput.style.display = "block";
        birthdayInput.style.display = "block";

        document.getElementById("saveContactButton").style.display = "block";
        document.getElementById("editContactButton").style.display = "none";
    } else {
        // Hide inputs, show divs
        nameDiv.style.display = "block";
        emailDiv.style.display = "block";
        phoneDiv.style.display = "block";
        addressDiv.style.display = "block";
        birthdayDiv.style.display = "block";

        nameInput.style.display = "none";
        emailInput.style.display = "none";
        phoneInput.style.display = "none";
        addressInput.style.display = "none";
        birthdayInput.style.display = "none";

        document.getElementById("saveContactButton").style.display = "none";
        document.getElementById("editContactButton").style.display = "block";

        // Save values from inputs to divs
        nameDiv.textContent = nameInput.value;
        emailDiv.textContent = emailInput.value;
        phoneDiv.textContent = phoneInput.value;
        addressDiv.textContent = addressInput.value;
        birthdayDiv.textContent = birthdayInput.value;

        // Call save function after switching back to display mode
        saveContactDetails();
    }
}

function exitEditMode() {
    const saveButton = document.getElementById("saveContactButton");
    const editButton = document.getElementById("editContactButton");

    const contactFields = [
        "contactDetailName",
        "contactDetailEmail",
        "contactDetailPhone",
        "contactDetailAddress",
        "contactDetailBirthday"
    ];

    contactFields.forEach(fieldId => {
        const input = document.getElementById(fieldId);
        if (input && input.tagName.toLowerCase() === "input") {
            const newText = document.createElement("div");
            newText.classList.add("contact-details-value");
            newText.textContent = input.dataset.original || input.value; // Restore original if empty
            newText.id = fieldId;
            input.replaceWith(newText);
        }
    });

    editButton.style.display = "flex";
    saveButton.style.display = "none";
}

// Function to save edited contact details
function saveContactDetails() {
    // Get values from input fields (not divs)
    const resourceName = document.getElementById("contactDetailId").value; // Hidden input field with contact ID
    const name = document.getElementById("contactNameInput").value; // Name input
    const email = document.getElementById("contactEmailInput").value; // Email input
    const phone = document.getElementById("contactPhoneInput").value; // Phone input
    const address = document.getElementById("contactAddressInput").value; // Address input
    const birthday = document.getElementById("contactBirthdayInput").value; // Birthday input

    console.log("Saving contact details:", resourceName, name, email, phone, address, birthday);

    fetch("/contacts/updateContact", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            resourceName: resourceName, // Ensure this is correct
            name: name,
            email: email,
            phone: phone,
            address: address,
            birthday: birthday
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            console.error("Update failed:", data.error);
        } else {
            console.log("Contact updated successfully:", data);
            showModal("Contact updated successfully!",null,false);
            location.reload();
        }
    })
    .catch(error => console.error("Error updating contact:", error));
}
// Toggle dropdown visibility
function toggleDropdown() {
    const dropdown = document.getElementById("profileDropdown");
    dropdown.style.display = dropdown.style.display === "block" ? "none" : "block";
}

// Hide dropdown when clicking outside
document.addEventListener("click", function (event) {
    const profilePic = document.querySelector(".profile-pic");
    const dropdown = document.getElementById("profileDropdown");

    if (!profilePic.contains(event.target) && !dropdown.contains(event.target)) {
        dropdown.style.display = "none";
    }
});

// Logout function
function logout() {
    fetch('/logout', { method: 'POST' }) // Adjust the logout URL if needed
        .then(response => {
            if (response.ok) {
                window.location.href = "/login"; // Redirect to login page
            } else {
                showModal("Logout failed. Try again.",null,false);
            }
        })
        .catch(error => console.error("Error:", error));
}

function showModal(message, callback, showCancel = false) {
    const modal = document.getElementById("customModal");
    const modalMessage = document.getElementById("modalMessage");
    const modalConfirm = document.getElementById("modalConfirm");
    const modalCancel = document.getElementById("modalCancel");

    modalMessage.textContent = message;
    modal.style.display = "flex";
    
    modalCancel.style.display = showCancel ? "inline-block" : "none";

    modalConfirm.onclick = function () {
        modal.style.display = "none";
        if (callback) callback(true);
    };

    modalCancel.onclick = function () {
        modal.style.display = "none";
        if (callback) callback(false);
    };
}
