document.addEventListener('DOMContentLoaded', () => {

    // 0. Responsive Hamburger Menu & Collapsible Sidebar Controller
    const toggleBtn = document.getElementById('sidebarToggleBtn');
    const backdrop = document.getElementById('sidebarBackdrop');
    const MOBILE_BREAKPOINT = 1024;

    function applyInitialSidebarState() {
        if (window.innerWidth <= MOBILE_BREAKPOINT) {
            document.body.classList.remove('sidebar-mobile-open');
            if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
        } else {
            const savedCollapsed = localStorage.getItem('myfinance_sidebar_collapsed') === 'true';
            document.body.classList.toggle('sidebar-collapsed', savedCollapsed);
            if (toggleBtn) toggleBtn.setAttribute('aria-expanded', String(!savedCollapsed));
        }
    }

    applyInitialSidebarState();

    if (toggleBtn) {
        toggleBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (window.innerWidth <= MOBILE_BREAKPOINT) {
                const isOpen = document.body.classList.toggle('sidebar-mobile-open');
                toggleBtn.setAttribute('aria-expanded', String(isOpen));
            } else {
                const isCollapsed = document.body.classList.toggle('sidebar-collapsed');
                localStorage.setItem('myfinance_sidebar_collapsed', String(isCollapsed));
                toggleBtn.setAttribute('aria-expanded', String(!isCollapsed));
            }
        });
    }


    const mobileOpenBtn = document.getElementById('mobileSidebarOpenBtn');
    if (mobileOpenBtn) {
        mobileOpenBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            document.body.classList.add('sidebar-mobile-open');
            if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'true');
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', () => {
            document.body.classList.remove('sidebar-mobile-open');
            if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
        });
    }

    // Close mobile drawer automatically when a menu tab is selected
    document.querySelectorAll('.sidebar [data-tab-target]').forEach(navBtn => {
        navBtn.addEventListener('click', () => {
            if (window.innerWidth <= MOBILE_BREAKPOINT) {
                document.body.classList.remove('sidebar-mobile-open');
                if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
            }
        });
    });

    // Smoothly adapt when browser window resizes/expands across breakpoints
    window.addEventListener('resize', () => {
        if (window.innerWidth > MOBILE_BREAKPOINT) {
            document.body.classList.remove('sidebar-mobile-open');
            const savedCollapsed = localStorage.getItem('myfinance_sidebar_collapsed') === 'true';
            document.body.classList.toggle('sidebar-collapsed', savedCollapsed);
            if (toggleBtn) toggleBtn.setAttribute('aria-expanded', String(!savedCollapsed));
        } else {
            document.body.classList.remove('sidebar-collapsed');
            if (toggleBtn) {
                toggleBtn.setAttribute('aria-expanded', String(document.body.classList.contains('sidebar-mobile-open')));
            }
        }
    });


    // 0B. Glassmorphic Modal Popup Controller (Finance Entry & Credential Vault)
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        modal.classList.add('is-open');
        document.body.style.overflow = 'hidden';
        const firstInput = modal.querySelector('input:not([type="hidden"]):not([type="file"])');
        if (firstInput) setTimeout(() => firstInput.focus(), 80);
    }

    function closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        modal.classList.remove('is-open');
        document.body.style.overflow = '';
    }

    document.querySelectorAll('[data-open-modal]').forEach(btn => {
        btn.addEventListener('click', () => openModal(btn.dataset.openModal));
    });

    document.querySelectorAll('[data-close-modal]').forEach(btn => {
        btn.addEventListener('click', () => closeModal(btn.dataset.closeModal));
    });

    document.querySelectorAll('.modal-backdrop').forEach(backdropEl => {
        backdropEl.addEventListener('click', (e) => {
            if (e.target === backdropEl) {
                closeModal(backdropEl.id);
            }
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-backdrop.is-open').forEach(m => closeModal(m.id));
        }
    });

    if (document.body.dataset.hasFieldErrors === 'true') {
        openModal('financeModal');
    }


    // 0C. Encrypted Local Cache System (Remembers Identifier + Password in Browser Storage)
    const CACHE_STORAGE_KEY = 'myfinance_auth_cache_v1';
    const CACHE_KEY_SEED = 'MyFinance-Local-Device-Cache-Seed-2026';

    async function getDeviceCacheCryptoKey() {
        const enc = new TextEncoder();
        const keyMaterial = await window.crypto.subtle.importKey(
            'raw',
            enc.encode(CACHE_KEY_SEED),
            { name: 'PBKDF2' },
            false,
            ['deriveKey']
        );
        return window.crypto.subtle.deriveKey(
            {
                name: 'PBKDF2',
                salt: enc.encode('myfinance-salt-v1'),
                iterations: 100000,
                hash: 'SHA-256'
            },
            keyMaterial,
            { name: 'AES-GCM', length: 256 },
            false,
            ['encrypt', 'decrypt']
        );
    }

    async function saveCredentialsToLocalCache(identifier, password) {
        if (!window.crypto || !window.crypto.subtle) return;
        const key = await getDeviceCacheCryptoKey();
        const iv = window.crypto.getRandomValues(new Uint8Array(12));
        const enc = new TextEncoder();
        const payload = JSON.stringify({ identifier, password, savedAt: Date.now() });
        const cipherBuf = await window.crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, enc.encode(payload));

        const ivB64 = btoa(String.fromCharCode(...iv));
        const cipherB64 = btoa(String.fromCharCode(...new Uint8Array(cipherBuf)));
        localStorage.setItem(CACHE_STORAGE_KEY, JSON.stringify({ iv: ivB64, data: cipherB64 }));
    }

    async function loadCredentialsFromLocalCache() {
        const raw = localStorage.getItem(CACHE_STORAGE_KEY);
        if (!raw || !window.crypto || !window.crypto.subtle) return null;
        try {
            const parsed = JSON.parse(raw);
            const key = await getDeviceCacheCryptoKey();
            const iv = Uint8Array.from(atob(parsed.iv), c => c.charCodeAt(0));
            const cipherBytes = Uint8Array.from(atob(parsed.data), c => c.charCodeAt(0));
            const plainBuf = await window.crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, cipherBytes);
            return JSON.parse(new TextDecoder().decode(plainBuf));
        } catch (_) {
            localStorage.removeItem(CACHE_STORAGE_KEY);
            return null;
        }
    }

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        const userField = document.getElementById('username');
        const pwdField = document.getElementById('password');
        const rememberCheck = document.getElementById('rememberLocalCache');
        const cachedBanner = document.getElementById('cachedAccountBanner');
        const cachedLabel = document.getElementById('cachedAccountLabel');
        const clearCacheBtn = document.getElementById('clearLocalCacheBtn');
        const toggleLoginPwdBtn = document.getElementById('toggleLoginPwdBtn');

        if (toggleLoginPwdBtn && pwdField) {
            toggleLoginPwdBtn.addEventListener('click', () => {
                const isPwd = pwdField.type === 'password';
                pwdField.type = isPwd ? 'text' : 'password';
                toggleLoginPwdBtn.textContent = isPwd ? 'Hide' : 'Show';
            });
        }

        // Restore cached username/mobile/email & password automatically on page load
        loadCredentialsFromLocalCache().then(cached => {
            if (cached && cached.identifier && cached.password) {
                if (userField) userField.value = cached.identifier;
                if (pwdField) pwdField.value = cached.password;
                if (rememberCheck) rememberCheck.checked = true;
                if (cachedBanner && cachedLabel) {
                    cachedLabel.textContent = cached.identifier;
                    cachedBanner.style.display = 'flex';
                }
            }
        });

        if (clearCacheBtn) {
            clearCacheBtn.addEventListener('click', () => {
                localStorage.removeItem(CACHE_STORAGE_KEY);
                if (userField) userField.value = '';
                if (pwdField) pwdField.value = '';
                if (cachedBanner) cachedBanner.style.display = 'none';
            });
        }

        loginForm.addEventListener('submit', async (e) => {
            if (rememberCheck && rememberCheck.checked && userField && pwdField) {
                e.preventDefault();
                await saveCredentialsToLocalCache(userField.value.trim(), pwdField.value);
                loginForm.submit();
            } else {
                localStorage.removeItem(CACHE_STORAGE_KEY);
            }
        });
    }

    // 0D. Edit Money Record Mode in Finance Modal
    const financeFormEl = document.getElementById('financeItemForm');
    const financeSubmitLabel = document.getElementById('financeSubmitLabel');
    const editStatusGroup = document.getElementById('editStatusGroup');
    const statusSelect = document.getElementById('statusSelect');
    const openFinanceModalBtn = document.getElementById('openFinanceModalBtn');

    const tabPlaceholders = {
        LEND: { title: 'e.g., Lent to Karthik for Emergency', cat: 'e.g., Karthik / Friend' },
        SPEND: { title: 'e.g., Office Internet & Electricity Bill', cat: 'e.g., ACT Fibernet / Utilities' },
        SALARY: { title: 'e.g., Monthly Software Engineer Salary', cat: 'e.g., Eshiam Software Pvt Ltd' },
        SAVINGS: { title: 'e.g., Emergency Reserve / Mutual Fund SIP', cat: 'e.g., HDFC Savings / Zerodha' },
        FINE: { title: 'e.g., Traffic Signal Challan / Late Fee', cat: 'e.g., RTA / Credit Card Fee' },
        BORROW: { title: 'e.g., Borrowed from Suresh for Travel', cat: 'e.g., Suresh / Personal' },
        EMI: { title: 'e.g., Home Loan Monthly EMI', cat: 'e.g., HDFC Home Loans' },
        LOAN: { title: 'e.g., Personal Term Loan Principal', cat: 'e.g., ICICI Bank Loan' },
        ALL: { title: 'Enter title or description', cat: 'e.g., HDFC Bank, Karthik, Employer' }
    };

    function applyTabContextToModal(tabKey) {
        const key = (tabKey || 'ALL').toUpperCase();
        const targetType = (key === 'ALL' || key === 'VAULT') ? 'LEND' : key;
        syncCustomSelect('itemType', targetType);

        if (dynamicFormTitle) {
            dynamicFormTitle.textContent = key === 'ALL' ? 'Add Financial Entry' : 'Add ' + key + ' Entry';
        }
        if (financeFormEl) {
            const titleInput = financeFormEl.querySelector('[name="title"]');
            const catInput = financeFormEl.querySelector('[name="category"]');
            const ph = tabPlaceholders[key] || tabPlaceholders.ALL;
            if (titleInput) titleInput.placeholder = ph.title;
            if (catInput) catInput.placeholder = ph.cat;
        }
    }

    function resetFinanceModalToCreateMode() {
        if (!financeFormEl) return;
        financeFormEl.action = '/dashboard/items';
        financeFormEl.reset();
        const preview = document.getElementById('imagePreviewBox');
        if (preview) preview.innerHTML = '';
        if (editStatusGroup) editStatusGroup.style.display = 'none';
        if (financeSubmitLabel) financeSubmitLabel.textContent = 'Save Entry & Arm Due Alert';
        applyTabContextToModal(currentActiveTab);
    }

    if (openFinanceModalBtn) {
        openFinanceModalBtn.addEventListener('click', () => {
            resetFinanceModalToCreateMode();
        });
    }

    document.querySelectorAll('.edit-finance-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            if (!financeFormEl) return;
            const id = btn.dataset.id;
            const type = btn.dataset.itemType || 'LEND';
            const title = btn.dataset.title || '';
            const category = btn.dataset.category || '';
            const amount = btn.dataset.amount || '';
            const dueDateStr = btn.dataset.dueDate || '';
            const status = btn.dataset.status || 'PENDING';
            const notes = btn.dataset.notes || '';

            financeFormEl.action = `/dashboard/items/${id}/edit`;
            syncCustomSelect('itemType', type);

            const titleInput = financeFormEl.querySelector('[name="title"]');
            const catInput = financeFormEl.querySelector('[name="category"]');
            const amtInput = financeFormEl.querySelector('[name="amount"]');
            const dueDisp = document.getElementById('dueDateDisplay');
            const dueHid = document.getElementById('dueDate');
            const notesInput = financeFormEl.querySelector('[name="notes"]');

            if (titleInput) titleInput.value = title;
            if (catInput) catInput.value = category;
            if (amtInput) amtInput.value = amount;
            if (dueDisp) {
                dueDisp.value = dueDateStr;
                if (dueHid) dueHid.value = dueDateStr ? (parseDdMmYyyyToIso(dueDateStr) || '') : '';
            }
            if (notesInput) notesInput.value = notes;
            if (editStatusGroup && statusSelect) {
                editStatusGroup.style.display = 'block';
                statusSelect.value = status;
            }
            if (dynamicFormTitle) dynamicFormTitle.textContent = `Edit ${type} Entry`;
            if (financeSubmitLabel) financeSubmitLabel.textContent = 'Update Record';

            openModal('financeModal');
        });
    });

    // 1. Theme Switcher Persistence
    const savedTheme = localStorage.getItem('myfinance_theme') || 'rose-finance';
    document.body.setAttribute('data-theme', savedTheme);
    document.querySelectorAll('.theme-btn').forEach(btn => {
        if (btn.dataset.themeChoice === savedTheme) btn.classList.add('active');
        btn.addEventListener('click', () => {
            const chosen = btn.dataset.themeChoice;
            document.body.setAttribute('data-theme', chosen);
            localStorage.setItem('myfinance_theme', chosen);
            document.querySelectorAll('.theme-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });

    // 2. Modern Custom Select Dropdown Engine
    function syncCustomSelect(selectId, value) {
        const wrapper = document.querySelector(`.custom-select-wrapper[data-target-select="${selectId}"]`);
        const nativeSelect = document.getElementById(selectId);
        if (!wrapper || !nativeSelect) return;

        nativeSelect.value = value;
        const options = wrapper.querySelectorAll('.custom-option');
        options.forEach(opt => {
            const isMatch = opt.dataset.value === value;
            opt.classList.toggle('selected', isMatch);
            if (isMatch) {
                const triggerLabel = wrapper.querySelector('[data-trigger-label]');
                const triggerIcon = wrapper.querySelector('[data-trigger-icon]');
                if (triggerLabel) triggerLabel.textContent = opt.querySelector('span:last-child').textContent;
                if (triggerIcon && opt.dataset.icon) triggerIcon.textContent = opt.dataset.icon;
            }
        });
    }

    document.querySelectorAll('.custom-select-wrapper').forEach(wrapper => {
        const selectId = wrapper.dataset.targetSelect;
        const trigger = wrapper.querySelector('.custom-select-trigger');
        const options = wrapper.querySelectorAll('.custom-option');

        trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = wrapper.classList.contains('open');
            document.querySelectorAll('.custom-select-wrapper.open').forEach(w => w.classList.remove('open'));
            wrapper.classList.toggle('open', !isOpen);
            trigger.setAttribute('aria-expanded', String(!isOpen));
        });

        trigger.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                trigger.click();
            } else if (e.key === 'Escape') {
                wrapper.classList.remove('open');
            }
        });

        options.forEach(opt => {
            opt.addEventListener('click', (e) => {
                e.stopPropagation();
                syncCustomSelect(selectId, opt.dataset.value);
                wrapper.classList.remove('open');
                trigger.setAttribute('aria-expanded', 'false');
            });
        });
    });

    document.addEventListener('click', () => {
        document.querySelectorAll('.custom-select-wrapper.open').forEach(w => {
            w.classList.remove('open');
            const t = w.querySelector('.custom-select-trigger');
            if (t) t.setAttribute('aria-expanded', 'false');
        });
    });

    // 3. Separate Tabs Controller (ALL, LEND, SPEND, SALARY, FINE, BORROW, EMI, LOAN, VAULT)
    const financeWorkspace = document.getElementById('financeWorkspace');
    const vaultWorkspace = document.getElementById('vaultWorkspace');
    const dynamicFormTitle = document.getElementById('dynamicFormTitle');
    const openFinanceModalLabel = document.getElementById('openFinanceModalLabel');
    const dynamicLedgerTitle = document.getElementById('dynamicLedgerTitle');
    const ledgerRows = document.querySelectorAll('.finance-row');
    const emptyFilterRow = document.getElementById('emptyFilterRow');

    const tabLabels = {
        ALL: 'All Finance Records',
        LEND: 'Money Lent (Receivables)',
        SPEND: 'Daily & Operational Spend',
        SALARY: 'Salary & Payroll Credits',
        SAVINGS: 'Savings & Wealth Reserves',
        FINE: 'Fines & Penalties',
        BORROW: 'Money Borrowed (Payables)',
        EMI: 'Monthly EMI Installments',
        LOAN: 'Loans & Principal Agreements',
        VAULT: 'Application Credentials Vault'
    };

    function activateTab(tabName) {
        const normalized = (tabName || 'ALL').toUpperCase();
        currentActiveTab = normalized;
        document.body.dataset.initialTab = normalized;

        document.querySelectorAll('[data-tab-target]').forEach(el => {
            el.classList.toggle('active', el.dataset.tabTarget === normalized);
        });

        const analyticsSection = document.getElementById('analyticsSection');
        if (normalized === 'VAULT') {
            if (financeWorkspace) financeWorkspace.classList.add('hidden-section');
            if (analyticsSection) analyticsSection.classList.add('hidden-section');
            if (vaultWorkspace) vaultWorkspace.classList.remove('hidden-section');
            return;
        }

        if (financeWorkspace) financeWorkspace.classList.remove('hidden-section');
        if (analyticsSection) analyticsSection.classList.remove('hidden-section');
        if (vaultWorkspace) vaultWorkspace.classList.add('hidden-section');

        if (dynamicFormTitle) {
            dynamicFormTitle.textContent = normalized === 'ALL'
                ? 'Add Financial Entry'
                : 'Add ' + normalized + ' Entry';
        }
        if (openFinanceModalLabel) {
            openFinanceModalLabel.textContent = normalized === 'ALL'
                ? 'Add Entry'
                : 'Add ' + normalized;
        }
        if (dynamicLedgerTitle) {
            dynamicLedgerTitle.textContent = tabLabels[normalized] || normalized;
        }
        applyTabContextToModal(normalized);

        let visibleCount = 0;
        ledgerRows.forEach(row => {
            const rowType = (row.dataset.itemType || '').toUpperCase();
            const show = normalized === 'ALL' || rowType === normalized;
            row.style.display = show ? '' : 'none';
            if (show) visibleCount++;
        });

        if (emptyFilterRow) {
            emptyFilterRow.style.display = visibleCount === 0 ? '' : 'none';
        }
    }

    document.querySelectorAll('[data-tab-target]').forEach(el => {
        el.addEventListener('click', (e) => {
            e.preventDefault();
            const target = el.dataset.tabTarget;
            activateTab(target);
            const url = new URL(window.location);
            url.searchParams.set('tab', target);
            window.history.replaceState({}, '', url);
        });
    });

    let currentActiveTab = (document.body.dataset.initialTab || 'ALL').toUpperCase();
    activateTab(currentActiveTab);

    // 4. Credential Vault Password Reveal & Copy
    document.querySelectorAll('.toggle-secret-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.dataset.secretId;
            const span = document.getElementById(targetId);
            if (!span) return;
            const isMasked = span.dataset.masked === 'true';
            if (isMasked) {
                span.textContent = span.dataset.realSecret;
                span.dataset.masked = 'false';
                btn.textContent = 'Hide';
            } else {
                span.textContent = '••••••••••••';
                span.dataset.masked = 'true';
                btn.textContent = 'Show';
            }
        });
    });

    document.querySelectorAll('.copy-secret-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            const textToCopy = btn.dataset.copyValue || '';
            try {
                await navigator.clipboard.writeText(textToCopy);
                const orig = btn.textContent;
                btn.textContent = 'Copied!';
                setTimeout(() => { btn.textContent = orig; }, 1400);
            } catch (_) {
                btn.textContent = 'Error';
            }
        });
    });

    // 5. Modern Drag & Drop Receipt Image Uploader + Preview Card
    const fileInput = document.getElementById('attachmentInput');
    const dropzone = document.getElementById('receiptDropzone');
    const previewBox = document.getElementById('imagePreviewBox');

    function handleSelectedFile(file) {
        if (!previewBox || !fileInput) return;
        previewBox.innerHTML = '';
        if (!file) return;

        const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            showFieldError(dropzone, 'Only JPEG, PNG, or WEBP images are permitted.');
            fileInput.value = '';
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            showFieldError(dropzone, 'Image must be 5MB or smaller.');
            fileInput.value = '';
            return;
        }
        clearFieldError(dropzone);

        const sizeKB = (file.size / 1024).toFixed(1) + ' KB';
        const objectUrl = URL.createObjectURL(file);

        const card = document.createElement('div');
        card.className = 'file-preview-card';
        card.innerHTML = `
            <div class="file-preview-info">
                <img src="${objectUrl}" alt="Receipt Preview">
                <div class="file-preview-meta">
                    <strong title="${file.name}">${file.name}</strong>
                    <small>${sizeKB} · Ready to attach</small>
                </div>
            </div>
            <button type="button" class="file-remove-btn">Remove</button>
        `;
        card.querySelector('.file-remove-btn').addEventListener('click', (e) => {
            e.stopPropagation();
            fileInput.value = '';
            previewBox.innerHTML = '';
        });
        previewBox.appendChild(card);
    }

    if (dropzone && fileInput) {
        dropzone.addEventListener('click', () => fileInput.click());
        dropzone.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                fileInput.click();
            }
        });

        ['dragenter', 'dragover'].forEach(evtName => {
            dropzone.addEventListener(evtName, (e) => {
                e.preventDefault();
                dropzone.classList.add('drag-over');
            });
        });

        ['dragleave', 'drop'].forEach(evtName => {
            dropzone.addEventListener(evtName, (e) => {
                e.preventDefault();
                dropzone.classList.remove('drag-over');
            });
        });

        dropzone.addEventListener('drop', (e) => {
            const files = e.dataTransfer.files;
            if (files && files.length > 0) {
                fileInput.files = files;
                handleSelectedFile(files[0]);
            }
        });

        fileInput.addEventListener('change', () => {
            handleSelectedFile(fileInput.files[0]);
        });
    }

    
    // 5B. Smart dd-mm-yyyy Date Input Auto-Formatter (Optional Field)
    const dueDateDisplayInput = document.getElementById('dueDateDisplay');
    const dueDateHiddenInput = document.getElementById('dueDate');

    function parseDdMmYyyyToIso(val) {
        const match = /^(\d{2})-(\d{2})-(\d{4})$/.exec(val);
        if (!match) return null;
        const day = parseInt(match[1], 10);
        const month = parseInt(match[2], 10);
        const year = parseInt(match[3], 10);
        if (month < 1 || month > 12 || day < 1 || day > 31 || year < 1900 || year > 2100) return null;
        const dt = new Date(year, month - 1, day);
        if (dt.getFullYear() !== year || dt.getMonth() !== month - 1 || dt.getDate() !== day) return null;
        return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }

    if (dueDateDisplayInput && dueDateHiddenInput) {
        dueDateDisplayInput.addEventListener('input', (e) => {
            let digits = e.target.value.replace(/\D/g, '').slice(0, 8);
            let formatted = digits;
            if (digits.length > 4) {
                formatted = digits.slice(0, 2) + '-' + digits.slice(2, 4) + '-' + digits.slice(4);
            } else if (digits.length > 2) {
                formatted = digits.slice(0, 2) + '-' + digits.slice(2);
            }
            e.target.value = formatted;

            if (formatted.length === 10) {
                const iso = parseDdMmYyyyToIso(formatted);
                dueDateHiddenInput.value = iso || '';
            } else {
                dueDateHiddenInput.value = '';
            }
        });
    }

    // 6. Client-Side Validation for Finance Form & Vault Form
    const financeForm = document.getElementById('financeItemForm');
    if (financeForm) {
        financeForm.addEventListener('submit', (e) => {
            let isValid = true;
            const title = financeForm.querySelector('[name="title"]');
            const category = financeForm.querySelector('[name="category"]');
            const amount = financeForm.querySelector('[name="amount"]');
            const dueDate = financeForm.querySelector('[name="dueDate"]');

            const dueDateDisplay = financeForm.querySelector('#dueDateDisplay');
            [title, category, amount, dueDateDisplay].forEach(clearFieldError);

            if (!title.value || title.value.trim().length < 3 || title.value.trim().length > 120) {
                showFieldError(title, 'Title must be between 3 and 120 characters.');
                isValid = false;
            }
            if (!category.value || category.value.trim().length === 0) {
                showFieldError(category, 'Person / Category is required.');
                isValid = false;
            }
            const numAmount = parseFloat(amount.value);
            if (isNaN(numAmount) || numAmount <= 0 || numAmount > 100000000) {
                showFieldError(amount, 'Enter a valid amount between 0.01 and 100,000,000.');
                isValid = false;
            }
            if (dueDateDisplay && dueDateDisplay.value.trim() !== '') {
                const isoVal = parseDdMmYyyyToIso(dueDateDisplay.value.trim());
                if (!isoVal) {
                    showFieldError(dueDateDisplay, 'Enter a valid date in dd-mm-yyyy format (e.g., 25-11-2026).');
                    isValid = false;
                } else {
                    dueDate.value = isoVal;
                }
            } else if (dueDate) {
                dueDate.value = '';
            }
            if (!isValid) e.preventDefault();
        });
    }

    const vaultForm = document.getElementById('vaultItemForm');
    if (vaultForm) {
        vaultForm.addEventListener('submit', (e) => {
            let isValid = true;
            const appTitle = vaultForm.querySelector('[name="appTitle"]');
            const loginUsername = vaultForm.querySelector('[name="loginUsername"]');
            const password = vaultForm.querySelector('[name="password"]');

            [appTitle, loginUsername, password].forEach(clearFieldError);

            if (!appTitle.value || appTitle.value.trim().length < 2) {
                showFieldError(appTitle, 'Application title must be at least 2 characters.');
                isValid = false;
            }
            if (!loginUsername.value || loginUsername.value.trim().length === 0) {
                showFieldError(loginUsername, 'Username or email is required.');
                isValid = false;
            }
            if (!password.value || password.value.length === 0) {
                showFieldError(password, 'Password is required.');
                isValid = false;
            }
            if (!isValid) e.preventDefault();
        });
    }

    function showFieldError(inputEl, message) {
        if (!inputEl) return;
        inputEl.classList.add('is-invalid');
        let errSpan = inputEl.parentElement.querySelector('.field-error');
        if (!errSpan) {
            errSpan = document.createElement('span');
            errSpan.className = 'field-error';
            inputEl.parentElement.appendChild(errSpan);
        }
        errSpan.textContent = message;
    }

    function clearFieldError(inputEl) {
        if (!inputEl) return;
        inputEl.classList.remove('is-invalid');
        const errSpan = inputEl.parentElement.querySelector('.field-error');
        if (errSpan) errSpan.remove();
    }

    // 7. Interactive Bar Graph, Hero Metrics & Pie Chart Renderer (Rose Finance Palette)
    function renderDashboardCharts() {
        const barContainer = document.getElementById('financeBarChart');
        const pieContainer = document.getElementById('financePieChart');
        const legendContainer = document.getElementById('financePieLegend');
        const totalBadge = document.getElementById('barChartTotalBadge');
        const heroBalanceEl = document.getElementById('heroTotalBalance');
        const savingsRatioEl = document.getElementById('savingsRatioText');
        const savingsBarEl = document.getElementById('savingsProgressBar');
        if (!barContainer || !pieContainer || !legendContainer) return;

        const sliceColors = {
            LEND: '#F472B6',
            SPEND: '#C084FC',
            SALARY: '#34D399',
            SAVINGS: '#E879F9',
            FINE: '#FB7185',
            BORROW: '#A855F7',
            EMI: '#F9A8D4',
            LOAN: '#818CF8'
        };

        const entries = [];
        document.querySelectorAll('.chart-data-node[data-tab-target]').forEach(node => {
            const key = node.dataset.tabTarget;
            const rawVal = parseFloat(node.dataset.chartVal || '0');
            entries.push({ key, value: isNaN(rawVal) ? 0 : rawVal, color: sliceColors[key] || '#F472B6' });
        });

        const totalSum = entries.reduce((acc, item) => acc + item.value, 0);
        const maxVal = Math.max(...entries.map(e => e.value), 1);

        const formattedTotal = '₹ ' + totalSum.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        if (totalBadge) totalBadge.textContent = 'Total: ' + formattedTotal;
        if (heroBalanceEl) heroBalanceEl.textContent = formattedTotal;

        const savingsEntry = entries.find(e => e.key === 'SAVINGS');
        const savingsVal = savingsEntry ? savingsEntry.value : 0;
        const savingsPct = totalSum > 0 ? Math.min(100, Math.round((savingsVal / totalSum) * 100)) : 0;
        if (savingsRatioEl) savingsRatioEl.textContent = savingsPct + '%';
        if (savingsBarEl) savingsBarEl.style.width = savingsPct + '%';

        // Render Bar Graph
        barContainer.innerHTML = '';
        entries.forEach(item => {
            const heightPct = item.value > 0 ? Math.max(8, Math.round((item.value / maxVal) * 100)) : 3;
            const shortVal = item.value >= 1000 ? '₹' + (item.value / 1000).toFixed(1) + 'k' : '₹' + item.value.toFixed(0);

            const col = document.createElement('div');
            col.className = 'bar-col-item';
            col.title = `${item.key}: ₹ ${item.value.toFixed(2)} (Click to filter)`;
            col.innerHTML = `
                <span class="bar-val-tip">${shortVal}</span>
                <div class="bar-track">
                    <div class="bar-fill" style="height:${heightPct}%;background:${item.color};"></div>
                </div>
                <span class="bar-label">${item.key}</span>
            `;
            col.addEventListener('click', () => activateTab(item.key));
            barContainer.appendChild(col);
        });

        // Render Centered SVG Donut / Pie Chart (160x160 viewport, r=54, stroke-width=22 -> strictly fits inside bounds)
        const radius = 54;
        const circumference = 2 * Math.PI * radius;
        let cumulativePct = 0;

        let circlesSvg = `<circle cx="80" cy="80" r="${radius}" fill="none" stroke="rgba(46, 26, 71, 0.65)" stroke-width="22"></circle>`;

        if (totalSum > 0) {
            entries.forEach(item => {
                if (item.value <= 0) return;
                const pct = item.value / totalSum;
                const dashLen = pct * circumference;
                const dashOffset = -cumulativePct * circumference;
                cumulativePct += pct;

                circlesSvg += `
                    <circle class="pie-slice" data-slice-tab="${item.key}"
                        cx="80" cy="80" r="${radius}"
                        fill="none"
                        stroke="${item.color}"
                        stroke-width="22"
                        stroke-dasharray="${dashLen.toFixed(2)} ${circumference.toFixed(2)}"
                        stroke-dashoffset="${dashOffset.toFixed(2)}"
                        transform="rotate(-90 80 80)">
                        <title>${item.key}: ₹ ${item.value.toFixed(2)} (${(pct * 100).toFixed(1)}%)</title>
                    </circle>
                `;
            });
        }

        const activeCount = entries.filter(e => e.value > 0).length;
        circlesSvg += `
            <text x="80" y="76" text-anchor="middle" fill="#FFFFFF" font-family="Manrope, sans-serif" font-size="13" font-weight="800">${activeCount} Active</text>
            <text x="80" y="93" text-anchor="middle" fill="#F472B6" font-family="DM Sans, sans-serif" font-size="9.5" font-weight="700">MODULES</text>
        `;

        pieContainer.innerHTML = `<svg width="152" height="152" viewBox="0 0 160 160" role="img" aria-label="Category allocation pie chart">${circlesSvg}</svg>`;

        pieContainer.querySelectorAll('.pie-slice').forEach(slice => {
            slice.addEventListener('click', () => activateTab(slice.dataset.sliceTab));
        });

        // Render Pie Legend
        legendContainer.innerHTML = '';
        entries.forEach(item => {
            const pctStr = totalSum > 0 ? ((item.value / totalSum) * 100).toFixed(1) + '%' : '0.0%';
            const row = document.createElement('div');
            row.className = 'pie-legend-item';
            row.title = `${item.key}: ₹ ${item.value.toFixed(2)}`;
            row.innerHTML = `
                <div class="pie-legend-left">
                    <span class="pie-dot" style="background:${item.color};"></span>
                    <span>${item.key}</span>
                </div>
                <span class="pie-pct">${pctStr}</span>
            `;
            row.addEventListener('click', () => activateTab(item.key));
            legendContainer.appendChild(row);
        });
    }

    renderDashboardCharts();
});
