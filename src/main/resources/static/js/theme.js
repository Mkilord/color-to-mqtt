// Подключается в <head>, чтобы тема применилась до отрисовки страницы и не мигала.
(function () {
    var saved = null;
    try {
        saved = localStorage.getItem('theme');
    } catch (e) {
        // localStorage может быть недоступен
    }
    var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.dataset.theme = saved || (prefersDark ? 'dark' : 'light');
})();
