/**
 * Уніфікована конфігурація TinyMCE 7 (self-hosted)
 * Використовується для всіх WYSIWYG редакторів у проекті RFA
 */

/**
 * Базова конфігурація TinyMCE
 * @param {string} selector - CSS селектор textarea (за замовчуванням 'textarea')
 * @param {Object} customOptions - Додаткові опції для перевизначення
 */
function initTinyMCE(selector, customOptions) {
    if (typeof selector === 'object' && selector !== null) {
        customOptions = selector;
        selector = 'textarea';
    }

    if (!selector) {
        selector = 'textarea';
    }

    var defaultOptions = {
        base_url: '/js/tinymce',
        language: 'uk_UA',
        icons: 'default',
        plugins: [
            'advlist', 'autolink', 'lists', 'link', 'image', 'charmap', 'preview',
            'anchor', 'searchreplace', 'visualblocks', 'code', 'fullscreen',
            'insertdatetime', 'media', 'table', 'wordcount', 'emoticons', 'help'
        ],
        toolbar: 'undo redo | blocks | bold italic forecolor | ' +
            'alignleft aligncenter alignright alignjustify | ' +
            'bullist numlist | link image | removeformat code help',
        height: 400,
        min_height: 300,
        content_style: 'body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px; line-height: 1.6; }',
        image_caption: true,
        link_target_list: [
            { title: 'В тому ж вікні', value: '' },
            { title: 'В новому вікні', value: '_blank' }
        ]
    };

    var finalOptions = Object.assign({}, defaultOptions, customOptions || {});

    console.log('[TinyMCE] Ініціалізація:', selector, finalOptions);
    console.log('[TinyMCE] tinymce:', typeof tinymce !== 'undefined' ? 'OK' : 'НЕМАЄ');

    if (typeof tinymce === 'undefined') {
        console.error('[TinyMCE] Бібліотеку tinymce не завантажено!');
        return;
    }

    // Видаляємо попередній екземпляр якщо є
    var existing = tinymce.get(selector.replace('#', ''));
    if (existing) {
        console.log('[TinyMCE] Видаляємо попередній екземпляр');
        existing.remove();
    }

    tinymce.init(finalOptions).then(function(editors) {
        console.log('[TinyMCE] Успішно ініціалізовано:', editors.length, 'редакторів');
    }).catch(function(err) {
        console.error('[TinyMCE] ПОМИЛКА ініціалізації:', err);
    });
}

/**
 * Швидка ініціалізація з мінімальним набором плагінів
 */
function initTinyMCESimple(selector, customOptions) {
    if (typeof selector === 'object' && selector !== null) {
        customOptions = selector;
        selector = 'textarea';
    }

    if (!selector) {
        selector = 'textarea';
    }

    initTinyMCE(selector, Object.assign({}, {
        plugins: [
            'autolink', 'lists', 'link', 'image', 'charmap', 'code',
            'insertdatetime', 'media', 'table', 'wordcount', 'help'
        ],
        toolbar: 'undo redo | blocks | bold italic | ' +
            'alignleft aligncenter alignright alignjustify | ' +
            'bullist numlist | link image media table | removeformat code help',
        menubar: false,
        height: 250,
        min_height: 200
    }, customOptions || {}));
}

/**
 * Повна ініціалізація для постів/статей (максимум плагінів)
 */
function initTinyMCEFull(selector, customOptions) {
    if (typeof selector === 'object' && selector !== null) {
        customOptions = selector;
        selector = 'textarea';
    }

    if (!selector) {
        selector = 'textarea';
    }

    initTinyMCE(selector, Object.assign({}, {
        plugins: [
            'preview', 'searchreplace', 'autolink', 'autosave', 'save',
            'directionality', 'code', 'visualblocks', 'visualchars', 'fullscreen',
            'image', 'link', 'media', 'table', 'charmap',
            'pagebreak', 'nonbreaking', 'anchor', 'insertdatetime', 'advlist',
            'lists', 'wordcount', 'help', 'emoticons',
            'accordion', 'codesample'
        ],
        toolbar: 'undo redo | blocks | ' +
            'bold italic underline strikethrough forecolor backcolor | ' +
            'alignleft aligncenter alignright alignjustify | ' +
            'bullist numlist outdent indent | ' +
            'link image media codesample table | ' +
            'charmap emoticons pagebreak | removeformat code fullscreen help',
        menubar: 'file edit view insert format tools table help',
        height: 450,
        min_height: 350,
        max_height: 700
    }, customOptions || {}));
}

/**
 * Ініціалізація з автоматичним збереженням
 */
function initTinyMCEAutoSave(selector, saveCallback) {
    var callback = saveCallback || function () {};

    initTinyMCE(selector, {
        autosave_interval: '30s',
        autosave_prefix: 'tinymce-autosave-{path}{query}-{id}-',
        autosave_restorewhenempty: true,
        autosave_retention: '1440m',
        setup: function (editor) {
            editor.on('StoreDraft', function () {
                console.log('Чернетку збережено автоматично');
            });
            editor.on('change', function () {
                callback(editor.getContent());
            });
        }
    });
}

/**
 * Знищити всі екземпляри TinyMCE
 */
function destroyTinyMCE() {
    if (typeof tinymce !== 'undefined' && tinymce.editors) {
        tinymce.editors.forEach(function(editor) {
            editor.remove();
        });
    }
}

/**
 * Отримати вміст редактора
 */
function getTinyMCEContent(editorId) {
    if (typeof tinymce === 'undefined') return null;
    var editor = tinymce.get(editorId.replace('#', ''));
    return editor ? editor.getContent() : null;
}

/**
 * Встановити вміст редактора
 */
function setTinyMCEContent(editorId, content) {
    if (typeof tinymce === 'undefined') return;
    var editor = tinymce.get(editorId.replace('#', ''));
    if (editor) {
        editor.setContent(content);
    }
}
