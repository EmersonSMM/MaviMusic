import Foundation
import Combine
import WebKit
import AVFoundation
import MediaPlayer
import AppKit

public enum RepeatMode: String, CaseIterable {
    case off = "Desactivado"
    case all = "Repetir todo"
    case one = "Repetir una"
}

public final class AudioEngine: NSObject, ObservableObject, WKScriptMessageHandler {
    public static let shared = AudioEngine()

    // MARK: - Published Playback State
    @Published public var currentTrack: Track?
    @Published public var isPlaying: Bool = false
    @Published public var isBuffering: Bool = false
    @Published public var currentTime: Double = 0.0
    @Published public var duration: Double = 0.0
    @Published public var volume: Double = 1.0 {
        didSet {
            applyVolume()
        }
    }

    // Queue & Modes
    @Published public var queue: [Track] = []
    @Published public var queueIndex: Int = 0
    @Published public var isShuffled: Bool = false
    @Published public var repeatMode: RepeatMode = .off

    // History & Favorites & Local
    @Published public var history: [Track] = []
    @Published public var favorites: [Track] = []
    @Published public var localLibrary: [Track] = []

    // Lyrics
    @Published public var currentLyrics: LyricsData?
    @Published public var activeLyricIndex: Int = 0

    // Private Player Backends
    private var webView: WKWebView?
    private var avPlayer: AVPlayer?
    private var avTimeObserver: Any?
    private var isUsingAVPlayer: Bool = false
    private var originalQueue: [Track] = []

    private let userDefaultsFavoritesKey = "MaviMusic_Favorites"
    private let userDefaultsHistoryKey = "MaviMusic_History"

    override private init() {
        super.init()
        loadPersistedData()
        setupWebKitEngine()
        setupMediaRemoteCommands()
        scanLocalMusic()
    }

    // MARK: - WebKit Player Setup
    private func setupWebKitEngine() {
        let config = WKWebViewConfiguration()
        let contentController = WKUserContentController()
        contentController.add(self, name: "playerBridge")
        config.userContentController = contentController
        config.mediaTypesRequiringUserActionForPlayback = []

        let wv = WKWebView(frame: NSRect(x: 0, y: 0, width: 1, height: 1), configuration: config)
        self.webView = wv

        // Load local player_host.html
        if let htmlPath = Bundle.main.path(forResource: "player_host", ofType: "html"),
           let htmlString = try? String(contentsOfFile: htmlPath, encoding: .utf8) {
            wv.loadHTMLString(htmlString, baseURL: URL(string: "https://music.youtube.com"))
        } else {
            // Embedded fallback HTML
            let fallbackHtml = """
            <!DOCTYPE html><html><head><script src="https://www.youtube.com/iframe_api"></script></head><body>
            <div id="yt-player"></div>
            <script>
            var player;
            function onYouTubeIframeAPIReady() {
                player = new YT.Player('yt-player', {
                    height:'100', width:'100', videoId:'',
                    playerVars: { 'autoplay': 1, 'controls': 0, 'origin': 'https://music.youtube.com' },
                    events: {
                        'onReady': function(){ window.webkit.messageHandlers.playerBridge.postMessage({type:'ready'}); },
                        'onStateChange': function(e){
                            window.webkit.messageHandlers.playerBridge.postMessage({type:'stateChange', state: e.data});
                        }
                    }
                });
                setInterval(function(){
                    if (player && player.getCurrentTime) {
                        window.webkit.messageHandlers.playerBridge.postMessage({
                            type:'timeUpdate',
                            currentTime: player.getCurrentTime() || 0,
                            duration: player.getDuration() || 0
                        });
                    }
                }, 250);
            }
            function loadTrack(id){ if(player && player.loadVideoById) player.loadVideoById(id); }
            function play(){ if(player && player.playVideo) player.playVideo(); }
            function pause(){ if(player && player.pauseVideo) player.pauseVideo(); }
            function seekTo(s){ if(player && player.seekTo) player.seekTo(s, true); }
            function setVolume(v){ if(player && player.setVolume) player.setVolume(v); }
            </script></body></html>
            """
            wv.loadHTMLString(fallbackHtml, baseURL: URL(string: "https://music.youtube.com"))
        }
    }

