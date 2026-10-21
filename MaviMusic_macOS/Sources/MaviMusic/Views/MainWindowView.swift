import SwiftUI

public struct MainWindowView: View {
    @State private var selectedTab: NavigationTab = .home
    @State private var showLyrics: Bool = false
    @ObservedObject var audioEngine = AudioEngine.shared
    @ObservedObject var settings = SettingsManager.shared

    public init() {}

    public var body: some View {
        ZStack {
            // Background Layer: Dynamic Blurred Artwork Backdrop (Apple Music style)
            if settings.blurArtworkBackground,
               let track = audioEngine.currentTrack,
               let artUrl = track.highResArtworkUrl,
               let url = URL(string: artUrl) {
                AsyncImage(url: url) { phase in
                    if case .success(let img) = phase {
                        img
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .blur(radius: 80)
                            .opacity(settings.glassIntensity * 0.45)
                            .overlay(Color.black.opacity(0.4))
                            .ignoresSafeArea()
                    }
                }
            }

            // Window Glass Vibrancy Base
            VisualEffectView(material: .underWindowBackground, blendingMode: .behindWindow)
                .ignoresSafeArea()

            // Main Split Navigation
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    // Sidebar
                    SidebarView(selectedTab: $selectedTab)

                    Divider().opacity(0.12)

                    // Page Content
                    ZStack {
                        switch selectedTab {
                        case .home, .explore:
                            HomeView()
                        case .search:
                            SearchView()
                        case .favorites:
                            LibraryListView(
                                title: "Canciones Favoritas",
                                subtitle: "\(audioEngine.favorites.count) canciones marcadas con me gusta",
                                icon: "heart.fill",
                                tracks: audioEngine.favorites,
                                emptyMessage: "Aún no tienes canciones favoritas. Haz clic en el corazón de cualquier canción para añadirla aquí."
                            )
                        case .history:
                            LibraryListView(
                                title: "Historial de Reproducción",
                                subtitle: "Tus canciones escuchadas recientemente",
                                icon: "clock.arrow.circlepath",
                                tracks: audioEngine.history,
                                emptyMessage: "Tu historial está vacío. Las canciones que reproduzcas aparecerán aquí automáticamente."
                            )
                        case .localMusic, .downloads:
                            LibraryListView(
                                title: selectedTab == .downloads ? "Música Descargada" : "Archivos Locales",
                                subtitle: "\(audioEngine.localLibrary.count) pistas encontradas en tu Mac",
                                icon: selectedTab == .downloads ? "arrow.down.circle.fill" : "folder.fill",
                                tracks: audioEngine.localLibrary,
                                emptyMessage: "No se encontraron archivos de música locales (.mp3, .m4a, .flac) en tus carpetas Música o Descargas."
                            )
                        case .settings:
                            SettingsView()
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }

                // Persistent Bottom Player Bar
                PlayerBarView(showLyrics: $showLyrics)
            }

            // Full-Window Apple Music Lyrics Overlay
            if showLyrics {
                LyricsView(isPresented: $showLyrics)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .zIndex(100)
            }
        }
        .frame(minWidth: 920, minHeight: 600)
    }
}
