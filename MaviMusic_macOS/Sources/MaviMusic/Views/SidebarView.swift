import SwiftUI

public enum NavigationTab: String, CaseIterable, Identifiable {
    case home = "Inicio"
    case explore = "Explorar"
    case search = "Buscar"
    case favorites = "Favoritos"
    case history = "Historial"
    case localMusic = "Música Local"
    case downloads = "Descargas"
    case settings = "Ajustes"

    public var id: String { rawValue }

    public var icon: String {
        switch self {
        case .home: return "house.fill"
        case .explore: return "safari.fill"
        case .search: return "magnifyingglass"
        case .favorites: return "heart.fill"
        case .history: return "clock.arrow.circlepath"
        case .localMusic: return "folder.fill"
        case .downloads: return "arrow.down.circle.fill"
        case .settings: return "gearshape.fill"
        }
    }
}

public struct SidebarView: View {
    @Binding var selectedTab: NavigationTab
    @ObservedObject var audioEngine = AudioEngine.shared

    public init(selectedTab: Binding<NavigationTab>) {
        self._selectedTab = selectedTab
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // App Branding Header
            HStack(spacing: 10) {
                Image("MenuBarIcon")
                    .renderingMode(.template)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 22, height: 22)
                    .foregroundColor(.accentColor)

                Text("MaviMusic")
                    .font(.system(size: 18, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)

            // Navigation Sections
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    // Section: Descubrir
                    navSection(title: "DESCUBRIR", items: [.home, .explore, .search])

                    // Section: Mi Biblioteca
                    navSection(title: "MI BIBLIOTECA", items: [.favorites, .history, .localMusic, .downloads])

                    // Section: Sistema
                    navSection(title: "SISTEMA", items: [.settings])
                }
                .padding(.horizontal, 10)
            }

            Spacer()

            // Mini status at bottom of sidebar
            if !audioEngine.favorites.isEmpty {
                HStack(spacing: 6) {
                    Image(systemName: "heart.fill")
                        .font(.system(size: 11))
                        .foregroundColor(.red.opacity(0.8))
                    Text("\(audioEngine.favorites.count) favoritas guardadas")
                        .font(.system(size: 11))
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 12)
            }
        }
        .frame(width: 210)
        .background(
            VisualEffectView(material: .sidebar, blendingMode: .withinWindow)
        )
    }

    @ViewBuilder
    private func navSection(title: String, items: [NavigationTab]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(.secondary.opacity(0.7))
                .padding(.horizontal, 12)
                .padding(.bottom, 2)

            ForEach(items) { tab in
                let isSelected = (selectedTab == tab)
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.15)) {
                        selectedTab = tab
                    }
                }) {
                    HStack(spacing: 10) {
                        Image(systemName: tab.icon)
                            .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
                            .foregroundColor(isSelected ? .accentColor : .secondary)
                            .frame(width: 18)

                        Text(tab.rawValue)
                            .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
                            .foregroundColor(isSelected ? .primary : .secondary)

                        Spacer()
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(isSelected ? Color.white.opacity(0.12) : Color.clear)
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }
}
