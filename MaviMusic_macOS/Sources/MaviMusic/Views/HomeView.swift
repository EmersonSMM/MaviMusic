import SwiftUI

public struct HomeView: View {
    @ObservedObject var audioEngine = AudioEngine.shared
    @State private var shelves: [MusicShelf] = []
    @State private var isLoading: Bool = true
    @State private var errorMessage: String? = nil

    public init() {}

    public var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 28) {
                // Hero Glass Header
                VStack(alignment: .leading, spacing: 6) {
                    Text("Bienvenido a MaviMusic")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)

                    Text("Descubre, reproduce y disfruta de tu música con estética Liquid Glass.")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 28)
                .padding(.top, 24)

                if isLoading {
                    VStack(spacing: 16) {
                        ProgressView()
                            .scaleEffect(1.2)
                        Text("Cargando selecciones musicales...")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, minHeight: 300)
                } else if let error = errorMessage {
                    VStack(spacing: 12) {
                        Image(systemName: "wifi.exclamationmark")
                            .font(.system(size: 32))
                            .foregroundColor(.secondary)
                        Text(error)
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                        Button("Reintentar") {
                            loadFeed()
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .frame(maxWidth: .infinity, minHeight: 250)
                } else {
                    // Shelves
                    ForEach(shelves) { shelf in
                        VStack(alignment: .leading, spacing: 14) {
                            HStack(alignment: .firstTextBaseline) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(shelf.title)
                                        .font(.system(size: 20, weight: .bold))
                                        .foregroundColor(.primary)
                                    if let sub = shelf.subtitle {
                                        Text(sub)
                                            .font(.system(size: 12))
                                            .foregroundColor(.secondary)
                                    }
                                }
                                Spacer()
                            }
                            .padding(.horizontal, 28)

                            // Horizontal scroll of responsive cards
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 16) {
                                    ForEach(shelf.tracks) { track in
                                        TrackCardView(track: track, allTracks: shelf.tracks)
                                    }
                                }
                                .padding(.horizontal, 28)
                                .padding(.vertical, 4)
                            }
                        }
                    }
                }
            }
            .padding(.bottom, 40)
        }
        .onAppear {
            if shelves.isEmpty {
                loadFeed()
            }
        }
    }

    private func loadFeed() {
        isLoading = true
        errorMessage = nil
        Task {
            do {
                let fetched = try await YouTubeMusicClient.shared.getHomeFeed()
                DispatchQueue.main.async {
                    self.shelves = fetched
                    self.isLoading = false
                }
            } catch {
                DispatchQueue.main.async {
                    self.errorMessage = "No se pudo cargar el inicio. Verifica tu conexión a internet."
                    self.isLoading = false
                }
            }
        }
    }
}

public struct TrackCardView: View {
    public let track: Track
    public let allTracks: [Track]
    @ObservedObject var audioEngine = AudioEngine.shared
    @State private var isHovered: Bool = false

    public var body: some View {
        Button(action: {
            audioEngine.playTrack(track, inQueue: allTracks)
        }) {
            VStack(alignment: .leading, spacing: 10) {
                // Artwork Container
                ZStack(alignment: .bottomTrailing) {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color.white.opacity(0.06))

                    if let artUrl = track.highResArtworkUrl, let url = URL(string: artUrl) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                            case .failure(_), .empty:
                                Image(systemName: "music.note")
                                    .font(.system(size: 32))
                                    .foregroundColor(.secondary)
                            @unknown default:
                                EmptyView()
                            }
                        }
                    } else {
                        Image(systemName: "music.note")
                            .font(.system(size: 32))
                            .foregroundColor(.secondary)
                    }

                    // Floating play button on hover
                    if isHovered {
                        ZStack {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 38, height: 38)
                                .shadow(color: Color.black.opacity(0.4), radius: 6, x: 0, y: 3)

                            Image(systemName: "play.fill")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.black)
                                .offset(x: 1)
                        }
                        .padding(8)
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .frame(width: 155, height: 155)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .shadow(color: Color.black.opacity(0.2), radius: 8, x: 0, y: 4)

                // Track Info
                VStack(alignment: .leading, spacing: 3) {
                    Text(track.title)
                        .font(.system(size: 13, weight: .semibold))
                        .lineLimit(1)
                        .foregroundColor(.primary)

                    Text(track.artist)
                        .font(.system(size: 11))
                        .lineLimit(1)
                        .foregroundColor(.secondary)
                }
                .frame(width: 155, alignment: .leading)
            }
            .padding(10)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isHovered ? Color.white.opacity(0.08) : Color.clear)
            )
        }
        .buttonStyle(.plain)
        .onHover { hovering in
            withAnimation(.easeInOut(duration: 0.15)) {
                isHovered = hovering
            }
        }
    }
}
