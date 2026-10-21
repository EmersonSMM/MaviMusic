import SwiftUI

public struct MenuBarPlayerView: View {
    @ObservedObject var audioEngine = AudioEngine.shared
    @State private var isSeeking: Bool = false
    @State private var seekPosition: Double = 0.0

    public init() {}

    public var body: some View {
        VStack(spacing: 14) {
            // Header Bar
            HStack {
                HStack(spacing: 6) {
                    Image("MenuBarIcon")
                        .renderingMode(.template)
                        .resizable()
                        .frame(width: 14, height: 14)
                        .foregroundColor(.accentColor)
                    Text("MaviMusic")
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                }

                Spacer()

                Button(action: {
                    openMainWindow()
                }) {
                    HStack(spacing: 4) {
                        Text("Abrir")
                            .font(.system(size: 11, weight: .medium))
                        Image(systemName: "arrow.up.forward.app")
                            .font(.system(size: 10))
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.12))
                    .cornerRadius(8)
                }
                .buttonStyle(.plain)
            }

            if let track = audioEngine.currentTrack {
                // Active Track Content
                HStack(spacing: 12) {
                    // Artwork
                    ZStack {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color.white.opacity(0.08))

                        if let artUrl = track.highResArtworkUrl, let url = URL(string: artUrl) {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let image):
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                case .failure(_), .empty:
                                    Image(systemName: "music.note")
                                        .font(.system(size: 26))
                                        .foregroundColor(.secondary)
                                @unknown default:
                                    EmptyView()
                                }
                            }
                        } else {
                            Image(systemName: "music.note")
                                .font(.system(size: 26))
                                .foregroundColor(.secondary)
                        }
                    }
                    .frame(width: 68, height: 68)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .shadow(color: Color.black.opacity(0.35), radius: 6, x: 0, y: 3)

                    // Track Info
                    VStack(alignment: .leading, spacing: 4) {
                        Text(track.title)
                            .font(.system(size: 13, weight: .semibold))
                            .lineLimit(1)
                            .foregroundColor(.primary)

                        Text(track.artist)
                            .font(.system(size: 11, weight: .regular))
                            .lineLimit(1)
                            .foregroundColor(.secondary)

                        if let album = track.album {
                            Text(album)
                                .font(.system(size: 10))
                                .lineLimit(1)
                                .foregroundColor(.secondary.opacity(0.8))
                        }
                    }

                    Spacer()

                    // Like Button
                    Button(action: {
                        audioEngine.toggleFavorite(track)
                    }) {
                        Image(systemName: audioEngine.isFavorite(track) ? "heart.fill" : "heart")
                            .font(.system(size: 15))
                            .foregroundColor(audioEngine.isFavorite(track) ? .red : .secondary)
                    }
                    .buttonStyle(.plain)
                }

                // Scrubber / Progress Bar
                VStack(spacing: 4) {
                    Slider(
                        value: Binding(
                            get: { isSeeking ? seekPosition : audioEngine.currentTime },
                            set: { newValue in
                                isSeeking = true
                                seekPosition = newValue
                            }
                        ),
                        in: 0...max(audioEngine.duration, 1.0),
                        onEditingChanged: { editing in
                            if !editing {
                                audioEngine.seek(to: seekPosition)
                                isSeeking = false
                            }
                        }
                    )
                    .accentColor(.white)

                    HStack {
                        Text(Track.formatSeconds(isSeeking ? seekPosition : audioEngine.currentTime))
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(Track.formatSeconds(audioEngine.duration))
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(.secondary)
                    }
                }

                // Playback Controls
                HStack(spacing: 20) {
                    // Shuffle
                    Button(action: { audioEngine.toggleShuffle() }) {
                        Image(systemName: "shuffle")
                            .font(.system(size: 13))
                            .foregroundColor(audioEngine.isShuffled ? .accentColor : .secondary)
                    }
                    .buttonStyle(.plain)

                    // Previous
                    Button(action: { audioEngine.previousTrack() }) {
                        Image(systemName: "backward.fill")
                            .font(.system(size: 15))
                            .foregroundColor(.primary)
                    }
                    .buttonStyle(.plain)

                    // Play / Pause Circle
                    Button(action: { audioEngine.togglePlayPause() }) {
                        ZStack {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 36, height: 36)
                                .shadow(color: Color.white.opacity(0.3), radius: 6, x: 0, y: 2)

                            if audioEngine.isBuffering {
                                ProgressView()
                                    .scaleEffect(0.7)
                                    .colorInvert()
                            } else {
                                Image(systemName: audioEngine.isPlaying ? "pause.fill" : "play.fill")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.black)
                                    .offset(x: audioEngine.isPlaying ? 0 : 1)
                            }
                        }
                    }
                    .buttonStyle(.plain)

                    // Next
                    Button(action: { audioEngine.nextTrack() }) {
                        Image(systemName: "forward.fill")
                            .font(.system(size: 15))
                            .foregroundColor(.primary)
                    }
                    .buttonStyle(.plain)

                    // Repeat
                    Button(action: { audioEngine.cycleRepeatMode() }) {
                        Image(systemName: audioEngine.repeatMode == .one ? "repeat.1" : "repeat")
                            .font(.system(size: 13))
                            .foregroundColor(audioEngine.repeatMode != .off ? .accentColor : .secondary)
                    }
                    .buttonStyle(.plain)
                }

                // Volume Bar
                HStack(spacing: 8) {
                    Image(systemName: "speaker.fill")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                    Slider(value: $audioEngine.volume, in: 0...1)
                        .accentColor(.white)
                    Image(systemName: "speaker.wave.3.fill")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                }
                .padding(.top, 2)

            } else {
                // Empty state when nothing is playing
                VStack(spacing: 12) {
                    Image(systemName: "music.quarternote.3")
                        .font(.system(size: 38))
                        .foregroundColor(.secondary.opacity(0.6))
                        .padding(.top, 8)

                    Text("Ninguna canción en reproducción")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.secondary)

                    Button(action: {
                        openMainWindow()
                    }) {
                        Text("Explorar Música en MaviMusic")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color.accentColor)
                            .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                    .padding(.bottom, 6)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
            }
        }
        .padding(14)
        .frame(width: 320)
        .background(
            VisualEffectView(material: .popover, blendingMode: .withinWindow)
        )
    }

    private func openMainWindow() {
        NSApp.activate(ignoringOtherApps: true)
        if let window = NSApp.windows.first(where: { $0.canBecomeMain }) {
            window.makeKeyAndOrderFront(nil)
        }
    }
}
