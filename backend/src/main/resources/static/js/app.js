document.addEventListener('DOMContentLoaded', () => {

    const registerForm = document.getElementById('register-form');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const errorDiv = document.getElementById('register-error');

            if (password !== confirmPassword) {
                errorDiv.textContent = 'Passwords do not match';
                errorDiv.classList.remove('d-none');
                return;
            }
            
            try {
                const response = await fetch('/api/v1/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, email, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    document.cookie = `jwt_token=${data.token}; path=/; max-age=86400; samesite=strict`;
                    window.location.href = '/';
                } else {
                    const errorText = await response.text();
                    errorDiv.textContent = errorText || 'Registration failed';
                    errorDiv.classList.remove('d-none');
                }
            } catch (err) {
                errorDiv.textContent = 'Network error occurred. Please try again.';
                errorDiv.classList.remove('d-none');
            }
        });
    }

    // Problems Search & Filter Form Debounce / Submission
    const filterForm = document.getElementById('filterForm');
    const searchInput = document.getElementById('searchProblem');
    if (filterForm && searchInput) {
        let searchTimeout;
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                filterForm.submit();
            }
        });
    }
});

function logout() {
    window.location.href = '/logout';
}
