import SwiftUI

public struct LyricsView: View {
    @ObservedObject var audioEngine = AudioEngine.shared
    @Binding var isPresented: Bool

    public init(isPresented: Binding<Bool>) {
        self._isPresented = isPresented
    }

    public var body: some View {
        ZStack {
            // Blurred Artwork Background (Apple Music style)
            if let track = audioEngine.currentTrack,
               let artUrl = track.highResArtworkUrl,
               let url = URL(string: artUrl) {
                AsyncImage(url: url) { phase in
                    if case .success(let img) = phase {
                        img
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .blur(radius: 70)
                            .overlay(Color.black.opacity(0.55))
                            .ignoresSafeArea()
                    }
                }
            } else {
                VisualEffectView(material: .fullScreenUI, blendingMode: .withinWindow)
                    .overlay(Color.black.opacity(0.6))
                    .ignoresSafeArea()
            }

            // Close button top right
            VStack {
                HStack {
                    Spacer()
                    Button(action: {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                            isPresented = false
                        }
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 26))
                            .foregroundColor(.white.opacity(0.6))
                    }
                    .buttonStyle(.plain)
                    .padding(24)
                }
                Spacer()
            }

            // Main Lyrics & Artwork Content
            HStack(spacing: 50) {
                // Left Column: Large Artwork & Track Metadata
                if let track = audioEngine.currentTrack {
                    VStack(alignment: .leading, spacing: 18) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .fill(Color.white.opacity(0.1))

                            if let artUrl = track.highResArtworkUrl, let url = URL(string: artUrl) {
                                AsyncImage(url: url) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                    case .failure(_), .empty:
                                        Image(systemName: "music.note")
                                            .font(.system(size: 60))
                                            .foregroundColor(.white.opacity(0.5))
                                    @unknown default:
                                        EmptyView()
                                    }
                                }
                            }
                        }
                        .frame(width: 320, height: 320)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .shadow(color: Color.black.opacity(0.5), radius: 24, x: 0, y: 12)

                        VStack(alignment: .leading, spacing: 6) {
                            Text(track.title)
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.white)
                                .lineLimit(2)

                            Text(track.artist)
                                .font(.system(size: 17, weight: .medium))
                                .foregroundColor(.white.opacity(0.7))
                                .lineLimit(1)

                            if let album = track.album {
                                Text(album)
                                    .font(.system(size: 14))
                                    .foregroundColor(.white.opacity(0.5))
                                    .lineLimit(1)
                            }
                        }
                    }
                    .frame(width: 340)
                    .padding(.leading, 40)
                }

                // Right Column: Synced Scrolling Lyrics
                ScrollViewReader { proxy in
                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 24) {
                            if let lyrics = audioEngine.currentLyrics, !lyrics.lines.isEmpty {
                                Color.clear.frame(height: 100)

                                ForEach(Array(lyrics.lines.enumerated()), id: \.element.id) { index, line in
                                    let isActive = (index == audioEngine.activeLyricIndex)

                                    Button(action: {
                                        audioEngine.seek(to: line.timeSeconds)
                                    }) {
                                        Text(line.text)
                                            .font(.system(size: isActive ? 28 : 22, weight: isActive ? .bold : .medium, design: .rounded))
                                            .foregroundColor(isActive ? .white : .white.opacity(0.38))
                                            .scaleEffect(isActive ? 1.03 : 1.0, anchor: .leading)
                                            .animation(.spring(response: 0.35, dampingFraction: 0.75), value: isActive)
                                            .shadow(color: isActive ? Color.white.opacity(0.3) : Color.clear, radius: 10)
                                            .multilineTextAlignment(.leading)
                                    }
                                    .buttonStyle(.plain)
                                    .id(index)
                                }

                                Color.clear.frame(height: 140)
                            } else {
                                VStack(spacing: 12) {
                                    Spacer().frame(height: 160)
                                    Image(systemName: "music.mic")
                                        .font(.system(size: 40))
                                        .foregroundColor(.white.opacity(0.4))
                                    Text("Letra no disponible para esta canción")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.white.opacity(0.6))
                                }
                            }
                        }
                        .padding(.trailing, 40)
                    }
                    .onChange(of: audioEngine.activeLyricIndex) { newIndex in
                        withAnimation(.easeInOut(duration: 0.4)) {
                            proxy.scrollTo(newIndex, anchor: .center)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
