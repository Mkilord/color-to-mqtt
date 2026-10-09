// Страница настроек: профили, простой и расширенный режим, сохранение без перезагрузки,
// снимок области, проверка цвета и отправка цвета на лампы.
(() => {
    const el = id => document.getElementById(id);
    const page = el('settings');
    const form = el('settings-form');
    const profile = form.dataset.profile;
    const num = name => {
        const value = parseFloat(form.elements[name].value);
        return Number.isFinite(value) ? value : 0;
    };
    const postJson = async (url, body) => {
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.error || 'HTTP ' + response.status);
        }
        return data;
    };
    const settingsUrl = name => '/settings?profile=' + encodeURIComponent(name);

    let defaults = {};
    try {
        defaults = JSON.parse(el('settings-defaults').textContent || '{}');
    } catch (e) {
        defaults = {};
    }

    // Java отдает дробные значения как "2.0", в поле удобнее видеть "2".
    form.querySelectorAll('input[type="number"]').forEach(input => {
        const value = Number(input.value);
        if (input.value !== '' && Number.isFinite(value)) {
            input.value = String(value);
        }
    });

    // ---- Расширенный режим ----
    const advancedToggle = el('advanced-toggle');
    const ADVANCED_KEY = 'colortomqtt.advanced';

    const isDefault = input => {
        if (!(input.name in defaults)) {
            return true;
        }
        const expected = defaults[input.name];
        if (typeof expected === 'number') {
            return Number(input.value) === expected;
        }
        return String(input.value) === String(expected);
    };

    const advancedInputs = () => [...form.querySelectorAll('[data-advanced] [name]')]
        .filter(input => input.type !== 'hidden' && !input.closest('[hidden]:not([data-advanced])'));

    function markChanged() {
        form.querySelectorAll('[name]').forEach(input => {
            const field = input.closest('.field');
            if (field && input.name in defaults) {
                field.classList.toggle('changed', !isDefault(input));
            }
        });
        const changed = advancedInputs().filter(input => !isDefault(input)).length;
        el('advanced-changed').textContent = changed
            ? `Изменено от умолчаний: ${changed}`
            : 'Все по умолчанию';
        el('advanced-reset').hidden = !changed;
    }

    function setAdvanced(on, remember = true) {
        page.classList.toggle('show-advanced', on);
        advancedToggle.checked = on;
        if (remember) {
            try {
                localStorage.setItem(ADVANCED_KEY, on ? '1' : '0');
            } catch (e) {
                // Без хранилища режим просто не запомнится.
            }
        }
    }

    let storedAdvanced = false;
    try {
        storedAdvanced = localStorage.getItem(ADVANCED_KEY) === '1';
    } catch (e) {
        storedAdvanced = false;
    }
    // Ошибка в скрытом поле не должна прятаться.
    setAdvanced(storedAdvanced || !!form.querySelector('[data-advanced] .invalid'), false);
    advancedToggle.addEventListener('change', () => {
        setAdvanced(advancedToggle.checked);
        if (advancedToggle.checked) {
            el('advanced-group').scrollIntoView({behavior: 'smooth', block: 'start'});
        }
    });

    el('advanced-reset').addEventListener('click', () => {
        form.querySelectorAll('[data-advanced] [name]').forEach(input => {
            if (input.type !== 'hidden' && input.name in defaults) {
                input.value = String(defaults[input.name]);
                clearError(input);
            }
        });
        updateTracker();
        updateDetector();
        setDirty(true);
        markChanged();
        updatePreview();
        drawSnapshot();
        showToast('Расширенные настройки возвращены к умолчанию. Сохраните, чтобы применить.');
    });

    // ---- Подсказки к способам расчета и сравнения ----
    const TRACKER_HELP = {
        HSB_TOLERANCE:
            'Цвет отправляется, если тон, насыщенность или яркость изменились больше допуска.',
        RGB_DISTANCE:
            'Цвет отправляется, если расстояние между цветами в RGB больше порога.',
        ANY_CHANGE:
            'Цвет отправляется при любом изменении. Сообщений будет много, сглаживание вспышек почти не сработает.'
    };
    const DETECTOR_HELP = {
        DOMINANT:
            'Цвет, которого больше всего среди цветных точек. Темный фон и серое не учитываются: красный объект на темном фоне даст красный.',
        VIVID:
            'Среднее, где яркие насыщенные точки весят больше темных и серых. Несколько цветов смешиваются.',
        AVERAGE:
            'Среднее по всем точкам. Темный фон и серое делают цвет бледнее.'
    };
    const tracker = el('tracker');
    const detector = el('detector');

    function updateTracker() {
        el('tracker-help').textContent = TRACKER_HELP[tracker.value] || '';
        form.querySelectorAll('[data-for]').forEach(field => {
            field.hidden = field.dataset.for !== tracker.value;
        });
    }

    function updateDetector() {
        el('detector-help').textContent = DETECTOR_HELP[detector.value] || '';
        form.querySelectorAll('[data-for-detector]').forEach(field => {
            field.hidden = field.dataset.forDetector !== detector.value;
        });
    }

    // ---- Выбор цвета для проверки ----
    const sample = el('sample-color');
    const pickH = el('pick-h');
    const pickS = el('pick-s');
    const pickB = el('pick-b');
    const hexInput = el('picker-hex');

    function rgbCss(hsb) {
        return ColorMath.rgbToHex(ColorMath.hsbToRgb(hsb));
    }

    function paintPicker(hsb) {
        pickH.value = Math.round(hsb.h);
        pickS.value = Math.round(hsb.s);
        pickB.value = Math.round(hsb.b);
        el('pick-h-value').textContent = `${Math.round(hsb.h)}°`;
        el('pick-s-value').textContent = `${Math.round(hsb.s)}%`;
        el('pick-b-value').textContent = `${Math.round(hsb.b)}%`;
        pickS.style.setProperty('--track', `linear-gradient(to right, ${rgbCss({h: hsb.h, s: 0, b: Math.max(hsb.b, 40)})}, ${rgbCss({h: hsb.h, s: 100, b: Math.max(hsb.b, 40)})})`);
        pickB.style.setProperty('--track', `linear-gradient(to right, #000, ${rgbCss({h: hsb.h, s: hsb.s, b: 100})})`);
        el('picker-swatch').style.background = sample.value;
    }

    /**
     * @param hex    цвет #rrggbb
     * @param manual выбран вручную: живой снимок перестает подменять цвет
     */
    function setSample(hex, manual) {
        sample.value = hex.toLowerCase();
        if (document.activeElement !== hexInput) {
            hexInput.value = sample.value;
        }
        hexInput.classList.remove('invalid');
        paintPicker(ColorMath.rgbToHsb(ColorMath.hexToRgb(sample.value)));
        if (manual && el('snapshot-auto').checked) {
            stopAuto();
            showToast('Живой снимок выключен: выбран свой цвет');
        }
        updatePreview();
    }

    [pickH, pickS, pickB].forEach(slider => slider.addEventListener('input', () => {
        const hex = rgbCss({h: Number(pickH.value), s: Number(pickS.value), b: Number(pickB.value)});
        sample.value = hex;
        hexInput.value = hex;
        el('pick-h-value').textContent = `${pickH.value}°`;
        el('pick-s-value').textContent = `${pickS.value}%`;
        el('pick-b-value').textContent = `${pickB.value}%`;
        if (el('snapshot-auto').checked) {
            stopAuto();
        }
        el('picker-swatch').style.background = hex;
        pickS.style.setProperty('--track', `linear-gradient(to right, ${rgbCss({h: Number(pickH.value), s: 0, b: Math.max(Number(pickB.value), 40)})}, ${rgbCss({h: Number(pickH.value), s: 100, b: Math.max(Number(pickB.value), 40)})})`);
        pickB.style.setProperty('--track', `linear-gradient(to right, #000, ${rgbCss({h: Number(pickH.value), s: Number(pickS.value), b: 100})})`);
        updatePreview();
    }));

    hexInput.addEventListener('input', () => {
        let value = hexInput.value.trim();
        if (!value.startsWith('#')) {
            value = '#' + value;
        }
        if (/^#[0-9a-fA-F]{6}$/.test(value)) {
            setSample(value, true);
        } else {
            hexInput.classList.add('invalid');
        }
    });
    hexInput.addEventListener('blur', () => {
        hexInput.value = sample.value;
        hexInput.classList.remove('invalid');
    });

    document.querySelectorAll('[data-sample]').forEach(button => {
        button.addEventListener('click', () => setSample(button.dataset.sample, true));
    });

    el('sample-current').addEventListener('click', async () => {
        try {
            const status = await (await fetch('/api/status', {cache: 'no-store'})).json();
            if (!status.running) {
                showToast('Захват не запущен. Возьмите цвет со снимка.', 'error');
                return;
            }
            setSample(status.color, true);
        } catch (e) {
            showToast('Не удалось получить текущий цвет', 'error');
        }
    });

    // ---- Проверка цвета ----
    let lastSent = null;

    function setBar(channel, max, before, after, rangeMin, rangeMax) {
        const bar = el('bar-' + channel);
        const pct = value => `${ColorMath.clamp(value / max * 100, 0, 100)}%`;
        const low = Math.min(rangeMin, rangeMax);
        const high = Math.max(rangeMin, rangeMax);
        bar.querySelector('.bar-mask.left').style.width = pct(low);
        bar.querySelector('.bar-mask.right').style.left = pct(high);
        bar.querySelector('.marker.before').style.left = pct(before);
        bar.querySelector('.marker.after').style.left = pct(after);
    }

    function updatePreview() {
        const source = ColorMath.rgbToHsb(ColorMath.hexToRgb(sample.value));
        const range = {
            minHue: num('minHue'), maxHue: num('maxHue'),
            minSaturation: num('minSaturation'), maxSaturation: num('maxSaturation'),
            minBrightness: num('minBrightness'), maxBrightness: num('maxBrightness')
        };
        const zone = ColorMath.zoneOf(source, {black: num('blackThreshold'), gray: num('grayThreshold')});
        let modified = ColorMath.modify(source, {
            h: num('modifyHue'), s: num('modifySaturation'), b: num('modifyBrightness'), boost: num('saturationBoost'),
            hueShifts: ['hueShiftRed', 'hueShiftYellow', 'hueShiftGreen', 'hueShiftCyan', 'hueShiftBlue', 'hueShiftMagenta'].map(num)
        });
        let sent = ColorMath.limit(modified, range);
        const note = el('zone-note');
        if (zone === 'black') {
            modified = {h: 0, s: 0, b: 0};
            sent = {h: 0, s: 0, b: 0};
            note.textContent = 'Темнее порога черного: уйдет яркость 0, лампы погаснут.';
        } else if (zone === 'gray') {
            modified = {h: 0, s: 1, b: modified.b};
            sent = {h: 0, s: 1, b: sent.b};
            note.textContent = 'Бледнее порога серого: уйдет белый в цветном режиме.';
        }
        note.hidden = zone === 'color';

        el('swatch-source').style.background = sample.value;
        el('swatch-modified').style.background = rgbCss(modified);
        el('swatch-sent').style.background = rgbCss(sent);

        el('bar-s').style.background =
            `linear-gradient(to right, ${rgbCss({h: modified.h, s: 0, b: Math.max(modified.b, 60)})}, ${rgbCss({h: modified.h, s: 100, b: Math.max(modified.b, 60)})})`;
        el('bar-b').style.background =
            `linear-gradient(to right, #000, ${rgbCss({h: modified.h, s: modified.s, b: 100})})`;
        setBar('h', 360, modified.h, sent.h, range.minHue, range.maxHue);
        setBar('s', 100, modified.s, sent.s, range.minSaturation, range.maxSaturation);
        setBar('b', 100, modified.b, sent.b, range.minBrightness, range.maxBrightness);
        el('value-h').textContent = `${Math.round(sent.h)}°`;
        el('value-s').textContent = `${Math.round(sent.s)}%`;
        el('value-b').textContent = `${Math.round(sent.b)}%`;

        el('message-topic').textContent = form.elements['topic'].value || 'не задан';
        el('message-payload').textContent = ColorMath.payload(sent);
        lastSent = sent;
    }

    el('send-test').addEventListener('click', async () => {
        if (!lastSent) {
            return;
        }
        const button = el('send-test');
        button.disabled = true;
        try {
            const result = await postJson('/api/test-color', {
                hue: Math.round(lastSent.h),
                sat: Math.round(lastSent.s),
                brightness: Math.round(lastSent.b)
            });
            showToast('Отправлено: ' + result.payload);
        } catch (e) {
            showToast(e.message, 'error');
        } finally {
            button.disabled = false;
        }
    });

    // ---- Карта области ----
    let screenSize = null;

    function updateScreenMap() {
        const caption = el('screen-map-caption');
        const area = el('screen-map-area');
        const width = num('screenWidth');
        const height = num('screenHeight');
        if (!screenSize) {
            caption.textContent = `${width} × ${height} px по центру экрана`;
            return;
        }
        const w = Math.min(width, screenSize.width);
        const h = Math.min(height, screenSize.height);
        area.style.width = `${w / screenSize.width * 100}%`;
        area.style.height = `${h / screenSize.height * 100}%`;
        const clipped = w < width || h < height ? ', больше экрана и будет обрезана' : '';
        caption.textContent = `${w} × ${h} из ${screenSize.width} × ${screenSize.height} px${clipped}`;
    }

    fetch('/api/screen').then(r => r.ok ? r.json() : null).then(size => {
        if (size) {
            screenSize = size;
            el('screen-map').style.aspectRatio = `${size.width} / ${size.height}`;
        }
        updateScreenMap();
    }).catch(() => updateScreenMap());

    // ---- Снимок ----
    const canvas = el('snapshot-canvas');
    let lastImage = null;
    let autoTimer = null;
    let loading = false;

    function drawSnapshot() {
        if (!lastImage) {
            return;
        }
        const image = lastImage;
        const width = image.naturalWidth;
        const height = image.naturalHeight;
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(image, 0, 0);

        const cell = Math.max(1, Math.round(num('cellSize')));
        let points = ColorMath.samplePoints(el('processor').value, width, height, cell);
        if (!points.length) {
            points = [[Math.floor(width / 2), Math.floor(height / 2)]];
        }
        const data = ctx.getImageData(0, 0, width, height).data;
        const pixels = points.map(([x, y]) => {
            const i = (y * width + x) * 4;
            return {r: data[i], g: data[i + 1], b: data[i + 2]};
        });
        const result = ColorMath.detect(detector.value, pixels, num('dominantMinShare'));
        const color = ColorMath.rgbToHex(result.color);
        const usedSet = new Set(result.used);

        if (el('snapshot-grid').checked) {
            const radius = Math.max(1.5, width / 220);
            points.forEach(([x, y], i) => {
                const used = usedSet.has(i);
                ctx.beginPath();
                ctx.arc(x + 0.5, y + 0.5, used ? radius : radius * 0.6, 0, Math.PI * 2);
                ctx.fillStyle = used ? 'rgba(255,255,255,0.95)' : 'rgba(255,255,255,0.25)';
                ctx.fill();
                ctx.lineWidth = 1;
                ctx.strokeStyle = used ? 'rgba(0,0,0,0.8)' : 'rgba(0,0,0,0.25)';
                ctx.stroke();
            });
        }

        el('snapshot-empty').hidden = true;
        const text = el('snapshot-result');
        text.hidden = false;
        text.textContent = result.fallback
            ? `${color}: преобладающего цвета нет, взято среднее по ${points.length} точкам`
            : `${color}: учтено ${result.used.length} из ${points.length} точек`;
        setSample(color, false);
    }

    async function takeSnapshot() {
        if (loading) {
            return;
        }
        loading = true;
        el('snapshot-button').disabled = true;
        try {
            const params = new URLSearchParams({
                width: Math.max(1, Math.round(num('screenWidth'))),
                height: Math.max(1, Math.round(num('screenHeight')))
            });
            const response = await fetch('/api/snapshot?' + params, {cache: 'no-store'});
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            const url = URL.createObjectURL(await response.blob());
            const image = new Image();
            image.onload = () => {
                lastImage = image;
                drawSnapshot();
                URL.revokeObjectURL(url);
            };
            image.src = url;
        } catch (e) {
            stopAuto();
            showToast('Не удалось сделать снимок экрана. Захват работает только на рабочем столе.', 'error');
        } finally {
            loading = false;
            el('snapshot-button').disabled = false;
        }
    }

    function stopAuto() {
        clearInterval(autoTimer);
        autoTimer = null;
        el('snapshot-auto').checked = false;
    }

    el('snapshot-button').addEventListener('click', takeSnapshot);
    el('snapshot-grid').addEventListener('change', drawSnapshot);
    el('snapshot-auto').addEventListener('change', event => {
        if (event.target.checked) {
            takeSnapshot();
            autoTimer = setInterval(() => {
                if (!document.hidden) {
                    takeSnapshot();
                }
            }, 1000);
        } else {
            stopAuto();
        }
    });

    // ---- Пароль ----
    const passwordToggle = el('password-toggle');
    passwordToggle.addEventListener('click', () => {
        const input = el('password');
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        passwordToggle.textContent = show ? 'Скрыть' : 'Показать';
        passwordToggle.setAttribute('aria-pressed', String(show));
    });

    // ---- Несохраненные изменения ----
    let dirty = false;
    let leaving = false;

    function setDirty(value) {
        dirty = value;
        el('dirty-note').hidden = !value;
    }

    form.addEventListener('input', event => {
        setDirty(true);
        clearError(event.target);
        if (event.target.name === 'screenWidth' || event.target.name === 'screenHeight') {
            updateScreenMap();
        }
        if (['cellSize', 'detector', 'processor', 'dominantMinShare'].includes(event.target.name)) {
            drawSnapshot();
        }
        markChanged();
        updatePreview();
    });
    window.addEventListener('beforeunload', event => {
        if (dirty && !leaving) {
            event.preventDefault();
            event.returnValue = '';
        }
    });

    // ---- Сохранение без перезагрузки ----
    // Ошибки проверок диапазонов приходят по имени свойства, показываем их у поля «до».
    const ERROR_FIELD = {
        hueRangeValid: 'maxHue',
        saturationRangeValid: 'maxSaturation',
        brightnessRangeValid: 'maxBrightness',
        detectorKnown: 'detector',
        processorKnown: 'processor',
        stateTrackerKnown: 'stateTracker'
    };

    function clearError(input) {
        if (!input || !input.name) {
            return;
        }
        input.classList.remove('invalid');
        const field = input.closest('.field');
        field?.querySelectorAll('.field-error').forEach(node => node.remove());
    }

    function clearErrors() {
        form.querySelectorAll('.invalid').forEach(node => node.classList.remove('invalid'));
        form.querySelectorAll('.field .field-error').forEach(node => node.remove());
        el('form-errors')?.remove();
    }

    function showErrors(errors) {
        let first = null;
        Object.entries(errors).forEach(([name, message]) => {
            const input = form.elements[ERROR_FIELD[name] || name];
            if (!input || !input.closest) {
                return;
            }
            input.classList.add('invalid');
            const field = input.closest('.field');
            const node = document.createElement('span');
            node.className = 'field-error';
            node.textContent = message;
            field.appendChild(node);
            first = first || input;
        });
        // Ошибка в расширенном поле не должна остаться невидимой.
        if (form.querySelector('[data-advanced] .invalid')) {
            setAdvanced(true, false);
        }
        first?.scrollIntoView({behavior: 'smooth', block: 'center'});
        first?.focus({preventScroll: true});
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const button = el('save-button');
        button.disabled = true;
        clearErrors();
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                headers: {'X-Requested-With': 'fetch'},
                body: new URLSearchParams(new FormData(form))
            });
            const data = await response.json().catch(() => ({}));
            if (response.status === 422) {
                showErrors(data.errors || {});
                showToast('Не сохранено: исправьте отмеченные поля', 'error');
                return;
            }
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            setDirty(false);
            const password = el('password');
            password.value = '';
            password.placeholder = data.passwordSet ? 'Сохранен' : 'Не задан';
            form.elements['passwordSet'].value = String(data.passwordSet);
            showToast(data.message || 'Сохранено');
        } catch (e) {
            showToast('Не удалось сохранить: ' + e.message, 'error');
        } finally {
            button.disabled = false;
        }
    });

    el('reset-button').addEventListener('click', event => {
        if (!confirm(event.currentTarget.dataset.confirm)) {
            event.preventDefault();
        } else {
            leaving = true;
        }
    });

    // ---- Профили ----
    const profileSelect = el('profile-select');

    function confirmLeave() {
        if (dirty && !confirm('Есть несохраненные изменения. Уйти без сохранения?')) {
            return false;
        }
        leaving = true;
        return true;
    }

    profileSelect.addEventListener('change', () => {
        if (!confirmLeave()) {
            profileSelect.value = profile;
            return;
        }
        location.href = settingsUrl(profileSelect.value);
    });

    el('profile-activate').addEventListener('click', async () => {
        try {
            await postJson('/api/profiles/activate', {name: profile});
            el('profile-usage-other').hidden = true;
            el('profile-usage-current').hidden = false;
            showToast(`Профиль «${profile}» активен`);
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    const dialog = el('profile-dialog');
    let dialogMode = 'new';

    function openDialog(mode) {
        dialogMode = mode;
        const isNew = mode === 'new';
        el('profile-dialog-title').textContent = isNew ? 'Новый профиль' : 'Переименовать профиль';
        el('profile-dialog-ok').textContent = isNew ? 'Создать' : 'Переименовать';
        el('profile-dialog-copy-wrap').hidden = !isNew;
        el('profile-dialog-copy-text').textContent = `Скопировать сохраненные настройки профиля «${profile}»`;
        el('profile-dialog-name').value = isNew ? '' : profile;
        el('profile-dialog-error').hidden = true;
        dialog.showModal();
        el('profile-dialog-name').select();
    }

    el('profile-new').addEventListener('click', () => {
        if (dirty && !confirm('Несохраненные изменения не попадут в новый профиль. Продолжить?')) {
            return;
        }
        openDialog('new');
    });
    el('profile-rename').addEventListener('click', () => openDialog('rename'));
    el('profile-dialog-cancel').addEventListener('click', () => dialog.close());

    el('profile-dialog-form').addEventListener('submit', async event => {
        event.preventDefault();
        const name = el('profile-dialog-name').value.trim();
        const error = el('profile-dialog-error');
        try {
            if (dialogMode === 'new') {
                await postJson('/api/profiles', {name, copyFrom: el('profile-dialog-copy').checked ? profile : null});
            } else {
                await postJson('/api/profiles/rename', {from: profile, to: name});
            }
            leaving = true;
            location.href = settingsUrl(name);
        } catch (e) {
            error.textContent = e.message;
            error.hidden = false;
        }
    });

    el('profile-delete').addEventListener('click', async () => {
        if (!confirm(`Удалить профиль «${profile}»? Его настройки пропадут.`)) {
            return;
        }
        try {
            const data = await postJson('/api/profiles/delete', {name: profile});
            leaving = true;
            location.href = settingsUrl(data.active);
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    // ---- Старт ----
    tracker.addEventListener('change', updateTracker);
    detector.addEventListener('change', updateDetector);
    updateTracker();
    updateDetector();
    markChanged();
    setSample(sample.value, false);
    if (el('form-errors')) {
        form.querySelector('.invalid')?.scrollIntoView({block: 'center'});
    }
})();
