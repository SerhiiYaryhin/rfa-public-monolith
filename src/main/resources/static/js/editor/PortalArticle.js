class PortalArticle {
    /**
     * Повідомляємо Editor.js, що цей плагін підтримує режим Read-Only
     */
    static get isReadOnlySupported() {
        return true;
    }

    static get toolbox() {
        return {
            title: 'Стаття порталу',
            icon: '<svg width="20" height="20" viewBox="0 0 24 24"><path d="M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10H7v2h10v-2zm0-4H7v2h10V9zm0-4H7v2h10V5z"/></svg>'
        };
    }

    constructor({ data, api, readOnly }) {
        this.api = api;
        this.readOnly = readOnly;
        this.data = {
            id: data.id || '',
            url: data.url || '',
            title: data.title || '',
            author: data.author || '',
            image: data.image || '',
            isExternal: data.isExternal || false
        };
        this.nodes = {
            wrapper: null
        };
    }

    render() {
        this.nodes.wrapper = document.createElement('div');
        this.nodes.wrapper.classList.add('portal-article-wrapper');

        // Базові стилі контейнера
        Object.assign(this.nodes.wrapper.style, {
            border: this.readOnly ? 'none' : '2px dashed #e2e8f0',
            borderRadius: '12px',
            padding: this.readOnly ? '0' : '20px',
            background: this.readOnly ? 'transparent' : '#f8fafc',
            margin: '10px 0',
            clear: 'both'
        });

        if (this.data.url || this.data.id) {
            this._showPreview();
        } else if (!this.readOnly) {
            this._showSearch();
        } else {
            this.nodes.wrapper.style.display = 'none';
        }

        return this.nodes.wrapper;
    }

    _showPreview() {
        this.nodes.wrapper.innerHTML = '';

        const preview = document.createElement('div');
        preview.classList.add('pa-preview');

        // Inline стилі для flex-контейнера
        Object.assign(preview.style, {
            display: 'flex',
            gap: '20px',
            alignItems: 'flex-start',
            background: this.readOnly ? 'transparent' : 'white',
            padding: this.readOnly ? '0' : '15px',
            borderRadius: '8px',
            border: this.readOnly ? 'none' : '1px solid #e2e8f0',
            width: '100%',
            boxSizing: 'border-box'
        });

        // КОНТЕЙНЕР ДЛЯ КАРТИНКИ (Жорстко 20%)
        const imgContainer = document.createElement('div');
        imgContainer.classList.add('pa-img-container');
        Object.assign(imgContainer.style, {
            width: this.readOnly ? '20%' : '80px',
            minWidth: this.readOnly ? '20%' : '80px',
            maxWidth: this.readOnly ? '20%' : '80px',
            flexShrink: '0'
        });

        const img = document.createElement('img');
        img.src = this.data.image || 'https://via.placeholder.com/150';
        Object.assign(img.style, {
            display: 'block',
            width: '100%',
            height: 'auto',
            aspectRatio: '1 / 1',
            objectFit: 'cover',
            borderRadius: '8px'
        });

        // КОНТЕЙНЕР ДЛЯ ІНФО
        const info = document.createElement('div');
        info.style.flex = '1';
        info.innerHTML = `
            <div style="font-weight:bold; font-size:14px; margin-bottom:4px">${this.data.title}</div>
            <div style="font-size:12px; color: #64748b">${this.data.author} ${this.data.isExternal ? '(Зовнішня)' : '(Портал)'}</div>
        `;

        imgContainer.appendChild(img);
        preview.appendChild(imgContainer);
        preview.appendChild(info);

        if (!this.readOnly) {
            const deleteBtn = document.createElement('button');
            deleteBtn.innerHTML = '✕';
            Object.assign(deleteBtn.style, {
                padding: '8px 12px',
                cursor: 'pointer',
                background: '#ef4444',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                fontSize: '12px',
                marginLeft: '10px'
            });
            deleteBtn.onclick = () => {
                this.data = { id: '', url: '', title: '', author: '', image: '', isExternal: false };
                this._showSearch();
            };
            preview.appendChild(deleteBtn);
        }

        this.nodes.wrapper.appendChild(preview);
    }

    _showSearch() {
        if (this.readOnly) return;
        this.nodes.wrapper.innerHTML = `
            <div class="pa-search">
                <input type="text" style="width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; margin-bottom: 10px; box-sizing: border-box;" placeholder="Пошук статті (назва або автор)...">
                <div class="pa-results" style="display:none; background: white; border: 1px solid #e2e8f0; border-radius: 6px; max-height: 200px; overflow-y: auto; margin-bottom: 10px;"></div>
                <span style="font-size: 12px; color: #64748b; text-decoration: underline; cursor: pointer; margin-top: 10px; display: block;" class="pa-toggle">Додати зовнішнє посилання вручну</span>
            </div>
        `;

        const input = this.nodes.wrapper.querySelector('input');
        const resultsDiv = this.nodes.wrapper.querySelector('.pa-results');

        input.addEventListener('input', async (e) => {
            const query = e.target.value;
            if (query.length < 3) return;

            const mockData = [
                { id: 'art_1', title: 'Музика майбутнього', author: 'Олексій Ш.', image: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=100' },
                { id: 'art_2', title: 'Тренди подкастів 2026', author: 'Марія Г.', image: 'https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=100' }
            ];

            resultsDiv.innerHTML = '';
            resultsDiv.style.display = 'block';
            mockData.forEach(item => {
                const div = document.createElement('div');
                div.style.padding = '10px';
                div.style.cursor = 'pointer';
                div.style.borderBottom = '1px solid #f1f5f9';
                div.innerHTML = `<strong>${item.title}</strong> <br> <small>${item.author}</small>`;
                div.onclick = () => {
                    this.data = { ...item, isExternal: false, url: `/article/${item.id}` };
                    this._showPreview();
                };
                resultsDiv.appendChild(div);
            });
        });

        this.nodes.wrapper.querySelector('.pa-toggle').onclick = () => this._showManualForm();
    }

    _showManualForm() {
        if (this.readOnly) return;
        this.nodes.wrapper.innerHTML = `
            <div class="pa-manual">
                <input type="text" id="m-url" style="width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; margin-bottom: 10px; box-sizing: border-box;" placeholder="URL статті">
                <input type="text" id="m-title" style="width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; margin-bottom: 10px; box-sizing: border-box;" placeholder="Заголовок">
                <input type="text" id="m-author" style="width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; margin-bottom: 10px; box-sizing: border-box;" placeholder="Автор">
                <input type="text" id="m-img" style="width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; margin-bottom: 10px; box-sizing: border-box;" placeholder="URL ілюстрації">
                <button style="padding: 8px 12px; cursor: pointer; background: #2563eb; color: white; border: none; border-radius: 4px;">Зберегти</button>
                <span style="font-size: 12px; color: #64748b; text-decoration: underline; cursor: pointer; margin-top: 10px; display: block;" class="pa-toggle">Назад до пошуку</span>
            </div>
        `;
        this.nodes.wrapper.querySelector('button').onclick = () => {
            this.data = {
                id: '',
                isExternal: true,
                url: document.getElementById('m-url').value,
                title: document.getElementById('m-title').value,
                author: document.getElementById('m-author').value,
                image: document.getElementById('m-img').value
            };
            this._showPreview();
        };
        this.nodes.wrapper.querySelector('.pa-toggle').onclick = () => this._showSearch();
    }

    save() {
        return this.data;
    }
}