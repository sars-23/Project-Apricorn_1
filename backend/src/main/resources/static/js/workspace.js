let editor;
let currentSubmissionId = null;
let pollInterval = null;
let lastSubmittedCode = null;

require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs' }});

require(['vs/editor/editor.main'], function() {
    const defaultCode = (typeof PROBLEM_STARTER_CODE !== 'undefined' && PROBLEM_STARTER_CODE) 
        ? PROBLEM_STARTER_CODE 
        : `class Solution {
    
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
        padding: { top: 16, bottom: 16 },
        formatOnType: true,
        formatOnPaste: true,
        autoIndent: "full",
        bracketPairColorization: { enabled: true },
        smoothScrolling: true
    });

    // Add Ctrl/Cmd + Enter to submit
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, function() {
        const submitBtn = document.getElementById('submitBtn');
        if (submitBtn && !submitBtn.disabled) {
            submitCode();
        }
    });

    // Auto-save draft to local storage and handle stale result panel
    editor.onDidChangeModelContent(() => {
        const currentCode = editor.getValue();
        localStorage.setItem(draftKey, currentCode);
        
        if (lastSubmittedCode !== null && currentCode !== lastSubmittedCode) {
            hideResult();
        }
    });
});

function resetCode() {
    if (confirm('Are you sure you want to reset your code? This will clear your current draft.')) {
        const codeToSet = (typeof PROBLEM_STARTER_CODE !== 'undefined' && PROBLEM_STARTER_CODE) 
            ? PROBLEM_STARTER_CODE 
            : `class Solution {
    
}`;
        editor.setValue(codeToSet);
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
    resultStatus.innerHTML = status;
    resultStatus.className = `mb-2 fs-5 font-monospace fw-bold ${colorClass}`;
    
    document.getElementById('resultDetails').innerHTML = detailsHtml;
}

async function submitCode() {
    if (!editor) {
        alert('Editor is not initialized yet. Please wait a moment and try again.');
        return;
    }

    const sourceCode = editor.getValue();
    if (!sourceCode || sourceCode.trim() === '') {
        alert('Please write some code before submitting.');
        return;
    }
    
    lastSubmittedCode = sourceCode;

    // Update UI
    const submitBtn = document.getElementById('submitBtn');
    const submitBtnText = document.getElementById('submitBtnText');
    const submitSpinner = document.getElementById('submitSpinner');
    
    submitBtn.disabled = true;
    submitSpinner.classList.remove('d-none');
    submitBtnText.textContent = ' Submitting...';

    showResult();
    updateResultUI('Queued...', 'Sending code to the judge...', 'text-secondary');

    try {
        const payload = {
            language: 'JAVA',
            sourceCode: sourceCode
        };

        const response = await fetch(`/api/v1/problems/${PROBLEM_SLUG}/submissions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.status === 202) {
            let data;
            try {
                data = await response.json();
            } catch (e) {
                updateResultUI('ERROR', 'Submission was received but the response could not be processed.', 'text-danger');
                resetSubmitBtn();
                return;
            }
            
            if (data && data.submissionId) {
                currentSubmissionId = data.submissionId;
                updateResultUI('Queued', 'Submission received. Waiting for judge...', 'text-warning');
                startPolling();
            } else {
                updateResultUI('ERROR', 'Submission created but no submission identifier was returned.', 'text-danger');
                resetSubmitBtn();
            }
        } else if (response.status === 401) {
            updateResultUI('UNAUTHORIZED', 'Session expired. Please <a href="/login">login again</a>.', 'text-danger');
            resetSubmitBtn();
        } else {
            let errMessage = 'Unknown error';
            try {
                const err = await response.json();
                errMessage = err.message || err.error || errMessage;
            } catch (e) {}
            updateResultUI('ERROR', `Failed to submit: ${errMessage}`, 'text-danger');
            resetSubmitBtn();
        }

    } catch (error) {
        console.error('Submit error:', error);
        updateResultUI('ERROR', 'Unable to connect to the server.', 'text-danger');
        resetSubmitBtn();
    }
}

function startPolling() {
    if (pollInterval) clearInterval(pollInterval);

    pollInterval = setInterval(async () => {
        try {
            const response = await fetch(`/api/v1/submissions/${currentSubmissionId}`);

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
                    let statusText = 'Pending...';
                    let detailText = 'Waiting in queue...';
                    let btnText = ' Pending...';
                    
                    if (data.status === 'RUNNING') {
                        statusText = 'Running Tests...';
                        detailText = 'Executing test cases against your code...';
                        btnText = ' Running...';
                    } else if (data.status === 'QUEUED' || data.status === 'PENDING') {
                        statusText = 'Compiling...';
                        detailText = 'Building your code...';
                        btnText = ' Compiling...';
                    }
                    
                    updateResultUI(statusText, detailText, 'text-info');
                    
                    const submitBtnText = document.getElementById('submitBtnText');
                    if (submitBtnText) submitBtnText.textContent = btnText;
                }
            } else {
                clearInterval(pollInterval);
                pollInterval = null;
                updateResultUI('ERROR', 'Failed to fetch submission status.', 'text-danger');
                resetSubmitBtn();
            }
        } catch (error) {
            console.error('Polling error:', error);
            clearInterval(pollInterval);
            pollInterval = null;
            updateResultUI('ERROR', 'Submission created, but status could not be retrieved.', 'text-danger');
            resetSubmitBtn();
        }
    }, 1500); // Poll every 1.5s
}

