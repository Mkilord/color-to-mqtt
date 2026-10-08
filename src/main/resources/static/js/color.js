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

    // Как MQTTColorPublisher.payload: округление до целых.
    function payload(hsb) {
        return `{"hue":${Math.round(hsb.h)},"sat":${Math.round(hsb.s)},"brightness":${Math.round(hsb.b)}}`;
    }

    return {hexToRgb, rgbToHex, rgbToHsb, hsbToRgb, modify, limit, zoneOf, payload, clamp};
})();
