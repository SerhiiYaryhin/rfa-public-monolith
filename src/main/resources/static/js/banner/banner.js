        document.addEventListener('DOMContentLoaded', function() {

            const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            // --- 1. Збираємо інформацію про банери на сторінці
            const bannerRequests = [];
            const bannerContainers = {};
            const elements = document.getElementsByClassName("rfa-banner");

            for (let i = 0; i < elements.length; i++) {
                const element = elements[i];
                const typeClass = Array.from(element.classList).find(cls => cls.startsWith('rfa-banner-'));
                if (typeClass) {
                    const bannerType = typeClass.replace('rfa-banner-', '').toUpperCase();

                    // Зберігаємо контейнер за типом
                    if (!bannerContainers[bannerType]) {
                        bannerContainers[bannerType] = [];
                    }
                    bannerContainers[bannerType].push(element);
                }
            }

                // Формуємо об'єкт для POST-запиту на сервер
                for (const type in bannerContainers) {
                    bannerRequests.push({
                        type: type,
                        count: bannerContainers[type].length
                    });
                }

                    // --- 2. Відправляємо запит на сервер
                    fetch('/api/2.0/banners/load', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            [header]: token
                        },
                        body: JSON.stringify(bannerRequests)
                    })
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('Network response was not ok');
                        }
                        return response.json();
                    })
                    .then(banners => {
                        console.log("Отримано банери:", banners);
                        // --- 3. Вставляємо банери у відповідні контейнери
                        const loadedBanners = {};
                        banners.forEach(banner => {
                            if (!loadedBanners[banner.bannertype]) {
                                loadedBanners[banner.bannertype] = [];
                            }
                            loadedBanners[banner.bannertype].push(banner);
                        });

                        for (const type in bannerContainers) {
                            const containers = bannerContainers[type];
                            const bannersForType = loadedBanners[type] || [];

                            containers.forEach((container, index) => {
                                const banner = bannersForType[index];
                                if (banner) {
                                    container.innerHTML = createBannerHtml(banner);
                                    incrementBannerView(banner.uuid);
                                } else {
                                    // container.innerHTML = `<p class="text-muted text-center">Немає ${type} банерів.</p>`;
                                    container.innerHTML = ``;
                                }
                            });
                        }
                    })
                    .catch(error => {
                        console.error('Error loading banners:', error);
                    });


                // Якщо немає банерів для завантаження, виходимо
                if (bannerRequests.length === 0) {
                    return;
                }

            // Функція для генерації HTML для банера
            function createBannerHtml(banner) {
                // Використовуємо різні шаблони в залежності від типу банера
                switch (banner.bannertype) {
                                    case 'TEXT':
                                        return `
                                        <div class="col">
                                            <div class="banner card shadow-sm rounded-4 text-center p-3 h-100 d-flex flex-column">
                                                <div class="card-body d-flex flex-column justify-content-center">
                                                    <h5 class="card-title fw-bold mb-2">${banner.title}</h5>
                                                    <p class="card-text small mb-3">${banner.description}</p>
                                                    <a href="${banner.link}" target="_blank"
                                                       class="btn btn-success btn-sm mt-auto track-transition"
                                                       data-uuid="${banner.uuid}">
                                                        Перейти
                                                    </a>
                                                </div>
                                            </div>
                                        </div>
                                        `;
                    case 'TEXT1':
                        return `
                        <div class="col-md-6 col-lg-4 mb-4">
                            <div class="banner card shadow-sm rounded-4 text-center p-3 h-100 d-flex flex-column">
                                <div class="card-body d-flex flex-column justify-content-center">
                                    <h5 class="card-title fw-bold mb-2">${banner.title}</h5>
                                    <p class="card-text small mb-3">${banner.description}</p>
                                    <a href="${banner.link}" target="_blank"
                                       class="btn btn-success btn-sm mt-auto track-transition"
                                       data-uuid="${banner.uuid}">
                                        Перейти
                                    </a>
                                </div>
                            </div>
                        </div>
                        `;
                    case 'IMAGE':
                        return `
                            <div class="banner-image mb-4">
                                <a href="${banner.link}" target="_blank"
                                data-uuid="${banner.uuid}">
                                    <img src="${banner.image_url}" class="img-fluid rounded shadow" alt="${banner.title}">
                                </a>
                            </div>
                        `;
                    default:
                        return ''; // Повертаємо порожній рядок для невідомих типів
                }
            }

                    // Нова функція для збільшення лічильника переглядів
                    function incrementBannerView(bannerId) {
                        fetch(`/api/banners/${bannerId}/view`, {
                            method: 'POST'
                        })
                        .then(response => {
                            if (!response.ok) {
                                console.error('Failed to increment banner view count for ID:', bannerId);
                            }
                        })
                        .catch(error => {
                            console.error('Error incrementing banner view:', error);
                        });
                    }

                // функція збільшення лічильника переходів
                function incrementBannerTransition(bannerUuid) {
                    fetch(`/api/banners/${bannerUuid}/transition`, {
                        method: 'POST'
                    })
                    .then(response => {
                        if (!response.ok) {
                            console.error('Failed to increment banner transition count for ID:', bannerId);
                        }
                    })
                    .catch(error => {
                        console.error('Error incrementing banner transition:', error);
                    });
                }


            // Функція для завантаження та відображення банерів
        function loadAndDisplayBanners(containerId, bannerCount, bannerType) {
            const container = document.getElementById(containerId);
            if (!container) return;

            let url = `/api/banners`;
            if (bannerType) {
                url += `?type=${bannerType}`;
            } else {
                url += `/random?count=${bannerCount}`;
            }

            fetch(url)
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.json();
                })
                .then(banners => {
                    if (banners && banners.length > 0) {
                        let bannersHtml = '';
                        banners.forEach(banner => {
                            bannersHtml += createBannerHtml(banner);
                            incrementBannerView(banner.uuid);
                        });
                        container.innerHTML = bannersHtml;
                    } else {
                        container.innerHTML = '<p class="text-muted text-center">Наразі немає доступних банерів цього типу.</p>';
                    }
                })
                .catch(error => {
                    console.error('Error fetching banners:', error);
                    container.innerHTML = '<p class="text-danger text-center">Не вдалося завантажити банери. Спробуйте пізніше.</p>';
                });
        }

        // ДОДАЄМО ОБРОБНИК ПОДІЙ ДЛЯ ВСІХ КЛІКІВ НА СТОРІНЦІ
        document.addEventListener('click', function(event) {
            const target = event.target.closest('.track-transition');
            if (target) {
                // Запобігаємо стандартній поведінці браузера (переходу за посиланням)
                event.preventDefault();

                // Отримуємо UUID банера з атрибута data-uuid
                const bannerUuid = target.dataset.uuid;

                // Викликаємо функцію для фіксації переходу
                incrementBannerTransition(bannerUuid);

                // Після того, як запит відправлено, перенаправляємо користувача
                window.location.href = target.href;
            }
        });


        // Завантажуємо 3 випадкових текстових банери
        var banner_text = document.getElementsByClassName("rfa-banner-text"); // взяли список елементів текстових банерів
        console.log(banner_text); //HTMLCollection[1]
        // loadAndDisplayBanners('text-banners-container', banner_text.length, 'TEXT');

            // Завантажуємо 1 випадковий банер-зображення
            // loadAndDisplayBanners('image-banners-container', 1, 'IMAGE');
            // Примітка: для цього прикладу припускається, що у вас є поле image_url у моделі Banner
        });
