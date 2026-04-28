class PortalArticle {
    static get toolbox() {
        return {
            title: 'Стаття порталу',
            icon: '<svg width="20" height="20" viewBox="0 0 24 24"><path d="M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10H7v2h10v-2zm0-4H7v2h10V9zm0-4H7v2h10V5z"/></svg>'
        };
    }

    constructor({ data, api }) {
        this.api = api;
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

        // Стилі безпосередньо в JS для незалежності плагіна
        const style = document.createElement('style');
        style.innerHTML = `
            .portal-article-wrapper { border: 2px dashed #e2e8f0; border-radius: 12px; padding: 20px; background: #f8fafc; }
            .pa-input { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; margin-bottom: 10px; box-sizing: border-box; }
            .pa-results { background: white; border: 1px solid #e2e8f0; border-radius: 6px; max-height: 200px; overflow-y: auto; margin-bottom: 10px; }
            .pa-item { padding: 10px; cursor: pointer; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
            .pa-item:hover { background: #eff6ff; }
            .pa-preview { display: flex; gap: 15px; align-items: center; background: white; padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0; }
            .pa-preview img { width: 60px; height: 60px; object-fit: cover; border-radius: 4px; }
            .pa-btn { padding: 8px 12px; cursor: pointer; background: #2563eb; color: white; border: none; border-radius: 4px; font-size: 12px; }
            .pa-toggle { font-size: 12px; color: #64748b; text-decoration: underline; cursor: pointer; margin-top: 10px; display: block; }
        `;
        this.nodes.wrapper.appendChild(style);

        if (this.data.url || this.data.id) {
            this._showPreview();
        } else {
            this._showSearch();
        }

        return this.nodes.wrapper;
    }

    _showSearch() {
        this.nodes.wrapper.innerHTML = `
            <div class="pa-search">
                <input type="text" class="pa-input" placeholder="Пошук статті (назва або автор)...">
                <div class="pa-results" style="display:none"></div>
                <span class="pa-toggle">Додати зовнішнє посилання вручну</span>
            </div>
        `;

        const input = this.nodes.wrapper.querySelector('input');
        const resultsDiv = this.nodes.wrapper.querySelector('.pa-results');

        input.addEventListener('input', async (e) => {
            const query = e.target.value;
            if (query.length < 3) return;

            // Імітація запиту до API порталу
            const mockData = [
                { id: 'art_1', title: 'Музика майбутнього', author: 'Олексій Ш.', image: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=100' },
                { id: 'art_2', title: 'Тренди подкастів 2026', author: 'Марія Г.', image: 'https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=100' }
            ];

            resultsDiv.innerHTML = '';
            resultsDiv.style.display = 'block';
            mockData.forEach(item => {
                const div = document.createElement('div');
                div.classList.add('pa-item');
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
        this.nodes.wrapper.innerHTML = `
            <div class="pa-manual">
                <input type="text" id="m-url" class="pa-input" placeholder="URL статті">
                <input type="text" id="m-title" class="pa-input" placeholder="Заголовок">
                <input type="text" id="m-author" class="pa-input" placeholder="Автор">
                <input type="text" id="m-img" class="pa-input" placeholder="URL ілюстрації">
                <button class="pa-btn">Зберегти як зовнішню</button>
                <span class="pa-toggle">Повернутися до пошуку</span>
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

    _showPreview() {
        this.nodes.wrapper.innerHTML = `
            <div class="pa-preview">
                <img src="${this.data.image}">
                <div style="flex-grow:1">
                    <div style="font-weight:bold; font-size:14px">${this.data.title}</div>
                    <div style="font-size:12px; color: #64748b">${this.data.author} ${this.data.isExternal ? '(Зовнішня)' : '(Портал)'}</div>
                </div>
                <button class="pa-btn" style="background:#ef4444">✕</button>
            </div>
        `;
        this.nodes.wrapper.querySelector('button').onclick = () => {
            this.data = { id: '', url: '', title: '', author: '', image: '', isExternal: false };
            this._showSearch();
        };
    }

    save() {
        return this.data;
    }
}
