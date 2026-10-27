/**
 * MaviMusic Remote Client
 * Lightweight, zero-dependency ES6 client for iOS Safari & modern mobile browsers.
 */
(function() {
  'use strict';

  // --- Elements ---
  const ambientBg = document.getElementById('ambient-bg');
  const offlineBanner = document.getElementById('offline-banner');
  const pairingModal = document.getElementById('pairing-modal');
  const pairingError = document.getElementById('pairing-error');
  const pinInputs = [
    document.getElementById('pin-0'),
    document.getElementById('pin-1'),
    document.getElementById('pin-2'),
    document.getElementById('pin-3'),
  ];
  const btnSubmitPin = document.getElementById('btn-submit-pin');

  // Player Elements
  const artwork = document.getElementById('artwork');
  const trackTitle = document.getElementById('track-title');
  const trackArtist = document.getElementById('track-artist');
  const btnLike = document.getElementById('btn-like');
  const iconLike = document.getElementById('icon-like');
  const progressBar = document.getElementById('progress-bar');
  const progressFill = document.getElementById('progress-fill');
  const timeCurrent = document.getElementById('time-current');
  const timeTotal = document.getElementById('time-total');
  const btnShuffle = document.getElementById('btn-shuffle');
  const btnPrev = document.getElementById('btn-prev');
  const btnPlay = document.getElementById('btn-play');
  const iconPlay = document.getElementById('icon-play');
  const iconPause = document.getElementById('icon-pause');
  const btnNext = document.getElementById('btn-next');
  const btnRepeat = document.getElementById('btn-repeat');
  const volumeSlider = document.getElementById('volume-slider');

  // Views & Tabs
  const lyricsContainer = document.getElementById('lyrics-container');
  const queueContainer = document.getElementById('queue-container');
  const navTabs = document.querySelectorAll('.nav-tab');
  const views = document.querySelectorAll('.view');

  // --- State ---
  let token = localStorage.getItem('mavimusic_remote_token') || '';
  let socket = null;
  let reconnectTimer = null;
  let isSeeking = false;
  let currentLyrics = [];
  let activeLyricIndex = -1;

  let state = {
    isPlaying: false,
    positionMs: 0,
    durationMs: 0,
    timestamp: Date.now(),
    volume: 1.0,
    shuffle: false,
    repeatMode: 'OFF', // OFF, ALL, ONE
    isLiked: false,
    song: null,
  };

  // --- Device Detection ---
  function getDeviceName() {
    const ua = navigator.userAgent;
    if (/iPhone/.test(ua)) return 'iPhone (Safari)';
    if (/iPad/.test(ua)) return 'iPad (Safari)';
    if (/Android/.test(ua)) return 'Dispositivo Android';
    if (/Macintosh/.test(ua)) return 'Mac Web';
    if (/Windows/.test(ua)) return 'Windows Web';
    return 'Navegador Web';
  }

  // --- Format Time Helper ---
  function formatTime(ms) {
    if (!ms || isNaN(ms) || ms < 0) return '0:00';
    const totalSec = Math.floor(ms / 1000);
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }

  // --- Pairing Flow ---
  async function pair(otp, pin) {
    pairingError.textContent = '';
    btnSubmitPin.disabled = true;
    try {
      const q = new URLSearchParams();
      if (otp) q.set('otp', otp);
      if (pin) q.set('pin', pin);
      q.set('deviceName', getDeviceName());

      const res = await fetch(`/api/pair?${q.toString()}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          otp: otp || undefined,
          pin: pin || undefined,
          deviceName: getDeviceName(),
        })
      });

      const data = await res.json();
      if (res.ok && data.success && data.token) {
        token = data.token;
        localStorage.setItem('mavimusic_remote_token', token);
        pairingModal.style.display = 'none';
        // Clean URL if OTP was present
        if (window.location.search) {
          window.history.replaceState({}, document.title, window.location.pathname);
        }
        connectWebSocket();
      } else {
        pairingError.textContent = data.message || 'Código de emparejamiento inválido.';
        pinInputs.forEach(i => i.value = '');
        pinInputs[0].focus();
      }
    } catch (e) {
      pairingError.textContent = 'Error de conexión con el reproductor.';
    } finally {
      btnSubmitPin.disabled = false;
    }
  }

  function setupPinInputs() {
    pinInputs.forEach((input, index) => {
      input.addEventListener('input', (e) => {
        if (e.target.value.length === 1 && index < pinInputs.length - 1) {
          pinInputs[index + 1].focus();
        }
        checkFullPin();
      });
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Backspace' && !input.value && index > 0) {
          pinInputs[index - 1].focus();
        }
      });
    });

    btnSubmitPin.addEventListener('click', () => {
      const pin = pinInputs.map(i => i.value).join('');
      if (pin.length === 4) pair(null, pin);
      else pairingError.textContent = 'Introduce los 4 dígitos.';
    });
  }

  function checkFullPin() {
    const pin = pinInputs.map(i => i.value).join('');
    if (pin.length === 4) {
      pair(null, pin);
    }
  }

  // --- WebSocket Connection ---
  function connectWebSocket() {
    if (!token) {
      showPairingModal();
      return;
    }

    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${proto}//${window.location.host}/ws?token=${encodeURIComponent(token)}`;

    try {
      socket = new WebSocket(wsUrl);
    } catch (e) {
      scheduleReconnect();
      return;
    }

    socket.onopen = () => {
      offlineBanner.classList.remove('visible');
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
    };

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        handleServerMessage(payload);
      } catch (err) {
        console.error('Failed to parse WebSocket message', err);
      }
    };

    socket.onclose = (event) => {
      offlineBanner.classList.add('visible');
      if (event.code === 4001 || event.code === 4003) {
        // Token revoked or unauthorized
        localStorage.removeItem('mavimusic_remote_token');
        token = '';
        showPairingModal();
      } else {
        scheduleReconnect();
      }
    };

    socket.onerror = () => {
      offlineBanner.classList.add('visible');
    };
  }

  function scheduleReconnect() {
    if (reconnectTimer) return;
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connectWebSocket();
    }, 2000);
  }

  function sendCommand(action, params = {}) {
    if (!socket || socket.readyState !== WebSocket.OPEN) return;
    const msg = JSON.stringify({ action, ...params });
    socket.send(msg);
  }

  // --- Server Message Handler ---
  function handleServerMessage(msg) {
    if (msg.type === 'STATE') {
      const prevSongId = state.song ? state.song.videoId : null;
      state = {
        ...state,
        ...msg.state,
        timestamp: Date.now(),
      };
      renderPlayerState();

      if (state.song && state.song.videoId !== prevSongId) {
        // Fetch new lyrics
        fetchLyrics();
      }
    } else if (msg.type === 'LYRICS') {
      currentLyrics = msg.lines || [];
      renderLyrics();
    } else if (msg.type === 'QUEUE') {
      renderQueue(msg.items || []);
    }
  }

  // --- Render Functions ---
  function renderPlayerState() {
    // Artwork & Ambient background
    const artUrl = state.song && state.song.artworkUrl ? state.song.artworkUrl : '';
    if (artUrl) {
      if (artwork.src !== artUrl) {
        artwork.src = artUrl;
        ambientBg.style.backgroundImage = `url(${artUrl})`;
      }
    } else {
      ambientBg.style.backgroundImage = 'none';
    }

    // Titles
    trackTitle.textContent = state.song ? state.song.title : 'Sin reproducción';
    trackArtist.textContent = state.song ? (state.song.artist || 'MaviMusic') : '—';

    // Play/Pause icon
    if (state.isPlaying) {
      iconPlay.style.display = 'none';
      iconPause.style.display = 'block';
    } else {
      iconPlay.style.display = 'block';
      iconPause.style.display = 'none';
    }

    // Like button
    if (state.isLiked) {
      btnLike.classList.add('active');
      iconLike.setAttribute('fill', 'currentColor');
    } else {
      btnLike.classList.remove('active');
      iconLike.setAttribute('fill', 'none');
    }

    // Shuffle & Repeat
    btnShuffle.classList.toggle('active', !!state.shuffle);
    btnRepeat.classList.toggle('active', state.repeatMode !== 'OFF');
    btnRepeat.style.opacity = state.repeatMode === 'ONE' ? '0.75' : '';

    // Total duration
    timeTotal.textContent = formatTime(state.durationMs);

    // Volume
    if (!document.activeElement || document.activeElement !== volumeSlider) {
      volumeSlider.value = Math.round((state.volume || 1.0) * 100);
    }
  }

  // 60 FPS playhead interpolation loop
  function updatePlayhead() {
    if (!isSeeking && state.durationMs > 0) {
      let currentMs = state.positionMs;
      if (state.isPlaying) {
        const elapsed = Date.now() - state.timestamp;
        currentMs = Math.min(state.durationMs, state.positionMs + elapsed);
      }
      const pct = Math.max(0, Math.min(100, (currentMs / state.durationMs) * 100));
      progressFill.style.width = pct + '%';
      timeCurrent.textContent = formatTime(currentMs);

      // Sync Lyrics Line
      updateActiveLyricLine(currentMs);
    }
    requestAnimationFrame(updatePlayhead);
  }

  // --- Lyrics Rendering & Sync ---
  function renderLyrics() {
    lyricsContainer.innerHTML = '';
    activeLyricIndex = -1;

    if (!currentLyrics || currentLyrics.length === 0) {
      lyricsContainer.innerHTML = '<div class="lyrics-empty">No hay letra disponible para esta pista.</div>';
      return;
    }

    const fragment = document.createDocumentFragment();
    currentLyrics.forEach((line, index) => {
      const div = document.createElement('div');
      div.className = 'lyric-line';
      div.textContent = line.text;
      div.dataset.timeMs = line.timeMs;
      div.addEventListener('click', () => {
        sendCommand('SEEK', { positionMs: line.timeMs });
      });
      fragment.appendChild(div);
    });
    lyricsContainer.appendChild(fragment);
  }

  function updateActiveLyricLine(currentMs) {
    if (!currentLyrics || currentLyrics.length === 0) return;

    let targetIndex = -1;
    for (let i = 0; i < currentLyrics.length; i++) {
      if (currentLyrics[i].timeMs <= currentMs + 250) {
        targetIndex = i;
      } else {
        break;
      }
    }

    if (targetIndex !== activeLyricIndex && targetIndex >= 0) {
      activeLyricIndex = targetIndex;
      const lines = lyricsContainer.children;
      for (let i = 0; i < lines.length; i++) {
        lines[i].classList.toggle('active', i === targetIndex);
      }
      const activeEl = lines[targetIndex];
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }

  async function fetchLyrics() {
    if (!token) return;
    try {
      const res = await fetch('/api/lyrics', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        currentLyrics = data.lines || [];
        renderLyrics();
      }
    } catch (e) {
      console.warn('Lyrics fetch failed', e);
    }
  }

  // --- Queue Rendering ---
  function renderQueue(items) {
    queueContainer.innerHTML = '';
    if (!items || items.length === 0) {
      queueContainer.innerHTML = '<div class="lyrics-empty">La cola está vacía.</div>';
      return;
    }

    const fragment = document.createDocumentFragment();
    items.forEach((item, idx) => {
      const row = document.createElement('div');
      row.className = 'queue-item' + (item.isCurrent ? ' current' : '');

      const img = document.createElement('img');
      img.className = 'queue-thumb';
      img.src = item.artworkUrl || '';
      img.alt = item.title;

      const meta = document.createElement('div');
      meta.className = 'queue-meta';

      const title = document.createElement('div');
      title.className = 'queue-title';
      title.textContent = item.title;

      const artist = document.createElement('div');
      artist.className = 'queue-artist';
      artist.textContent = `${item.artist || 'MaviMusic'} • ${formatTime(item.durationMs)}`;

      meta.appendChild(title);
      meta.appendChild(artist);

      const delBtn = document.createElement('button');
      delBtn.className = 'queue-delete-btn';
      delBtn.textContent = '✕';
      delBtn.setAttribute('aria-label', 'Quitar de la cola');
      delBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        sendCommand('REMOVE_QUEUE_INDEX', { index: item.index });
      });

      row.appendChild(img);
      row.appendChild(meta);
      row.appendChild(delBtn);

      row.addEventListener('click', () => {
        sendCommand('PLAY_QUEUE_INDEX', { index: item.index });
      });

      fragment.appendChild(row);
    });
    queueContainer.appendChild(fragment);
  }

  // --- Progress Scrubber Events ---
  function setupScrubber() {
    function handleSeek(e) {
      if (!state.durationMs) return;
      const rect = progressBar.getBoundingClientRect();
      const clientX = e.touches ? e.touches[0].clientX : e.clientX;
      const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
      const targetMs = Math.round(ratio * state.durationMs);

      progressFill.style.width = (ratio * 100) + '%';
      timeCurrent.textContent = formatTime(targetMs);
      return targetMs;
    }

    progressBar.addEventListener('mousedown', (e) => {
      isSeeking = true;
      const ms = handleSeek(e);
      const onMove = (ev) => handleSeek(ev);
      const onUp = (ev) => {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
        const finalMs = handleSeek(ev);
        isSeeking = false;
        if (finalMs !== undefined) sendCommand('SEEK', { positionMs: finalMs });
      };
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });

    progressBar.addEventListener('touchstart', (e) => {
      isSeeking = true;
      handleSeek(e);
    }, { passive: true });

    progressBar.addEventListener('touchmove', (e) => {
      handleSeek(e);
    }, { passive: true });

    progressBar.addEventListener('touchend', (e) => {
      const rect = progressBar.getBoundingClientRect();
      const touch = e.changedTouches[0];
      const ratio = Math.max(0, Math.min(1, (touch.clientX - rect.left) / rect.width));
      const targetMs = Math.round(ratio * state.durationMs);
      isSeeking = false;
      sendCommand('SEEK', { positionMs: targetMs });
    });
  }

  // --- Navigation & Setup ---
  function setupNavigation() {
    navTabs.forEach((tab) => {
      tab.addEventListener('click', () => {
        const viewId = tab.dataset.view;
        navTabs.forEach(t => t.classList.toggle('active', t === tab));
        views.forEach(v => v.classList.toggle('active', v.id === viewId));
      });
    });
  }

  function setupControls() {
    btnPlay.addEventListener('click', () => sendCommand('PLAY_PAUSE'));
    btnNext.addEventListener('click', () => sendCommand('NEXT'));
    btnPrev.addEventListener('click', () => sendCommand('PREVIOUS'));
    btnLike.addEventListener('click', () => sendCommand('TOGGLE_LIKE'));
    btnShuffle.addEventListener('click', () => sendCommand('TOGGLE_SHUFFLE'));
    btnRepeat.addEventListener('click', () => sendCommand('CYCLE_REPEAT'));

    volumeSlider.addEventListener('input', (e) => {
      const vol = parseFloat(e.target.value) / 100.0;
      sendCommand('SET_VOLUME', { volume: vol });
    });
  }

  function showPairingModal() {
    pairingModal.style.display = 'flex';
    pinInputs.forEach(i => i.value = '');
    setTimeout(() => pinInputs[0].focus(), 100);
  }

  function init() {
    setupPinInputs();
    setupNavigation();
    setupControls();
    setupScrubber();

    // Check for OTP in URL query
    const params = new URLSearchParams(window.location.search);
    const otp = params.get('otp');
    if (otp) {
      pair(otp, null);
    } else if (token) {
      connectWebSocket();
    } else {
      showPairingModal();
    }

    // Visibility wake-up listener for iOS Safari
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        connectWebSocket();
      }
    });
    window.addEventListener('pageshow', () => {
      connectWebSocket();
    });

    // Start 60 FPS interpolation loop
    requestAnimationFrame(updatePlayhead);
  }

  init();
})();
