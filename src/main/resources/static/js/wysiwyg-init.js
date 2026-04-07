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
        // Якщо перший аргумент — об'єкт опцій, використаємо селектор за замовчуванням
        customOptions = selector;
        selector = 'textarea';
    }

    if (!selector) {
        selector = 'textarea';
    }

    var defaultOptions = {
        // Шлях до self-hosted TinyMCE
        base_url: '/js/tinymce',
        
        // Мова інтерфейсу
        language: 'uk',
        
        // Плагіни
        plugins: [
            'advlist', 'autolink', 'lists', 'link', 'image', 'charmap', 'preview',
            'anchor', 'searchreplace', 'visualblocks', 'code', 'fullscreen',
            'insertdatetime', 'media', 'table', 'wordcount', 'emoticons', 'help'
        ],
        
        // Панель інструментів
        toolbar: 'undo redo | blocks fontfamily fontsize | ' +
            'bold italic underline strikethrough forecolor backcolor | ' +
            'alignleft aligncenter alignright alignjustify | ' +
            'bullist numlist outdent indent | link image media table emoticons | ' +
            'removeformat code fullscreen help',
        
        // Меню
        menubar: 'file edit view insert format tools table',
        
        // Висота редактора
        min_height: 300,
        max_height: 600,
        
        // Автоматичне змінення висоти
        autoresize_on_init: true,
        
        // Форматування
        content_style: 'body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px; line-height: 1.6; }',
        
        // Валідація
        valid_elements: '*[*]',
        extended_valid_elements: 'iframe[src|width|height|frameborder|allow|allowfullscreen|class]',
        
        // Зображення
        image_advtab: true,
        image_caption: true,
        
        // Посилання
        link_target_list: [
            { title: 'В тому ж вікні', value: '' },
            { title: 'В новому вікні', value: '_blank' }
        ],
        
        // Таблиці
        table_default_styles: {
            width: '100%',
            border-collapse: 'collapse'
        },
        
        // Швидкі вставки
        quickbars_selection_toolbar: 'bold italic | quicklink h2 h3 blockquote quickimage quicktable',
        quickbars_insert_toolbar: 'quickimage media table',
        
        // Code sample
        codesample_languages: [
            { text: 'HTML/XML', value: 'markup' },
            { text: 'JavaScript', value: 'javascript' },
            { text: 'CSS', value: 'css' },
            { text: 'Java', value: 'java' },
            { text: 'Python', value: 'python' },
            { text: 'SQL', value: 'sql' },
            { text: 'Bash', value: 'bash' }
        ],
        
        // Без AI/коментарів (відключено)
        ai_request: false,
        tinycomments_mode: 'embedded',
        
        // Збереження при втраті фокусу
        save_enablewhendirty: true,
        
        // Стилі блоків
        style_formats: [
            { title: 'Заголовки', items: [
                { title: 'Заголовок 1', format: 'h1' },
                { title: 'Заголовок 2', format: 'h2' },
                { title: 'Заголовок 3', format: 'h3' },
                { title: 'Заголовок 4', format: 'h4' }
            ]},
            { title: 'Інлайн', items: [
                { title: 'Жирний', icon: 'bold', format: 'bold' },
                { title: 'Курсив', icon: 'italic', format: 'italic' },
                { title: 'Підкреслений', icon: 'underline', format: 'underline' },
                { title: 'Закреслений', icon: 'strikethrough', format: 'strikethrough' },
                { title: 'Верхній індекс', icon: 'superscript', format: 'superscript' },
                { title: 'Нижній індекс', icon: 'subscript', format: 'subscript' }
            ]},
            { title: 'Блоки', items: [
                { title: 'Параграф', format: 'p' },
                { title: 'Цитата', format: 'blockquote' },
                { title: 'Код', format: 'code' },
                { title: 'Преформатований', format: 'pre' }
            ]}
        ],
        
        // Template (заготовки)
        templates: [
            { title: 'Базова стаття', description: 'Стаття з заголовком та абзацами', content: '<h2>Заголовок розділу</h2><p>Текст абзацу...</p>' },
            { title: 'Таблиця', description: 'Таблиця 3x3', content: '<table><tbody><tr><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td></tr></tbody></table>' }
        ]
    };

    // Об'єднуємо опції
    var finalOptions = Object.assign({}, defaultOptions, customOptions || {});
    
    // Ініціалізуємо TinyMCE
    tinymce.init(finalOptions);
}

/**
 * Швидка ініціалізація з мінімальним набором плагінів
 * Використовується для простих форм (подкасти, банери тощо)
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
        min_height: 200,
        max_height: 400
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
            'preview', 'importcss', 'searchreplace', 'autolink', 'autosave', 'save',
            'directionality', 'code', 'visualblocks', 'visualchars', 'fullscreen',
            'image', 'link', 'media', 'template', 'codesample', 'table', 'charmap',
            'pagebreak', 'nonbreaking', 'anchor', 'insertdatetime', 'advlist',
            'lists', 'wordcount', 'help', 'charmap', 'quickbars', 'emoticons',
            'accordion', 'footnotes'
        ],
        toolbar: 'undo redo | blocks fontfamily fontsize | ' +
            'bold italic underline strikethrough forecolor backcolor | ' +
            'alignleft aligncenter alignright alignjustify | ' +
            'bullist numlist outdent indent | ' +
            'link image media template codesample table | ' +
            'charmap emoticons pagebreak | removeformat code fullscreen help',
        menubar: 'file edit view insert format tools table help',
        min_height: 400,
        max_height: 700
    }, customOptions || {}));
}

/**
 * Ініціалізація з автоматичним збереженням
 * Використовується для довгих форм
 */
function initTinyMCEAutoSave(selector, saveCallback) {
    var callback = saveCallback || function () {};
    
    initTinyMCE(selector, {
        autosave_interval: '30s',
        autosave_prefix: 'tinymce-autosave-{path}{query}-{id}-',
        autosave_restorewhenempty: true,
        autosave_retention: '1440m', // 24 години
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
    if (tinymce && tinymce.activeEditor) {
        tinymce.remove();
    }
}

/**
 * Отримати вміст редактора
 */
function getTinyMCEContent(editorId) {
    var editor = tinymce.get(editorId);
    return editor ? editor.getContent() : null;
}

/**
 * Встановити вміст редактора
 */
function setTinyMCEContent(editorId, content) {
    var editor = tinymce.get(editorId);
    if (editor) {
        editor.setContent(content);
    }
}
