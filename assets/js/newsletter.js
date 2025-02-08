document.addEventListener('DOMContentLoaded', function() {
    const emailInput = document.getElementById('bd-email');
    const submitButton = emailInput.closest('form').querySelector('input[type="submit"]');
    
    function validateEmail(email) {
        // Regular expression for basic email validation
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }
    
    function updateSubmitButton() {
        const email = emailInput.value.trim();
        submitButton.disabled = !validateEmail(email);
    }
    
    // Add event listeners
    emailInput.addEventListener('input', updateSubmitButton);
    emailInput.addEventListener('change', updateSubmitButton);
});