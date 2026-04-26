import lottieWeb from 'https://cdn.skypack.dev/lottie-web';

class AudioPlayer extends HTMLElement {
    myaudio = null;
    myEpisodeList = null;
    myEpisodeCurrentUUID = null;
    myNextEpisodeUUID = null;

    myplayState = null;

    constructor() {
        super();
        const template = document.querySelector('template');
        const templateContent = template.content;
        const shadow = this.attachShadow({mode: 'open'});
        shadow.appendChild(templateContent.cloneNode(true));
    }

    setmyNextEpisodeUUID(next) {
        this.myNextEpisodeUUID = next;
    }
    getmyNextEpisodeUUID() {
        return this.myNextEpisodeUUID;
    }

    connectedCallback() {
        everything(this);
    }

    setEpisodeList(tEpisodeList) { // закинули uuid епізодів подкасту
        this.myEpisodeList = tEpisodeList;
    }
    getEpisodeList() { // закинули uuid епізодів подкасту
        return this.myEpisodeList;
    }

    getmyEpisodeCurrentUUID() {
       return this.myEpisodeCurrentUUID;
    }
    setmyEpisodeCurrentUUID(cuuid) {
       this.myEpisodeCurrentUUID = cuuid;
    }

    myChangeTrack(itemid,nameEpisode,storeid,file) { // використовую для програвання епізоду на сторінці подкасту

//        if(playState === 'play') {
//            this.myaudio.play();
//            playAnimation.playSegments([14, 27], true);
//            requestAnimationFrame(whilePlaying);
//            playState = 'pause';
//        } else {
//            audio.pause();
//            playAnimation.playSegments([0, 14], true);
//            cancelAnimationFrame(raf);
//            playState = 'play';
//        }

        this.setmyEpisodeCurrentUUID(itemid);
//        this.myEpisodeCurrentUUID = id;
        this.shadowRoot.getElementById('trackName').innerHTML = nameEpisode; // працює, але коряво :(
        this.myaudio.pause();
        this.myaudio.src= "/podcast/audio/"+storeid+"/"+file;
        this.myaudio.load();
        this.myaudio.play();


    }

    async getNextEpisode(curuuid, nextuuid) {
        let getURL = "/podcast/getepisode/"+curuuid+"/"+nextuuid;
        let response = await fetch(getURL);

        if (response.ok) { // если HTTP-статус в диапазоне 200-299 // получаем тело ответа (см. про этот метод ниже)

            let json = await response.json();
            if (json.adv) { // коли це рекламна вставка між епізодами
                this.myChangeTrack(json.current,json.title,json.storeuuid,json.file);
            } else { // коли це черговий епізод
                this.myChangeTrack(json.next,json.title,json.storeuuid,json.file);
            }
        } else {
            alert("getNextEpisode: REST Ошибка HTTP: " + response.status);
        }
    }
}

