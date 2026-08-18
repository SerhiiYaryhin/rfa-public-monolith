/**
 * ============================================================================
 * ФАЙЛ: static/js/rfa-editorjs.js
 * ОПИС: Клас для ініціалізації Editor.js у режимі read-only.
 * ============================================================================
 *
 * ВИКОРИСТАННЯ:
 *
 * Спосіб 1 (Автоматичний):
 *   Просто підключіть цей скрипт на сторінці. Він автоматично знайде всі
 *   елементи з класом .rfa-editorjs-content та ініціалізує їх.
 *
 * Спосіб 2 (Ручний):
 *   const editor = new RFAEditorJS('my-unique-id');
 *
 * ============================================================================
 */

class RFAEditorJS {
    /**
     * @param {string} uniqueId - Унікальний ідентифікатор екземпляра.
     *                            Має відповідати ID, переданому у Thymeleaf фрагмент.
     */
    constructor(uniqueId) {
        this.uniqueId = uniqueId;
        this.containerId = 'rfa-editorjs-' + uniqueId;
        this.dataId = 'rfa-editorjs-data-' + uniqueId;

        this.container = document.getElementById(this.containerId);
        this.dataElement = document.getElementById(this.dataId);

        if (!this.container) {
            console.warn(`[RFAEditorJS] Контейнер #${this.containerId} не знайдено.`);
            return;
        }

        this.init();
    }

    init() {
        // Якщо немає даних — нічого не робимо
        if (!this.dataElement) {
            this.container.innerHTML = '<p class="text-muted fst-italic">Опис відсутній.</p>';
            return;
        }

        const rawData = this.dataElement.textContent.trim();

        // Перевірка: чи схоже це на JSON?
        if (!rawData || rawData === '{}' || !rawData.startsWith('{')) {
            // Це звичайний текст — відображаємо як абзац
            this.container.innerHTML = `<p style="white-space: pre-wrap;">${this.escapeHtml(rawData)}</p>`;
            return;
        }

        try {
            const parsedData = JSON.parse(rawData);

            // Перевірка структури Editor.js
            if (parsedData.blocks && Array.isArray(parsedData.blocks)) {
                // Перевіряємо, чи завантажилося ядро Editor.js
                if (typeof EditorJS === 'undefined') {
                    console.error('[RFAEditorJS] Editor.js не завантажено. Підключіть CDN-скрипти.');
                    this.container.innerHTML = `<p style="white-space: pre-wrap;">${this.escapeHtml(rawData)}</p>`;
                    return;
                }

                // Ініціалізуємо Editor.js у режимі read-only
                new EditorJS({
                    holder: this.containerId,
                    readOnly: true,
                    tools: {
                        header: typeof Header !== 'undefined' ? Header : undefined,
                        paragraph: typeof Paragraph !== 'undefined' ? Paragraph : undefined,
                        list: typeof List !== 'undefined' ? List : undefined,
                        quote: typeof Quote !== 'undefined' ? Quote : undefined,
                        delimiter: typeof Delimiter !== 'undefined' ? Delimiter : undefined,
                        image: typeof ImageTool !== 'undefined' ? ImageTool : undefined
                    },
                    data: parsedData,
                    onReady: () => {
                        console.log(`[RFAEditorJS] Готовий: ${this.uniqueId}`);
                    }
                });
            } else {
                // JSON є, але не формат Editor.js — відображаємо як текст
                this.container.innerHTML = `<p style="white-space: pre-wrap;">${this.escapeHtml(rawData)}</p>`;
            }
        } catch (e) {
            console.warn(`[RFAEditorJS] Помилка парсингу JSON для ${this.uniqueId}:`, e);
            this.container.innerHTML = `<p style="white-space: pre-wrap;">${this.escapeHtml(rawData)}</p>`;
        }
    }

    /**
     * Безпечне екранування HTML для захисту від XSS
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

/**
 * Автоматична ініціалізація всіх Editor.js на сторінці.
 * Шукає всі контейнери з класом .rfa-editorjs-content та відповідні data-елементи.
 */
document.addEventListener('DOMContentLoaded', () => {
    const containers = document.querySelectorAll('.rfa-editorjs-content');

    containers.forEach(container => {
        // Витягуємо uniqueId з ID контейнера (формат: "rfa-editorjs-{uniqueId}")
        const containerId = container.id;
        if (!containerId || !containerId.startsWith('rfa-editorjs-')) return;

        const uniqueId = containerId.replace('rfa-editorjs-', '');

        // Перевіряємо, чи є відповідний data-елемент
        const dataElement = document.getElementById('rfa-editorjs-data-' + uniqueId);
        if (dataElement) {
            new RFAEditorJS(uniqueId);
        }
    });
});