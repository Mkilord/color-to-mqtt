// Страница настроек: проверка цвета, сообщение MQTT, карта области и снимок.
(() => {
    const el = id => document.getElementById(id);
    const form = el('settings-form');
    const num = name => {
        const value = parseFloat(form.elements[name].value);
        return Number.isFinite(value) ? value : 0;
    };

    // Java отдает дробные значения как "2.0", в поле удобнее видеть "2".
    form.querySelectorAll('input[type="number"]').forEach(input => {
        const value = Number(input.value);
        if (input.value !== '' && Number.isFinite(value)) {
            input.value = String(value);
        }
    });

    // ---- Способ сравнения ----
    const TRACKER_HELP = {
        'ru.mkilord.colortomqttapp.core.tracker.ToleranceColorStateTracker':
            'Цвет отправляется, если тон, насыщенность или яркость изменились больше допуска. Подходит для большинства случаев.',
        'ru.mkilord.colortomqttapp.core.tracker.DefaultColorStateTracker':
            'Цвет отправляется, если расстояние между цветами в RGB больше порога.',
        'ru.mkilord.colortomqttapp.core.tracker.SimpleColorStateTracker':
            'Цвет отправляется при любом изменении. Сообщений будет много.'
    };
    const tracker = el('tracker');

    const DETECTOR_HELP = {
        'ru.mkilord.colortomqttapp.core.detector.DominantColorDetector':
            'Берется цвет, которого больше всего среди цветных точек. Темный фон и серые элементы не учитываются: красный объект на темном фоне даст красный, а не грязно-серый.',
        'ru.mkilord.colortomqttapp.core.detector.VividColorDetector':
            'Среднее, где яркие насыщенные точки весят больше темных и серых. Несколько цветов смешиваются.',
        'ru.mkilord.colortomqttapp.core.detector.AverageColorDetector':
            'Среднее по всем точкам. Темный фон и серые элементы делают цвет бледнее.'
    };
    const detector = el('detector');

    function updateDetector() {
        el('detector-help').textContent = DETECTOR_HELP[detector.value] || '';
        form.querySelectorAll('[data-for-detector]').forEach(field => {
            field.hidden = field.dataset.forDetector !== detector.value;
        });
    }

    function updateTracker() {
        el('tracker-help').textContent = TRACKER_HELP[tracker.value] || '';
        form.querySelectorAll('[data-for]').forEach(field => {
            const visible = field.dataset.for === tracker.value;
            field.hidden = !visible;
        });
    }

    // ---- Проверка цвета ----
    const sample = el('sample-color');

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

    function rgbCss(hsb) {
        return ColorMath.rgbToHex(ColorMath.hsbToRgb(hsb));
    }

    function updatePreview() {
        const source = ColorMath.rgbToHsb(ColorMath.hexToRgb(sample.value));
        const range = {
            minHue: num('minHue'), maxHue: num('maxHue'),
            minSaturation: num('minSaturation'), maxSaturation: num('maxSaturation'),
            minBrightness: num('minBrightness'), maxBrightness: num('maxBrightness')
        };
        const zone = ColorMath.zoneOf(source, {black: num('blackThreshold'), gray: num('grayThreshold')});
        let modified = ColorMath.modify(source, {h: num('modifyHue'), s: num('modifySaturation'), b: num('modifyBrightness'), boost: num('saturationBoost'),
            hueShifts: ['hueShiftRed', 'hueShiftYellow', 'hueShiftGreen', 'hueShiftCyan', 'hueShiftBlue', 'hueShiftMagenta'].map(num)});
        let sent = ColorMath.limit(modified, range);
        const note = el('zone-note');
        if (zone === 'black') {
            modified = {h: 0, s: 0, b: 0};
            sent = {h: 0, s: 0, b: 0};
            note.textContent = 'Темнее порога черного: уйдет яркость 0, коррекция и ограничения не применяются.';
        } else if (zone === 'gray') {
            modified = {h: 0, s: 1, b: modified.b};
            sent = {h: 0, s: 1, b: sent.b};
            note.textContent = 'Насыщенность ниже порога серого: уйдет белый в цветном режиме, корректируется только яркость.';
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

    let lastSent = null;

    document.querySelectorAll('[data-sample]').forEach(button => {
        button.addEventListener('click', () => {
            sample.value = button.dataset.sample;
            updatePreview();
        });
    });

    el('send-test').addEventListener('click', async () => {
        if (!lastSent) {
            return;
        }
        const button = el('send-test');
        button.disabled = true;
        try {
            const response = await fetch('/api/test-color', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    hue: Math.round(lastSent.h),
                    sat: Math.round(lastSent.s),
                    brightness: Math.round(lastSent.b)
                })
            });
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.error || 'HTTP ' + response.status);
            }
            showToast('Отправлено: ' + result.payload);
        } catch (e) {
            showToast(e.message, 'error');
        } finally {
            button.disabled = false;
        }
    });

    el('sample-current').addEventListener('click', async () => {
        try {
            const status = await (await fetch('/api/status', {cache: 'no-store'})).json();
            if (!status.running) {
                showToast('Захват не запущен, текущего цвета нет. Возьмите цвет со снимка.', 'error');
                return;
            }
            sample.value = status.color;
            updatePreview();
        } catch (e) {
            showToast('Не удалось получить текущий цвет', 'error');
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
            caption.textContent = `Область ${width} × ${height} px по центру экрана`;
            return;
        }
        const w = Math.min(width, screenSize.width);
        const h = Math.min(height, screenSize.height);
        area.style.width = `${w / screenSize.width * 100}%`;
        area.style.height = `${h / screenSize.height * 100}%`;
        const clipped = w < width || h < height ? '. Больше экрана, будет обрезана' : '';
        caption.textContent = `Область ${w} × ${h} из ${screenSize.width} × ${screenSize.height} px${clipped}`;
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
        const average = ColorMath.rgbToHex(result.color);
        const usedSet = new Set(result.used);

        if (el('snapshot-grid').checked) {
            const radius = Math.max(1.5, image.naturalWidth / 250);
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
        el('snapshot-average').hidden = false;
        el('snapshot-average-chip').style.background = average;
        el('snapshot-average-text').textContent = result.fallback
            ? `Цвет ${average}: преобладающего цвета нет, взято среднее по ${points.length} точкам`
            : `Цвет ${average}, учтено точек: ${result.used.length} из ${points.length}`;
        sample.value = average;
        updatePreview();
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
            const response = await fetch('/settings/preview_image?' + params, {cache: 'no-store'});
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
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

    // ---- Сброс и несохраненные изменения ----
    document.querySelectorAll('[data-confirm]').forEach(button => {
        button.addEventListener('click', event => {
            if (!confirm(button.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    let dirty = false;
    let submitting = false;
    form.addEventListener('input', event => {
        dirty = true;
        el('dirty-note').hidden = false;
        if (event.target.name === 'screenWidth' || event.target.name === 'screenHeight') {
            updateScreenMap();
        }
        if (['cellSize', 'detector', 'processor', 'dominantMinShare'].includes(event.target.name)) {
            drawSnapshot();
        }
        updatePreview();
    });
    form.addEventListener('submit', () => submitting = true);
    window.addEventListener('beforeunload', event => {
        if (dirty && !submitting) {
            event.preventDefault();
            event.returnValue = '';
        }
    });

    tracker.addEventListener('change', updateTracker);
    detector.addEventListener('change', () => {
        updateDetector();
        drawSnapshot();
    });
    el('processor').addEventListener('change', drawSnapshot);
    sample.addEventListener('input', updatePreview);
    updateTracker();
    updateDetector();
    updatePreview();
})();
