let editor;
let currentSubmissionId = null;
let pollInterval = null;

require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs' }});

require(['vs/editor/editor.main'], function() {
    const defaultCode = `public class Solution {
    public static void main(String[] args) {
        // Write your code here
        
    }
}`;

    // Load drafted code from local storage if exists
    const draftKey = `apricorn_draft_${PROBLEM_SLUG}`;
    const savedCode = localStorage.getItem(draftKey);
    const initialCode = savedCode ? savedCode : defaultCode;

    editor = monaco.editor.create(document.getElementById('editorContainer'), {
        value: initialCode,
        language: 'java',
        theme: 'vs-dark',
        automaticLayout: true,
        minimap: { enabled: false },
        fontSize: 14,
        fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
        scrollBeyondLastLine: false,
        roundedSelection: false,
        padding: { top: 16, bottom: 16 }
    });

    // Auto-save draft to local storage
    editor.onDidChangeModelContent(() => {
        localStorage.setItem(draftKey, editor.getValue());
    });
});

function resetCode() {
    if (confirm('Are you sure you want to reset your code? This will clear your current draft.')) {
        const defaultCode = `public class Solution {
    public static void main(String[] args) {
        // Write your code here
        
    }
}`;
        editor.setValue(defaultCode);
        localStorage.removeItem(`apricorn_draft_${PROBLEM_SLUG}`);
    }
}

function hideResult() {
    document.getElementById('resultConsole').classList.add('d-none');
}

function showResult() {
    document.getElementById('resultConsole').classList.remove('d-none');
}

function updateResultUI(status, detailsHtml, colorClass) {
    const resultStatus = document.getElementById('resultStatus');
    resultStatus.textContent = status;
    resultStatus.className = `mb-2 fs-5 font-monospace fw-bold ${colorClass}`;
    
    document.getElementById('resultDetails').innerHTML = detailsHtml;
}

async function submitCode() {
    const code = editor.getValue();
    if (!code || code.trim() === '') {
        alert('Please write some code before submitting.');
        return;
    }

    // Update UI
    const submitBtn = document.getElementById('submitBtn');
    const submitBtnText = document.getElementById('submitBtnText');
    const submitSpinner = document.getElementById('submitSpinner');
    
    submitBtn.disabled = true;
    submitSpinner.classList.remove('d-none');
    submitBtnText.textContent = ' Submitting...';

    showResult();
    updateResultUI('SUBMITTING...', 'Sending code to the judge...', 'text-secondary');

    try {
        const token = localStorage.getItem('token');
        if (!token) {
            updateResultUI('UNAUTHORIZED', 'You must be logged in to submit code. Please <a href="/login">login here</a>.', 'text-danger');
            resetSubmitBtn();
            return;
        }

        const response = await fetch(`/api/v1/problems/${PROBLEM_ID}/submissions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                language: 'JAVA',
                code: code
            })
        });

        if (response.status === 202) {
            const location = response.headers.get('Location');
            // location might be a full URL or relative. We can parse it.
            const parts = location.split('/');
            currentSubmissionId = parts[parts.length - 1];
            
            updateResultUI('PENDING', 'Submission received. Waiting for judge...', 'text-warning');
            startPolling();
        } else if (response.status === 401) {
            updateResultUI('UNAUTHORIZED', 'Session expired. Please <a href="/login">login again</a>.', 'text-danger');
            resetSubmitBtn();
        } else {
            const err = await response.json();
            updateResultUI('ERROR', `Failed to submit: ${err.message || 'Unknown error'}`, 'text-danger');
            resetSubmitBtn();
        }

    } catch (error) {
        console.error('Submit error:', error);
        updateResultUI('ERROR', 'Network error while submitting. Please try again.', 'text-danger');
        resetSubmitBtn();
    }
}

function startPolling() {
    if (pollInterval) clearInterval(pollInterval);

    pollInterval = setInterval(async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`/api/v1/problems/${PROBLEM_ID}/submissions/${currentSubmissionId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                
                if (data.status === 'COMPLETED') {
                    clearInterval(pollInterval);
                    pollInterval = null;
                    renderFinalVerdict(data);
                    resetSubmitBtn();
                } else if (data.status === 'ERROR') {
                    clearInterval(pollInterval);
                    pollInterval = null;
                    updateResultUI('INTERNAL ERROR', 'The judge encountered an internal error processing your submission.', 'text-danger');
                    resetSubmitBtn();
                } else {
                    // PENDING or RUNNING
                    updateResultUI(data.status, 'Executing test cases...', 'text-warning');
                }
            } else {
                clearInterval(pollInterval);
                pollInterval = null;
                updateResultUI('ERROR', 'Failed to fetch submission status.', 'text-danger');
                resetSubmitBtn();
            }
        } catch (error) {
            console.error('Polling error:', error);
        }
    }, 1500); // Poll every 1.5s
}

function renderFinalVerdict(data) {
    const verdict = data.verdict;
    let colorClass = 'text-danger';
    if (verdict === 'ACCEPTED') colorClass = 'text-success';
    else if (verdict === 'WRONG_ANSWER') colorClass = 'text-danger';
    else if (verdict === 'COMPILATION_ERROR') colorClass = 'text-warning';
    
    let detailsHtml = `<div class="mb-3">
        <div class="row">
            <div class="col-6"><strong>Time:</strong> ${data.executionTimeMs != null ? data.executionTimeMs + ' ms' : 'N/A'}</div>
            <div class="col-6"><strong>Memory:</strong> ${data.memoryUsedMb != null ? data.memoryUsedMb + ' MB' : 'N/A'}</div>
        </div>
    </div>`;

    if (verdict === 'COMPILATION_ERROR' && data.errorMessage) {
        detailsHtml += `<h6>Compilation Error:</h6>
        <pre class="bg-dark p-2 text-danger border border-danger">${data.errorMessage}</pre>`;
    } else if (data.errorMessage) {
        detailsHtml += `<h6>Error Details:</h6>
        <pre class="bg-dark p-2 text-warning border border-warning">${data.errorMessage}</pre>`;
    }

    updateResultUI(verdict.replace('_', ' '), detailsHtml, colorClass);
}

function resetSubmitBtn() {
    const submitBtn = document.getElementById('submitBtn');
    const submitBtnText = document.getElementById('submitBtnText');
    const submitSpinner = document.getElementById('submitSpinner');
    
    submitBtn.disabled = false;
    submitSpinner.classList.add('d-none');
    submitBtnText.innerHTML = '<i class="bi bi-play-fill"></i> Submit';
}
