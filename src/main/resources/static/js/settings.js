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
        let modified = ColorMath.modify(source, {h: num('modifyHue'), s: num('modifySaturation'), b: num('modifyBrightness')});
        let sent = ColorMath.limit(modified, range);
        const note = el('zone-note');
        const whiteKelvin = num('whiteKelvin');
        let kelvin = null;
        if (zone === 'black') {
            modified = {h: 0, s: 0, b: 0};
            sent = {h: 0, s: 0, b: 0};
            note.textContent = 'Темнее порога черного: уйдет яркость 0, коррекция и ограничения не применяются.';
        } else if (zone === 'gray') {
            modified = {h: 0, s: 0, b: modified.b};
            sent = {h: 0, s: 0, b: sent.b};
            kelvin = whiteKelvin > 0 ? whiteKelvin : null;
            note.textContent = kelvin
                ? `Насыщенность ниже порога серого: уйдет белый ${kelvin} K, корректируется только яркость.`
                : 'Насыщенность ниже порога серого: уйдет белый свет, корректируется только яркость.';
        }
        note.hidden = zone === 'color';

        el('swatch-source').style.background = sample.value;
        el('swatch-modified').style.background = rgbCss(modified);
        if (kelvin) {
            const white = ColorMath.kelvinToRgb(kelvin);
            const k = sent.b / 100;
            el('swatch-sent').style.background = ColorMath.rgbToHex({r: white.r * k, g: white.g * k, b: white.b * k});
        } else {
            el('swatch-sent').style.background = rgbCss(sent);
        }

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
        el('message-payload').textContent = ColorMath.payload(sent, kelvin);
    }

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

    // Те же точки, что обходит ChessProcessor на сервере.
    function chessPoints(width, height, cell) {
        const points = [];
        for (let y = 0; y < height; y += cell) {
            const startX = (y / cell) % 2 === 0 ? cell : 0;
            for (let x = startX; x < width; x += cell * 2) {
                points.push([x, y]);
            }
        }
        return points;
    }

    function drawSnapshot() {
        if (!lastImage) {
            return;
        }
        const image = lastImage;
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(image, 0, 0);

        const cell = Math.max(1, Math.round(num('cellSize')));
        const points = chessPoints(image.naturalWidth, image.naturalHeight, cell);
        const data = ctx.getImageData(0, 0, image.naturalWidth, image.naturalHeight).data;
        let r = 0, g = 0, b = 0;
        points.forEach(([x, y]) => {
            const i = (y * image.naturalWidth + x) * 4;
            r += data[i];
            g += data[i + 1];
            b += data[i + 2];
        });
        let average;
        if (points.length) {
            average = ColorMath.rgbToHex({r: Math.floor(r / points.length), g: Math.floor(g / points.length), b: Math.floor(b / points.length)});
        } else {
            const i = (Math.floor(image.naturalHeight / 2) * image.naturalWidth + Math.floor(image.naturalWidth / 2)) * 4;
            average = ColorMath.rgbToHex({r: data[i], g: data[i + 1], b: data[i + 2]});
        }

        if (el('snapshot-grid').checked) {
            const radius = Math.max(1.5, image.naturalWidth / 250);
            points.forEach(([x, y]) => {
                ctx.beginPath();
                ctx.arc(x + 0.5, y + 0.5, radius, 0, Math.PI * 2);
                ctx.fillStyle = 'rgba(255,255,255,0.9)';
                ctx.fill();
                ctx.lineWidth = 1;
                ctx.strokeStyle = 'rgba(0,0,0,0.7)';
                ctx.stroke();
            });
        }

        el('snapshot-empty').hidden = true;
        el('snapshot-average').hidden = false;
        el('snapshot-average-chip').style.background = average;
        el('snapshot-average-text').textContent = `Средний цвет ${average}, точек: ${points.length || 1}`;
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
        if (event.target.name === 'cellSize') {
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
    sample.addEventListener('input', updatePreview);
    updateTracker();
    updatePreview();
})();