const everything = function(element) {
    const shadow = element.shadowRoot;
    const audioPlayerContainer = shadow.getElementById('audio-player-container');
    const playIconContainer = shadow.getElementById('play-icon');
    const seekSlider = shadow.getElementById('seek-slider');
    const volumeSlider = shadow.getElementById('volume-slider');
    const muteIconContainer = shadow.getElementById('mute-icon');
    const audio = shadow.querySelector('audio');
    const durationContainer = shadow.getElementById('duration');
    const currentTimeContainer = shadow.getElementById('current-time');
    const outputContainer = shadow.getElementById('volume-output');
    let playState = 'play';
    element.myplayState = playState; // Намагаюся звʼязати змінні зі змінними в класі.
    let muteState = 'unmute';
    let raf = null;

    element.myaudio = audio;
    audio.src = element.getAttribute('data-src');

    const playAnimation = lottieWeb.loadAnimation({
        container: playIconContainer,
        path: 'https://maxst.icons8.com/vue-static/landings/animated-icons/icons/pause/pause.json',
        renderer: 'svg',
        loop: false,
        autoplay: false,
        name: "Play Animation",
    });

    const muteAnimation = lottieWeb.loadAnimation({
        container: muteIconContainer,
        path: 'https://maxst.icons8.com/vue-static/landings/animated-icons/icons/mute/mute.json',
        renderer: 'svg',
        loop: false,
        autoplay: false,
        name: "Mute Animation",
    });

    playAnimation.goToAndStop(14, true);

    const whilePlaying = () => {
        seekSlider.value = Math.floor(audio.currentTime);
        currentTimeContainer.textContent = calculateTime(seekSlider.value);
        audioPlayerContainer.style.setProperty('--seek-before-width', `${seekSlider.value / seekSlider.max * 100}%`);
        raf = requestAnimationFrame(whilePlaying);
    }

    //const audioSetTrack =
    function audioSetTrack(srcTrack) {
        const audio = shadow.querySelector('audio');
        audio.src = srcTrack;
    }

    const showRangeProgress = (rangeInput) => {
        if(rangeInput === seekSlider) audioPlayerContainer.style.setProperty('--seek-before-width', rangeInput.value / rangeInput.max * 100 + '%');
        else audioPlayerContainer.style.setProperty('--volume-before-width', rangeInput.value / rangeInput.max * 100 + '%');
    }

    const calculateTime = (secs) => {
        const minutes = Math.floor(secs / 60);
        const seconds = Math.floor(secs % 60);
        const returnedSeconds = seconds < 10 ? `0${seconds}` : `${seconds}`;
        return `${minutes}:${returnedSeconds}`;
    }

    const displayDuration = () => {
        durationContainer.textContent = calculateTime(audio.duration);
    }

    const setSliderMax = () => {
        seekSlider.max = Math.floor(audio.duration);
    }

    const displayBufferedAmount = () => {
        if (audio.buffered.length > 0) {
            const bufferedAmount = Math.floor(audio.buffered.end(audio.buffered.length - 1));
            audioPlayerContainer.style.setProperty('--buffered-width', `${(bufferedAmount / seekSlider.max) * 100}%`);
        }
    }

    if (audio.readyState > 0) {
        displayDuration();
        setSliderMax();
        displayBufferedAmount();
    } else {
        audio.addEventListener('loadedmetadata', () => {
            displayDuration();
            setSliderMax();
            displayBufferedAmount();
        });
    }



    playIconContainer.addEventListener('click', () => {
        if(playState === 'play') {
            audio.play();
            playAnimation.playSegments([14, 27], true);
            requestAnimationFrame(whilePlaying);
            playState = 'pause';
        } else {
            audio.pause();
            playAnimation.playSegments([0, 14], true);
            cancelAnimationFrame(raf);
            playState = 'play';
        }
    });

    muteIconContainer.addEventListener('click', () => {
        if(muteState === 'unmute') {
            muteAnimation.playSegments([0, 15], true);
            audio.muted = true;
            muteState = 'mute';
        } else {
            muteAnimation.playSegments([15, 25], true);
            audio.muted = false;
            muteState = 'unmute';
        }
    });

    audio.addEventListener("ended", (event) => { // працює
      let ttt = element.getEpisodeList(); // функція описана в HTML файлі
      let ppp = element.getmyEpisodeCurrentUUID();
      let itemuuid = null;
      let ii = null;
      let i = 0;
      for (i; i < ttt.length; ++i) {
        if (ttt[i] === ppp)  {
          itemuuid = ttt[i];
          ii = i;
          break;
        }
      }
        ii = ii+1;
        if (ii > ttt.length - 1) {
            ii = 0;
        }
        element.getNextEpisode(element.getmyEpisodeCurrentUUID(), ttt[ii]);
    });

    audio.addEventListener('progress', displayBufferedAmount);

    seekSlider.addEventListener('input', (e) => {
        showRangeProgress(e.target);
        currentTimeContainer.textContent = calculateTime(seekSlider.value);
        if(!audio.paused) {
            cancelAnimationFrame(raf);
        }
    });

    seekSlider.addEventListener('change', () => {
        audio.currentTime = seekSlider.value;
        if(!audio.paused) {
            requestAnimationFrame(whilePlaying);
        }
    });

    volumeSlider.addEventListener('input', (e) => {
        const value = e.target.value;
        showRangeProgress(e.target);
        outputContainer.textContent = value;
        audio.volume = value / 100;
    });

    if('mediaSession' in navigator) {
        navigator.mediaSession.metadata = new MediaMetadata({
            title: 'Komorebi',
            artist: 'Anitek',
            album: 'MainStay',
            artwork: [
                { src: 'https://assets.codepen.io/4358584/1.300.jpg', sizes: '96x96', type: 'image/png' },
                { src: 'https://assets.codepen.io/4358584/1.300.jpg', sizes: '128x128', type: 'image/png' },
                { src: 'https://assets.codepen.io/4358584/1.300.jpg', sizes: '192x192', type: 'image/png' },
                { src: 'https://assets.codepen.io/4358584/1.300.jpg', sizes: '256x256', type: 'image/png' },
                { src: 'https://assets.codepen.io/4358584/1.300.jpg', sizes: '384x384', type: 'image/png' },
                { src: 'https://assets.codepen.io/4358584/1.300.jpg', sizes: '512x512', type: 'image/png' }
            ]
        });
        navigator.mediaSession.setActionHandler('play', () => {
            if(playState === 'play') {
                audio.play();
                playAnimation.playSegments([14, 27], true);
                requestAnimationFrame(whilePlaying);
                playState = 'pause';
            } else {
                audio.pause();
                playAnimation.playSegments([0, 14], true);
                cancelAnimationFrame(raf);
                playState = 'play';
            }
        });
        navigator.mediaSession.setActionHandler('pause', () => {
            if(playState === 'play') {
                audio.play();
                playAnimation.playSegments([14, 27], true);
                requestAnimationFrame(whilePlaying);
                playState = 'pause';
            } else {
                audio.pause();
                playAnimation.playSegments([0, 14], true);
                cancelAnimationFrame(raf);
                playState = 'play';
            }
        });
        navigator.mediaSession.setActionHandler('seekbackward', (details) => {
            audio.currentTime = audio.currentTime - (details.seekOffset || 10);
        });
        navigator.mediaSession.setActionHandler('seekforward', (details) => {
            audio.currentTime = audio.currentTime + (details.seekOffset || 10);
        });
        navigator.mediaSession.setActionHandler('seekto', (details) => {
            if (details.fastSeek && 'fastSeek' in audio) {
              audio.fastSeek(details.seekTime);
              return;
            }
            audio.currentTime = details.seekTime;
        });
        navigator.mediaSession.setActionHandler('stop', () => {
            audio.currentTime = 0;
            seekSlider.value = 0;
            audioPlayerContainer.style.setProperty('--seek-before-width', '0%');
            currentTimeContainer.textContent = '0:00';
            if(playState === 'pause') {
                playAnimation.playSegments([0, 14], true);
                cancelAnimationFrame(raf);
                playState = 'play';
            }
        });
    }
}



customElements.define('audio-player', AudioPlayer);