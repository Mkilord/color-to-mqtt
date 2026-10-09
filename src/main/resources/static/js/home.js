// Главная: опрос /api/status, отображение цвета, состояния и ошибок, запуск и остановка.
(() => {
    const home = document.getElementById('home');
    const el = id => document.getElementById(id);
    const button = el('toggle-capture');
    let running = home.dataset.running === 'true';
    let timer = null;
    let busy = false;

    function renderColor(hex) {
        const rgb = ColorMath.hexToRgb(hex);
        const hsb = ColorMath.rgbToHsb(rgb);
        home.style.setProperty('--live', hex);
        el('color-hex').textContent = hex;
        el('color-rgb').textContent = `${rgb.r}, ${rgb.g}, ${rgb.b}`;
        el('color-hsb').textContent = `${Math.round(hsb.h)}°, ${Math.round(hsb.s)}%, ${Math.round(hsb.b)}%`;
    }

    function setState(node, text, kind) {
        node.textContent = text;
        node.dataset.kind = kind;
        node.hidden = false;
    }

    function hintFor(error) {
        if (/подключиться|потеряно|authoriz|connect/i.test(error)) {
            return 'Проверьте адрес брокера, логин и пароль.';
        }
        if (/захват/i.test(error)) {
            return 'Захват экрана работает только на рабочем столе с графической оболочкой.';
        }
        return '';
    }

    function render(status) {
        running = status.running;
        home.dataset.running = String(running);
        renderColor(status.color);

        setState(el('capture-state'), running ? 'Выполняется' : 'Остановлен', running ? 'ok' : 'idle');
        el('broker').textContent = status.broker || 'не задан';
        el('broker').title = status.broker || '';
        el('topic-text').textContent = status.topic || 'не задан';

        const mqtt = el('mqtt-state');
        if (status.connected === null) {
            mqtt.hidden = true;
        } else {
            setState(mqtt, status.connected ? 'подключен' : 'нет соединения', status.connected ? 'ok' : 'error');
        }

        const perf = status.performance;
        if (perf) {
            el('performance-fps').textContent = perf.idle
                ? `${perf.fps} кадров/с, экран не меняется`
                : `${perf.fps} кадров/с`;
            el('performance-detail').textContent = `снимок ${perf.captureMs} мс, расчет ${perf.processMs} мс`;
        } else {
            el('performance-fps').textContent = '—';
            el('performance-detail').textContent = 'появится после запуска';
        }

        el('last-payload').textContent = status.lastPayload || 'Еще не отправлялось';
        el('last-payload').title = status.lastPayload || '';
        el('last-sent').textContent = status.lastSentAt ? `в ${formatTime(status.lastSentAt)}` : '';

        const banner = el('error-banner');
        if (status.error) {
            el('error-text').textContent = status.error;
            el('error-time').textContent = status.errorAt ? `В ${formatTime(status.errorAt)}.` : '';
            el('error-hint').textContent = hintFor(status.error);
            banner.hidden = false;
        } else {
            banner.hidden = true;
        }

        button.textContent = running ? 'Остановить захват' : 'Запустить захват';
        button.classList.toggle('stop', running);
    }

    async function refresh() {
        clearTimeout(timer);
        try {
            const response = await fetch('/api/status', {cache: 'no-store'});
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            render(await response.json());
        } catch (e) {
            el('error-text').textContent = 'Нет связи с приложением. Проверьте, что оно запущено.';
            el('error-time').textContent = '';
            el('error-hint').textContent = '';
            el('error-banner').hidden = false;
        }
        timer = setTimeout(refresh, running ? 700 : 3000);
    }

    button.addEventListener('click', async () => {
        if (busy) {
            return;
        }
        busy = true;
        button.disabled = true;
        try {
            const response = await fetch(running ? '/api/capture/stop' : '/api/capture/start', {method: 'POST'});
            const data = await response.json().catch(() => ({}));
            showToast(data.message || data.error || 'HTTP ' + response.status, response.ok ? 'success' : 'error');
        } catch (e) {
            showToast('Нет связи с приложением', 'error');
        } finally {
            busy = false;
            button.disabled = false;
            refresh();
        }
    });

    // ---- Профили ----
    const chips = el('profile-chips');

    function renderProfiles(data) {
        chips.replaceChildren(...data.profiles.map(name => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'profile-chip';
            chip.dataset.profile = name;
            chip.textContent = name;
            chip.setAttribute('aria-pressed', String(name === data.active));
            return chip;
        }));
        el('profile-edit').href = '/settings?profile=' + encodeURIComponent(data.active);
    }

    chips.addEventListener('click', async event => {
        const chip = event.target.closest('[data-profile]');
        if (!chip || chip.getAttribute('aria-pressed') === 'true' || chips.dataset.busy) {
            return;
        }
        chips.dataset.busy = 'true';
        try {
            const response = await fetch('/api/profiles/activate', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({name: chip.dataset.profile})
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || 'HTTP ' + response.status);
            }
            renderProfiles(data);
            showToast(running ? `Профиль «${data.active}», захват перезапущен` : `Профиль «${data.active}»`);
            refresh();
        } catch (e) {
            showToast('Не удалось сменить профиль: ' + e.message, 'error');
        } finally {
            delete chips.dataset.busy;
        }
    });

    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            refresh();
        }
    });

    refresh();
})();
