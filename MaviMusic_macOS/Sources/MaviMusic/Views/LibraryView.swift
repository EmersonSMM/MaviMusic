import SwiftUI

public struct LibraryListView: View {
    public let title: String
    public let subtitle: String?
    public let icon: String
    public let tracks: [Track]
    public let emptyMessage: String
    @ObservedObject var audioEngine = AudioEngine.shared

    public init(title: String, subtitle: String? = nil, icon: String, tracks: [Track], emptyMessage: String) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.tracks = tracks
        self.emptyMessage = emptyMessage
    }

    public var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                HStack(spacing: 14) {
                    Image(systemName: icon)
                        .font(.system(size: 24))
                        .foregroundColor(.accentColor)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.system(size: 26, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                        if let sub = subtitle {
                            Text(sub)
                                .font(.system(size: 13))
                                .foregroundColor(.secondary)
                        }
                    }

                    Spacer()

                    if !tracks.isEmpty {
                        Button(action: {
                            if let first = tracks.first {
                                audioEngine.playTrack(first, inQueue: tracks)
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "play.fill")
                                Text("Reproducir Todo")
                            }
                            .font(.system(size: 12, weight: .semibold))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 28)
                .padding(.top, 24)

                if tracks.isEmpty {
                    VStack(spacing: 12) {
                        Spacer().frame(height: 100)
                        Image(systemName: icon)
                            .font(.system(size: 44))
                            .foregroundColor(.secondary.opacity(0.4))
                        Text(emptyMessage)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    // Track List Rows
                    VStack(spacing: 2) {
                        ForEach(Array(tracks.enumerated()), id: \.element.id) { index, track in
                            TrackRowView(index: index + 1, track: track, allTracks: tracks)
                        }
                    }
                    .padding(.horizontal, 20)
                }
            }
            .padding(.bottom, 40)
        }
    }
}

public struct TrackRowView: View {
    public let index: Int
    public let track: Track
    public let allTracks: [Track]
    @ObservedObject var audioEngine = AudioEngine.shared
    @State private var isHovered: Bool = false

    public var body: some View {
        Button(action: {
            audioEngine.playTrack(track, inQueue: allTracks)
        }) {
            HStack(spacing: 14) {
                // Index / Play indicator
                ZStack {
                    if audioEngine.currentTrack?.id == track.id && audioEngine.isPlaying {
                        Image(systemName: "speaker.wave.2.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.accentColor)
                    } else if isHovered {
                        Image(systemName: "play.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.primary)
                    } else {
                        Text("\(index)")
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundColor(.secondary)
                    }
                }
                .frame(width: 24)

                // Thumbnail
                ZStack {
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .fill(Color.white.opacity(0.06))

                    if let artUrl = track.highResArtworkUrl, let url = URL(string: artUrl) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().aspectRatio(contentMode: .fill)
                            case .failure(_), .empty:
                                Image(systemName: "music.note").foregroundColor(.secondary)
                            @unknown default:
                                EmptyView()
                            }
                        }
                    } else {
                        Image(systemName: "music.note").foregroundColor(.secondary)
                    }
                }
                .frame(width: 42, height: 42)
                .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))

                // Title & Artist
                VStack(alignment: .leading, spacing: 3) {
                    Text(track.title)
                        .font(.system(size: 13, weight: .medium))
                        .lineLimit(1)
                        .foregroundColor(audioEngine.currentTrack?.id == track.id ? .accentColor : .primary)

                    Text(track.artist)
                        .font(.system(size: 11))
                        .lineLimit(1)
                        .foregroundColor(.secondary)
                }

                Spacer()

                if let album = track.album {
                    Text(album)
                        .font(.system(size: 11))
                        .lineLimit(1)
                        .foregroundColor(.secondary.opacity(0.8))
                        .frame(maxWidth: 180, alignment: .leading)
                }

                // Like heart
                Button(action: {
                    audioEngine.toggleFavorite(track)
                }) {
                    Image(systemName: audioEngine.isFavorite(track) ? "heart.fill" : "heart")
                        .font(.system(size: 13))
                        .foregroundColor(audioEngine.isFavorite(track) ? .red : .secondary.opacity(0.6))
                }
                .buttonStyle(.plain)
                .padding(.trailing, 6)

                Text(track.formattedDuration)
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundColor(.secondary)
                    .frame(width: 40, alignment: .trailing)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(isHovered ? Color.white.opacity(0.08) : Color.clear)
            )
        }
        .buttonStyle(.plain)
        .onHover { h in
            isHovered = h
        }
    }
}
