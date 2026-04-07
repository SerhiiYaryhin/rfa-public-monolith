/**
 * RFA — Динамічне визначення висоти navbar
 * Вимірює фактичну висоту navbar та оновлює CSS custom property
 * Запускається при завантаженні та при зміні розміру вікна
 */
(function() {
    'use strict';

    function updateNavbarHeight() {
        var navbar = document.querySelector('.fixed-top, .navbar.fixed-top, #mainNav');
        if (!navbar) return;

        var height = navbar.offsetHeight;
        // Додаємо невеликий буфер (2px) для надійності
        var heightWithBuffer = height + 2;

        document.documentElement.style.setProperty('--navbar-height', heightWithBuffer + 'px');

        // Responsive fallback values
        if (window.innerWidth < 768) {
            document.documentElement.style.setProperty('--navbar-height-mobile', heightWithBuffer + 'px');
        } else if (window.innerWidth < 992) {
            document.documentElement.style.setProperty('--navbar-height-tablet', heightWithBuffer + 'px');
        }

        console.log('[Navbar] Висота:', heightWithBuffer + 'px, ширина вікна:', window.innerWidth + 'px');
    }

    // Запускаємо при завантаженні DOM
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', updateNavbarHeight);
    } else {
        // DOM вже завантажений
        updateNavbarHeight();
    }

    // Запускаємо при зміні розміру вікна (debounce 100ms)
    var resizeTimer;
    window.addEventListener('resize', function() {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(updateNavbarHeight, 100);
    });

})();
