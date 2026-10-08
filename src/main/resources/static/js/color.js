// Цветовые преобразования, совпадающие с java.awt.Color.RGBtoHSB/HSBtoRGB
// и логикой DefaultColorModifier и DefaultColorLimit на сервере.
const ColorMath = (() => {
    function hexToRgb(hex) {
        const value = hex.replace('#', '');
        return {
            r: parseInt(value.slice(0, 2), 16),
            g: parseInt(value.slice(2, 4), 16),
            b: parseInt(value.slice(4, 6), 16)
        };
    }

    function rgbToHex({r, g, b}) {
        return '#' + [r, g, b].map(v => Math.round(v).toString(16).padStart(2, '0')).join('');
    }

    // Тон 0..360, насыщенность и яркость 0..100.
    function rgbToHsb({r, g, b}) {
        const max = Math.max(r, g, b);
        const min = Math.min(r, g, b);
        const brightness = max / 255;
        const saturation = max === 0 ? 0 : (max - min) / max;
        let hue = 0;
        if (saturation !== 0) {
            const rc = (max - r) / (max - min);
            const gc = (max - g) / (max - min);
            const bc = (max - b) / (max - min);
            if (r === max) {
                hue = bc - gc;
            } else if (g === max) {
                hue = 2 + rc - bc;
            } else {
                hue = 4 + gc - rc;
            }
            hue /= 6;
            if (hue < 0) {
                hue += 1;
            }
        }
        return {h: hue * 360, s: saturation * 100, b: brightness * 100};
    }

    function hsbToRgb({h, s, b}) {
        const sat = s / 100;
        const bri = b / 100;
        if (sat === 0) {
            const v = bri * 255;
            return {r: v, g: v, b: v};
        }
        const hh = ((h % 360) + 360) % 360 / 60;
        const i = Math.floor(hh);
        const f = hh - i;
        const p = bri * (1 - sat);
        const q = bri * (1 - sat * f);
        const t = bri * (1 - sat * (1 - f));
        const [r, g, bl] = [
            [bri, t, p], [q, bri, p], [p, bri, t], [p, q, bri], [t, p, bri], [bri, p, q]
        ][i];
        return {r: r * 255, g: g * 255, b: bl * 255};
    }

    const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

    // Как DefaultColorModifier: усиление насыщенности, затем сдвиг.
    function modify(hsb, shift) {
        let hue = (hsb.h + shift.h) % 360;
        if (hue < 0) {
            hue += 360;
        }
        const boosted = hsb.s + (100 - hsb.s) * (shift.boost || 0) / 100;
        return {h: hue, s: clamp(boosted + shift.s, 0, 100), b: clamp(hsb.b + shift.b, 0, 100)};
    }

    function limit(hsb, range) {
        return {
            h: clamp(hsb.h, range.minHue, range.maxHue),
            s: clamp(hsb.s, range.minSaturation, range.maxSaturation),
            b: clamp(hsb.b, range.minBrightness, range.maxBrightness)
        };
    }

    // Как ColorZones на сервере: черный, серый или цветной.
    function zoneOf(hsb, zones) {
        if (hsb.b < zones.black) {
            return 'black';
        }
        if (hsb.s < zones.gray) {
            return 'gray';
        }
        return 'color';
    }

    // ---- Расчет цвета кадра, как детекторы на сервере. Точки: [{r, g, b}] ----
    const pkg = 'ru.mkilord.colortomqttapp.core.';
    const DOMINANT = pkg + 'detector.DominantColorDetector';
    const VIVID = pkg + 'detector.VividColorDetector';
    const GRID = pkg + 'processor.GridProcessor';

    // Точки обхода, как ChessProcessor и GridProcessor.
    function samplePoints(processor, width, height, cell) {
        const points = [];
        for (let y = 0; y < height; y += cell) {
            const chess = processor !== GRID;
            const startX = chess ? ((y / cell) % 2 === 0 ? cell : 0) : 0;
            for (let x = startX; x < width; x += chess ? cell * 2 : cell) {
                points.push([x, y]);
            }
        }
        return points;
    }

    function average(pixels) {
        const sum = pixels.reduce((a, p) => ({r: a.r + p.r, g: a.g + p.g, b: a.b + p.b}), {r: 0, g: 0, b: 0});
        const n = pixels.length;
        return {r: Math.floor(sum.r / n), g: Math.floor(sum.g / n), b: Math.floor(sum.b / n)};
    }

    function weighted(pixels, weights) {
        let r = 0, g = 0, b = 0, total = 0;
        pixels.forEach((p, i) => {
            r += p.r * weights[i];
            g += p.g * weights[i];
            b += p.b * weights[i];
            total += weights[i];
        });
        return {color: {r: Math.floor(r / total), g: Math.floor(g / total), b: Math.floor(b / total)}, total};
    }

    // Возвращает цвет и номера точек, которые в него вошли.
    function detect(detector, pixels, minSharePercent) {
        const all = pixels.map((_, i) => i);
        if (detector === VIVID) {
            const weights = pixels.map(p => {
                const hsb = rgbToHsb(p);
                return hsb.s / 100 * hsb.b / 100;
            });
            const result = weighted(pixels, weights);
            if (result.total < 0.01 * pixels.length) {
                return {color: average(pixels), used: all, fallback: true};
            }
            return {color: result.color, used: all.filter(i => weights[i] > 0.05)};
        }
        if (detector === DOMINANT) {
            const BINS = 24;
            const bins = [];
            const weights = [];
            const binWeight = new Array(BINS).fill(0);
            pixels.forEach((p, i) => {
                const hsb = rgbToHsb(p);
                if (hsb.s < 20 || hsb.b < 15) {
                    bins[i] = -1;
                    return;
                }
                bins[i] = Math.min(Math.floor(hsb.h / 360 * BINS), BINS - 1);
                weights[i] = hsb.s / 100 * hsb.b / 100;
                binWeight[bins[i]] += weights[i];
            });
            let best = -1, bestWeight = 0;
            for (let bin = 0; bin < BINS; bin++) {
                const group = binWeight[bin] + binWeight[(bin + 1) % BINS] + binWeight[(bin + BINS - 1) % BINS];
                if (group > bestWeight) {
                    bestWeight = group;
                    best = bin;
                }
            }
            const near = bin => {
                const d = Math.abs(bin - best);
                return Math.min(d, BINS - d) <= 1;
            };
            const used = best < 0 ? [] : all.filter(i => bins[i] >= 0 && near(bins[i]));
            if (!used.length || used.length < minSharePercent / 100 * pixels.length) {
                return {color: average(pixels), used: all, fallback: true};
            }
            const result = weighted(used.map(i => pixels[i]), used.map(i => weights[i]));
            return {color: result.color, used};
        }
        return {color: average(pixels), used: all};
    }

    // Как MQTTColorPublisher.payload: округление до целых.
    function payload(hsb) {
        return `{"hue":${Math.round(hsb.h)},"sat":${Math.round(hsb.s)},"brightness":${Math.round(hsb.b)}}`;
    }

    return {hexToRgb, rgbToHex, rgbToHsb, hsbToRgb, modify, limit, zoneOf, samplePoints, detect, payload, clamp};
})();
