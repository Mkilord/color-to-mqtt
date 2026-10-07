// Переключатель темы и всплывающие уведомления, общие для всех страниц.
(() => {
    const toggle = document.querySelector('[data-theme-toggle]');
    if (toggle) {
        toggle.addEventListener('click', () => {
            const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            document.documentElement.dataset.theme = next;
            try {
                localStorage.setItem('theme', next);
            } catch (e) {
                // тема просто не запомнится
            }
        });
    }
})();

let toastTimer = null;

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) {
        return;
    }
    toast.textContent = message;
    toast.dataset.type = type;
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove('show'), type === 'error' ? 6000 : 3000);
}

function formatTime(iso) {
    if (!iso) {
        return '';
    }
    return new Date(iso).toLocaleTimeString('ru-RU');
}
