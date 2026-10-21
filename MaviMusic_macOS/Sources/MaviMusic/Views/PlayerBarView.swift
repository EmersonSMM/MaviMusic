import SwiftUI

public struct PlayerBarView: View {
    @ObservedObject var audioEngine = AudioEngine.shared
    @Binding var showLyrics: Bool
    @State private var isSeeking: Bool = false
    @State private var seekPosition: Double = 0.0

    public init(showLyrics: Binding<Bool>) {
        self._showLyrics = showLyrics
    }

    public var body: some View {
        HStack(spacing: 20) {
            // Left: Current Track Info
            HStack(spacing: 12) {
                if let track = audioEngine.currentTrack {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
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
                                        .font(.system(size: 20))
                                        .foregroundColor(.secondary)
                                @unknown default:
                                    EmptyView()
                                }
                            }
                        } else {
                            Image(systemName: "music.note")
                                .font(.system(size: 20))
                                .foregroundColor(.secondary)
                        }
                    }
                    .frame(width: 52, height: 52)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .shadow(color: Color.black.opacity(0.3), radius: 6, x: 0, y: 3)

                    VStack(alignment: .leading, spacing: 3) {
                        Text(track.title)
                            .font(.system(size: 13, weight: .semibold))
                            .lineLimit(1)
                            .foregroundColor(.primary)

                        Text(track.artist)
                            .font(.system(size: 11, weight: .regular))
                            .lineLimit(1)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: 180, alignment: .leading)

                    Button(action: {
                        audioEngine.toggleFavorite(track)
                    }) {
                        Image(systemName: audioEngine.isFavorite(track) ? "heart.fill" : "heart")
                            .font(.system(size: 15))
                            .foregroundColor(audioEngine.isFavorite(track) ? .red : .secondary)
                    }
                    .buttonStyle(.plain)
                    .padding(.leading, 4)

                } else {
                    HStack(spacing: 10) {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color.white.opacity(0.05))
                            .frame(width: 52, height: 52)
                            .overlay(
                                Image(systemName: "music.note")
                                    .foregroundColor(.secondary.opacity(0.5))
                            )
                        Text("Elige una canción para reproducir")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: 240, alignment: .leading)
                }
            }
            .frame(width: 260, alignment: .leading)

            Spacer()

            // Center: Scrubber & Controls
            VStack(spacing: 6) {
                // Media Buttons
                HStack(spacing: 22) {
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
                            .font(.system(size: 16))
                            .foregroundColor(.primary)
                    }
                    .buttonStyle(.plain)

                    // Play / Pause Circle
                    Button(action: { audioEngine.togglePlayPause() }) {
                        ZStack {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 38, height: 38)
                                .shadow(color: Color.white.opacity(0.35), radius: 8, x: 0, y: 2)

                            if audioEngine.isBuffering {
                                ProgressView()
                                    .scaleEffect(0.7)
                                    .colorInvert()
                            } else {
                                Image(systemName: audioEngine.isPlaying ? "pause.fill" : "play.fill")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.black)
                                    .offset(x: audioEngine.isPlaying ? 0 : 1)
                            }
                        }
                    }
                    .buttonStyle(.plain)

                    // Next
                    Button(action: { audioEngine.nextTrack() }) {
                        Image(systemName: "forward.fill")
                            .font(.system(size: 16))
                            .foregroundColor(.primary)
                    }
                    .buttonStyle(.plain)

                    // Repeat Mode
                    Button(action: { audioEngine.cycleRepeatMode() }) {
                        Image(systemName: audioEngine.repeatMode == .one ? "repeat.1" : "repeat")
                            .font(.system(size: 13))
                            .foregroundColor(audioEngine.repeatMode != .off ? .accentColor : .secondary)
                    }
                    .buttonStyle(.plain)
                }

                // Scrubber Bar
                HStack(spacing: 10) {
                    Text(Track.formatSeconds(isSeeking ? seekPosition : audioEngine.currentTime))
                        .font(.system(size: 10, weight: .medium, design: .monospaced))
                        .foregroundColor(.secondary)
                        .frame(width: 34, alignment: .trailing)

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
                    .frame(maxWidth: 480)

                    Text(Track.formatSeconds(audioEngine.duration))
                        .font(.system(size: 10, weight: .medium, design: .monospaced))
                        .foregroundColor(.secondary)
                        .frame(width: 34, alignment: .leading)
                }
            }
            .frame(maxWidth: .infinity)

            Spacer()

            // Right: Volume & Utilities
            HStack(spacing: 14) {
                // Lyrics Toggle
                Button(action: {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        showLyrics.toggle()
                    }
                }) {
                    Image(systemName: showLyrics ? "quote.bubble.fill" : "quote.bubble")
                        .font(.system(size: 15))
                        .foregroundColor(showLyrics ? .accentColor : .secondary)
                }
                .buttonStyle(.plain)
                .help("Mostrar letra en tiempo real")

                // Volume Bar
                HStack(spacing: 6) {
                    Button(action: {
                        if audioEngine.volume > 0 {
                            audioEngine.volume = 0
                        } else {
                            audioEngine.volume = 0.8
                        }
                    }) {
                        Image(systemName: audioEngine.volume == 0 ? "speaker.slash.fill" : "speaker.wave.2.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)

                    Slider(value: $audioEngine.volume, in: 0...1)
                        .frame(width: 85)
                        .accentColor(.white)
                }
            }
            .frame(width: 180, alignment: .trailing)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
        .background(
            VisualEffectView(material: .headerView, blendingMode: .withinWindow)
                .overlay(
                    Rectangle()
                        .frame(height: 1)
                        .foregroundColor(Color.white.opacity(0.1)),
                    alignment: .top
                )
        )
    }
}
