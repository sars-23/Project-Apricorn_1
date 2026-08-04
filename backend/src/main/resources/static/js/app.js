document.addEventListener('DOMContentLoaded', () => {

    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const errorDiv = document.getElementById('login-error');
            
            try {
                const response = await fetch('/api/v1/auth/authenticate', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    document.cookie = `jwt_token=${data.token}; path=/; max-age=86400; samesite=strict`;
                    window.location.href = '/';
                } else {
                    errorDiv.textContent = 'Invalid email or password';
                    errorDiv.classList.remove('d-none');
                }
            } catch (err) {
                errorDiv.textContent = 'Network error occurred. Please try again.';
                errorDiv.classList.remove('d-none');
            }
        });
    }

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

    // Problems Filtering & Sorting
    const searchInput = document.getElementById('searchProblem');
    const difficultyFilter = document.getElementById('filterDifficulty');
    const tagFilter = document.getElementById('filterTag');
    const sortSelect = document.getElementById('sortProblem');
    const problemContainer = document.getElementById('problemContainer');
    const emptyState = document.getElementById('emptyState');

    if (searchInput && difficultyFilter && tagFilter && sortSelect && problemContainer) {
        
        const filterProblems = () => {
            const searchTerm = searchInput.value.toLowerCase().trim();
            const difficulty = difficultyFilter.value;
            const tag = tagFilter.value;
            
            let visibleCount = 0;
            const cards = Array.from(problemContainer.querySelectorAll('.problem-card'));
            
            cards.forEach(card => {
                const title = card.getAttribute('data-title');
                const cardDiff = card.getAttribute('data-difficulty');
                const cardTags = card.getAttribute('data-tags') ? card.getAttribute('data-tags').split(',') : [];
                
                const matchesSearch = searchTerm === '' || title.includes(searchTerm);
                const matchesDiff = difficulty === 'All' || cardDiff === difficulty;
                const matchesTag = tag === 'All' || cardTags.includes(tag);
                
                if (matchesSearch && matchesDiff && matchesTag) {
                    card.classList.remove('d-none');
                    visibleCount++;
                } else {
                    card.classList.add('d-none');
                }
            });
            
            if (visibleCount === 0) {
                emptyState.classList.remove('d-none');
            } else {
                emptyState.classList.add('d-none');
            }
        };

        const sortProblems = () => {
            const sortVal = sortSelect.value;
            const cards = Array.from(problemContainer.querySelectorAll('.problem-card'));
            
            cards.sort((a, b) => {
                if (sortVal === 'newest') {
                    return parseInt(b.getAttribute('data-id')) - parseInt(a.getAttribute('data-id'));
                } else if (sortVal === 'oldest') {
                    return parseInt(a.getAttribute('data-id')) - parseInt(b.getAttribute('data-id'));
                } else if (sortVal === 'title') {
                    return a.getAttribute('data-title').localeCompare(b.getAttribute('data-title'));
                } else if (sortVal === 'difficulty') {
                    const diffRank = { 'EASY': 1, 'MEDIUM': 2, 'HARD': 3 };
                    return diffRank[a.getAttribute('data-difficulty')] - diffRank[b.getAttribute('data-difficulty')];
                }
                return 0;
            });
            
            cards.forEach(card => problemContainer.appendChild(card));
        };

        searchInput.addEventListener('input', filterProblems);
        difficultyFilter.addEventListener('change', filterProblems);
        tagFilter.addEventListener('change', filterProblems);
        sortSelect.addEventListener('change', sortProblems);
    }
});

function logout() {
    document.cookie = "jwt_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    window.location.href = '/';
}
