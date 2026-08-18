/**
 * ============================================================================
 * ФАЙЛ: static/js/rfa-editorjs-edit.js
 * ОПИС: Клас для ініціалізації Editor.js у режимі РЕДАГУВАННЯ та синхронізації з формою.
 * ============================================================================
 */

class RFAEditorJSEdit {
    constructor(uniqueId, fieldName) {
        this.uniqueId = uniqueId;
        this.fieldName = fieldName;
        this.containerId = 'rfa-editorjs-edit-' + uniqueId;
        this.dataId = 'rfa-editorjs-edit-data-' + uniqueId;
        this.initialDataId = 'rfa-editorjs-edit-initial-' + uniqueId;

        this.container = document.getElementById(this.containerId);
        this.textarea = document.getElementById(this.dataId);
        this.initialDataElement = document.getElementById(this.initialDataId);
        this.editor = null;
        this.form = null;

        if (!this.container || !this.textarea) {
            console.warn(`[RFAEditorJSEdit] Елементи для ${this.uniqueId} не знайдено.`);
            return;
        }

        this.form = this.textarea.closest('form');
        this.init();
    }

    async init() {
        if (typeof window.EditorJS === 'undefined') {
            console.error('[RFAEditorJSEdit] Editor.js не завантажено. Перевірте CDN-скрипти.');
            return;
        }

        // Отримуємо початкові дані з script елемента (type="text/plain" + th:utext)
        let initialData = {};
        if (this.initialDataElement) {
            const rawData = this.initialDataElement.textContent.trim();

            // ДІАГНОСТИКА: виводимо перші 200 символів, щоб побачити, що саме приходить
            console.log('[RFAEditorJSEdit] Отримані початкові дані (перші 200 символів):', rawData.substring(0, 200));
            console.log('[RFAEditorJSEdit] Перший символ:', rawData.charAt(0), 'Код:', rawData.charCodeAt(0));

            if (rawData && rawData !== '{}' && rawData.startsWith('{')) {
                try {
                    initialData = JSON.parse(rawData);
                    console.log('[RFAEditorJSEdit] Успішно розпарсено JSON');
                } catch (e) {
                    console.error('[RFAEditorJSEdit] Помилка парсингу JSON:', e);
                    console.error('[RFAEditorJSEdit] Повні дані:', rawData);
                }
            } else {
                console.warn('[RFAEditorJSEdit] Дані не починаються з "{", пропускаємо парсинг');
            }
        } else {
            console.log('[RFAEditorJSEdit] Початкові дані відсутні (новий альбом)');
        }

        // Формуємо конфігурацію інструментів
        const toolsConfig = {};
        if (typeof window.Header !== 'undefined') toolsConfig.header = window.Header;
        if (typeof window.Paragraph !== 'undefined') toolsConfig.paragraph = window.Paragraph;
        if (typeof window.List !== 'undefined') toolsConfig.list = window.List;
        if (typeof window.Quote !== 'undefined') toolsConfig.quote = window.Quote;
        if (typeof window.Delimiter !== 'undefined') toolsConfig.delimiter = window.Delimiter;
        if (typeof window.ImageTool !== 'undefined') toolsConfig.image = window.ImageTool;

        // Ініціалізація Editor.js
        this.editor = new window.EditorJS({
            holder: this.containerId,
            readOnly: false,
            tools: toolsConfig,
            data: initialData,
            placeholder: 'Опишіть ваш альбом...',
            onReady: () => {
                console.log(`[RFAEditorJSEdit] Успішно ініціалізовано: ${this.uniqueId}`);
            }
        });

        // Перехоплення відправки форми для збереження даних
        if (this.form) {
            this.form.addEventListener('submit', (event) => this.syncBeforeSubmit(event));
        }
    }

    async syncBeforeSubmit(event) {
        if (!this.editor) return;

        event.preventDefault();

        try {
            const outputData = await this.editor.save();
            this.textarea.value = JSON.stringify(outputData);
            this.form.submit();
        } catch (error) {
            console.error('[RFAEditorJSEdit] Не вдалося зберегти дані редактора:', error);
            alert('Сталася помилка при збереженні опису. Спробуйте ще раз.');
        }
    }
}

// Автоматична ініціалізація
document.addEventListener('DOMContentLoaded', () => {
    const containers = document.querySelectorAll('.rfa-editorjs-edit-container');

    containers.forEach(container => {
        const containerId = container.id;
        if (!containerId || !containerId.startsWith('rfa-editorjs-edit-')) return;

        const uniqueId = containerId.replace('rfa-editorjs-edit-', '');
        const textarea = document.getElementById('rfa-editorjs-edit-data-' + uniqueId);

        if (textarea) {
            const fieldName = textarea.getAttribute('name');
            new RFAEditorJSEdit(uniqueId, fieldName);
        }
    });
});