function renderFinalVerdict(data) {
    const verdict = data.verdict;
    let colorClass = 'text-primary';
    let iconHtml = '';
    let verdictText = verdict ? verdict.replace(/_/g, ' ') : 'UNKNOWN';

    if (verdict === 'ACCEPTED') {
        colorClass = 'text-success';
        iconHtml = '<i class="bi bi-check-lg"></i>';
    } else if (verdict === 'WRONG_ANSWER' || verdict === 'RUNTIME_ERROR') {
        colorClass = 'text-danger';
        iconHtml = '<i class="bi bi-x-lg"></i>';
    } else if (verdict === 'COMPILATION_ERROR' || verdict === 'TIME_LIMIT_EXCEEDED') {
        colorClass = 'text-warning';
        iconHtml = '<i class="bi bi-exclamation-triangle"></i>';
    } else if (verdict === 'INTERNAL_ERROR') {
        colorClass = 'text-danger';
        iconHtml = '<i class="bi bi-bug"></i>';
        verdictText = 'Judge Error';
    }

    const timeText = data.executionTimeMs != null ? (data.executionTimeMs === 0 ? '< 1 ms' : `${data.executionTimeMs} ms`) : 'N/A';
    const memoryText = data.memoryUsedKb != null ? `${data.memoryUsedKb} KB` : 'Not measured';
    const passedTests = data.passedTestCases != null ? data.passedTestCases : 0;
    const totalTests = data.totalTestCases != null ? data.totalTestCases : 0;
    
    let detailsHtml = `
        <div class="row g-3 mt-1 text-light">
            <div class="col-4">
                <div class="text-secondary small">Tests Passed</div>
                <div class="fs-6 fw-bold">${passedTests} / ${totalTests}</div>
            </div>
            <div class="col-4">
                <div class="text-secondary small">Runtime</div>
                <div class="fs-6 fw-bold">${timeText}</div>
            </div>
            <div class="col-4">
                <div class="text-secondary small">Memory</div>
                <div class="fs-6 fw-bold">${memoryText}</div>
            </div>
        </div>
    `;

    if (verdict === 'COMPILATION_ERROR' && data.errorMessage) {
        detailsHtml += `
        <div class="mt-3">
            <div class="text-secondary small mb-1">Compilation Error</div>
            <pre class="bg-black p-3 text-warning border border-secondary rounded font-monospace" style="white-space: pre-wrap; font-size: 0.85rem; max-height: 200px; overflow-y: auto;">${data.errorMessage}</pre>
        </div>`;
    } else if (data.errorMessage && verdict !== 'INTERNAL_ERROR') {
        if (data.errorMessage.includes('Failed Test Case:')) {
            // It's our formatted WRONG_ANSWER message from JavaDockerExecutor
            const parts = data.errorMessage.split('\\n\\n');
            let testCaseHtml = '';
            
            parts.forEach(part => {
                if (part.startsWith('Failed Test Case:\\n')) {
                    testCaseHtml += `<div class="mt-3"><div class="text-secondary small mb-1">Failed Test Case (Input)</div><pre class="bg-black p-2 text-light border border-secondary rounded font-monospace" style="white-space: pre-wrap; font-size: 0.85rem;">${part.replace('Failed Test Case:\\n', '')}</pre></div>`;
                } else if (part.startsWith('Expected Output:\\n')) {
                    testCaseHtml += `<div class="mt-2"><div class="text-secondary small mb-1">Expected Output</div><pre class="bg-black p-2 text-success border border-secondary rounded font-monospace" style="white-space: pre-wrap; font-size: 0.85rem;">${part.replace('Expected Output:\\n', '')}</pre></div>`;
                } else if (part.startsWith('Actual Output:\\n')) {
                    testCaseHtml += `<div class="mt-2"><div class="text-secondary small mb-1">Actual Output</div><pre class="bg-black p-2 text-danger border border-secondary rounded font-monospace" style="white-space: pre-wrap; font-size: 0.85rem;">${part.replace('Actual Output:\\n', '')}</pre></div>`;
                }
            });
            
            if (testCaseHtml !== '') {
                detailsHtml += testCaseHtml;
            } else {
                detailsHtml += `
                <div class="mt-3">
                    <div class="text-secondary small mb-1">Error Details</div>
                    <pre class="bg-black p-3 text-danger border border-secondary rounded font-monospace" style="white-space: pre-wrap; font-size: 0.85rem;">${data.errorMessage}</pre>
                </div>`;
            }
        } else {
            detailsHtml += `
            <div class="mt-3">
                <div class="text-secondary small mb-1">Error Details</div>
                <pre class="bg-black p-3 text-danger border border-secondary rounded font-monospace" style="white-space: pre-wrap; font-size: 0.85rem;">${data.errorMessage}</pre>
            </div>`;
        }
    }

    detailsHtml += `
        <div class="mt-3 pt-2 border-top border-secondary border-opacity-50 d-flex justify-content-between align-items-center">
            <span class="text-secondary small">${verdict === 'ACCEPTED' ? 'Solution accepted!' : 'Track your attempt progression'}</span>
            <a href="/problems/${PROBLEM_SLUG}/journey" class="btn btn-sm ${verdict === 'ACCEPTED' ? 'btn-success' : 'btn-outline-info'} rounded-pill px-3 fw-bold">
                <i class="bi bi-compass me-1"></i> ${verdict === 'ACCEPTED' ? 'View Solution Journey' : 'Review Attempt in Journey'}
            </a>
        </div>
    `;

    updateResultUI(`${iconHtml} ${verdictText}`, detailsHtml, colorClass);
}

function resetSubmitBtn() {
    const submitBtn = document.getElementById('submitBtn');
    const submitBtnText = document.getElementById('submitBtnText');
    const submitSpinner = document.getElementById('submitSpinner');
    
    submitBtn.disabled = false;
    submitSpinner.classList.add('d-none');
    submitBtnText.innerHTML = '<i class="bi bi-play-fill"></i> Submit';
}