    // MARK: - WKScriptMessageHandler
    public func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let dict = message.body as? [String: Any],
              let type = dict["type"] as? String else { return }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            switch type {
            case "ready":
                self.applyVolume()
            case "timeUpdate":
                if !self.isUsingAVPlayer {
                    let cur = dict["currentTime"] as? Double ?? 0.0
                    let dur = dict["duration"] as? Double ?? 0.0
                    self.currentTime = cur
                    if dur > 0 && self.duration != dur {
                        self.duration = dur
                    }
                    self.updateLyricProgress(cur)
                    self.updateNowPlayingProgress()
                }
            case "stateChange":
                let state = dict["state"] as? Int ?? -1
                if !self.isUsingAVPlayer {
                    switch state {
                    case 1: // PLAYING
                        self.isPlaying = true
                        self.isBuffering = false
                    case 2: // PAUSED
                        self.isPlaying = false
                        self.isBuffering = false
                    case 3: // BUFFERING
                        self.isBuffering = true
                    case 0: // ENDED
                        self.isPlaying = false
                        self.isBuffering = false
                        self.handleTrackEnded()
                    default:
                        break
                    }
                    self.updateNowPlayingInfo()
                }
            default:
                break
            }
        }
    }

    // MARK: - Playback Control
    public func playTrack(_ track: Track, inQueue: [Track] = []) {
        if !inQueue.isEmpty {
            self.originalQueue = inQueue
            if isShuffled {
                var shuffled = inQueue
                shuffled.shuffle()
                if let idx = shuffled.firstIndex(of: track) {
                    shuffled.remove(at: idx)
                    shuffled.insert(track, at: 0)
                }
                self.queue = shuffled
                self.queueIndex = 0
            } else {
                self.queue = inQueue
                self.queueIndex = inQueue.firstIndex(of: track) ?? 0
            }
        } else if queue.isEmpty || !queue.contains(track) {
            self.queue = [track]
            self.originalQueue = [track]
            self.queueIndex = 0
        }

        self.currentTrack = track
        self.currentTime = 0.0
        self.duration = track.durationSeconds
        self.isPlaying = true
        self.isBuffering = true

        addToHistory(track)
        loadLyrics(for: track)

        if track.isLocal, let pathStr = track.localPath {
            playLocalFile(URL(fileURLWithPath: pathStr))
        } else {
            playYouTubeTrack(videoId: track.id)
        }

        updateNowPlayingInfo()
    }

    private func playYouTubeTrack(videoId: String) {
        isUsingAVPlayer = false
        avPlayer?.pause()
        avPlayer = nil

        let escapedId = videoId.replacingOccurrences(of: "'", with: "\\'")
        let js = "loadTrack('\(escapedId)');"
        webView?.evaluateJavaScript(js, completionHandler: nil)
    }

    private func playLocalFile(_ url: URL) {
        isUsingAVPlayer = true
        webView?.evaluateJavaScript("pause();", completionHandler: nil)

        let playerItem = AVPlayerItem(url: url)
        avPlayer = AVPlayer(playerItem: playerItem)
        avPlayer?.volume = Float(volume)
        avPlayer?.play()

        // Time observer for local file
        let interval = CMTime(seconds: 0.25, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        avTimeObserver = avPlayer?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self = self, self.isUsingAVPlayer else { return }
            let cur = time.seconds
            self.currentTime = cur
            if let dur = self.avPlayer?.currentItem?.duration.seconds, !dur.isNaN && dur > 0 {
                self.duration = dur
            }
            self.updateLyricProgress(cur)
            self.updateNowPlayingProgress()
        }

        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: playerItem,
            queue: .main
        ) { [weak self] _ in
            guard let self = self, self.isUsingAVPlayer else { return }
            self.handleTrackEnded()
        }
    }

    public func togglePlayPause() {
        if isPlaying {
            pause()
        } else {
            play()
        }
    }

    public func play() {
        isPlaying = true
        if isUsingAVPlayer {
            avPlayer?.play()
        } else {
            webView?.evaluateJavaScript("play();", completionHandler: nil)
        }
        updateNowPlayingInfo()
    }

    public func pause() {
        isPlaying = false
        if isUsingAVPlayer {
            avPlayer?.pause()
        } else {
            webView?.evaluateJavaScript("pause();", completionHandler: nil)
        }
        updateNowPlayingInfo()
    }

    public func seek(to seconds: Double) {
        currentTime = seconds
        if isUsingAVPlayer {
            let target = CMTime(seconds: seconds, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
            avPlayer?.seek(to: target)
        } else {
            webView?.evaluateJavaScript("seekTo(\(seconds));", completionHandler: nil)
        }
        updateNowPlayingProgress()
    }

    public func nextTrack() {
        guard !queue.isEmpty else { return }
        if queueIndex + 1 < queue.count {
            queueIndex += 1
            playTrack(queue[queueIndex])
        } else if repeatMode == .all {
            queueIndex = 0
            playTrack(queue[queueIndex])
        }
    }

    public func previousTrack() {
        if currentTime > 3.0 {
            seek(to: 0)
            return
        }
        guard !queue.isEmpty else { return }
        if queueIndex > 0 {
            queueIndex -= 1
            playTrack(queue[queueIndex])
        } else {
            seek(to: 0)
        }
    }

    public func toggleShuffle() {
        isShuffled.toggle()
        guard let current = currentTrack else { return }
        if isShuffled {
            var shuffled = originalQueue
            shuffled.shuffle()
            if let idx = shuffled.firstIndex(of: current) {
                shuffled.remove(at: idx)
                shuffled.insert(current, at: 0)
            }
            queue = shuffled
            queueIndex = 0
        } else {
            queue = originalQueue
            queueIndex = queue.firstIndex(of: current) ?? 0
        }
    }

    public func cycleRepeatMode() {
        switch repeatMode {
        case .off: repeatMode = .all
        case .all: repeatMode = .one
        case .one: repeatMode = .off
        }
    }

    private func handleTrackEnded() {
        switch repeatMode {
        case .one:
            if let current = currentTrack {
                seek(to: 0)
                playTrack(current)
            }
        case .all, .off:
            nextTrack()
        }
    }

    private func applyVolume() {
        let volClamped = max(0.0, min(1.0, volume))
        if isUsingAVPlayer {
            avPlayer?.volume = Float(volClamped)
        } else {
            let pct = Int(volClamped * 100.0)
            webView?.evaluateJavaScript("setVolume(\(pct));", completionHandler: nil)
        }
    }

    // MARK: - Lyrics
    private func loadLyrics(for track: Track) {
        currentLyrics = nil
        activeLyricIndex = 0
        Task {
            if let lyrics = await LyricsService.shared.fetchLyrics(for: track) {
                DispatchQueue.main.async { [weak self] in
                    if self?.currentTrack?.id == track.id {
                        self?.currentLyrics = lyrics
                    }
                }
            }
        }
    }

    private func updateLyricProgress(_ currentSec: Double) {
        guard let lyrics = currentLyrics, lyrics.isSynced, !lyrics.lines.isEmpty else { return }
        for (index, line) in lyrics.lines.enumerated().reversed() {
            if currentSec >= line.timeSeconds {
                if activeLyricIndex != index {
                    activeLyricIndex = index
                }
                break
            }
        }
    }

    // MARK: - Favorites & History & Persistence
    public func toggleFavorite(_ track: Track) {
        if let idx = favorites.firstIndex(where: { $0.id == track.id }) {
            favorites.remove(at: idx)
        } else {
            var fav = track
            fav.isLiked = true
            favorites.insert(fav, at: 0)
        }
        if currentTrack?.id == track.id {
            currentTrack?.isLiked.toggle()
        }
        savePersistedData()
    }

    public func isFavorite(_ track: Track) -> Bool {
        return favorites.contains(where: { $0.id == track.id })
    }

    private func addToHistory(_ track: Track) {
        history.removeAll(where: { $0.id == track.id })
        history.insert(track, at: 0)
        if history.count > 100 {
            history.removeLast()
        }
        savePersistedData()
    }

    private func savePersistedData() {
        let encoder = JSONEncoder()
        if let favData = try? encoder.encode(favorites) {
            UserDefaults.standard.set(favData, forKey: userDefaultsFavoritesKey)
        }
        if let histData = try? encoder.encode(history) {
            UserDefaults.standard.set(histData, forKey: userDefaultsHistoryKey)
        }
    }

    private func loadPersistedData() {
        let decoder = JSONDecoder()
        if let favData = UserDefaults.standard.data(forKey: userDefaultsFavoritesKey),
           let favs = try? decoder.decode([Track].self, from: favData) {
            self.favorites = favs
        }
        if let histData = UserDefaults.standard.data(forKey: userDefaultsHistoryKey),
           let hists = try? decoder.decode([Track].self, from: histData) {
            self.history = hists
        }
    }

    // MARK: - Local Music Scanning
    public func scanLocalMusic() {
        let fileManager = FileManager.default
        let musicDir = fileManager.urls(for: .musicDirectory, in: .userDomainMask).first ?? URL(fileURLWithPath: "~/Music")
        let downloadsDir = fileManager.urls(for: .downloadsDirectory, in: .userDomainMask).first ?? URL(fileURLWithPath: "~/Downloads")

        var scanned: [Track] = []
        let extensions = ["mp3", "m4a", "wav", "flac", "aac"]

        for dir in [musicDir, downloadsDir] {
            guard let enumerator = fileManager.enumerator(at: dir, includingPropertiesForKeys: [.isRegularFileKey], options: [.skipsHiddenFiles, .skipsPackageDescendants]) else { continue }
            for case let fileUrl as URL in enumerator {
                if extensions.contains(fileUrl.pathExtension.lowercased()) {
                    let filename = fileUrl.deletingPathExtension().lastPathComponent
                    let parts = filename.components(separatedBy: " - ")
                    let artist = parts.count > 1 ? parts[0] : "Biblioteca Local"
                    let title = parts.count > 1 ? parts[1] : filename

                    let track = Track(
                        id: "local_\(fileUrl.path.hashValue)",
                        title: title,
                        artist: artist,
                        isLocal: true,
                        localPath: fileUrl.path
                    )
                    scanned.append(track)
                }
            }
        }
        self.localLibrary = scanned
    }

    // MARK: - Now Playing & Media Remote Commands
    private func setupMediaRemoteCommands() {
        let commandCenter = MPRemoteCommandCenter.shared()

        commandCenter.playCommand.addTarget { [weak self] _ in
            self?.play()
            return .success
        }
        commandCenter.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }
        commandCenter.togglePlayPauseCommand.addTarget { [weak self] _ in
            self?.togglePlayPause()
            return .success
        }
        commandCenter.nextTrackCommand.addTarget { [weak self] _ in
            self?.nextTrack()
            return .success
        }
        commandCenter.previousTrackCommand.addTarget { [weak self] _ in
            self?.previousTrack()
            return .success
        }
        commandCenter.changePlaybackPositionCommand.addTarget { [weak self] event in
            if let posEvent = event as? MPChangePlaybackPositionCommandEvent {
                self?.seek(to: posEvent.positionTime)
                return .success
            }
            return .commandFailed
        }
    }

    private func updateNowPlayingInfo() {
        guard let track = currentTrack else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }

        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: track.artist,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: currentTime,
            MPMediaItemPropertyPlaybackDuration: duration,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? 1.0 : 0.0
        ]

        if let album = track.album {
            info[MPMediaItemPropertyAlbumTitle] = album
        }

        // Fetch artwork async for Now Playing
        if let artUrlStr = track.highResArtworkUrl, let artUrl = URL(string: artUrlStr) {
            Task {
                if let (data, _) = try? await URLSession.shared.data(from: artUrl),
                   let image = NSImage(data: data) {
                    let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
                    DispatchQueue.main.async {
                        var currentInfo = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
                        currentInfo[MPMediaItemPropertyArtwork] = artwork
                        MPNowPlayingInfoCenter.default().nowPlayingInfo = currentInfo
                    }
                }
            }
        }

        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func updateNowPlayingProgress() {
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentTime
        info[MPMediaItemPropertyPlaybackDuration] = duration
        info[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }
}
