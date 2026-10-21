import SwiftUI

public struct SearchView: View {
    @ObservedObject var audioEngine = AudioEngine.shared
    @State private var searchQuery: String = ""
    @State private var selectedCategory: SearchCategory = .all
    @State private var searchResults: [Track] = []
    @State private var suggestions: [String] = []
    @State private var isSearching: Bool = false
    @State private var searchTask: Task<Void, Never>? = nil

    public init() {}

    private let columns = [
        GridItem(.adaptive(minimum: 155, maximum: 210), spacing: 16)
    ]

    public var body: some View {
        VStack(spacing: 0) {
            // Search Input Header
            VStack(spacing: 14) {
                HStack(spacing: 12) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)

                    TextField("Buscar canciones, artistas, podcasts o álbumes...", text: $searchQuery)
                        .textFieldStyle(.plain)
                        .font(.system(size: 15))
                        .onChange(of: searchQuery) { newValue in
                            onQueryChanged(newValue)
                        }
                        .onSubmit {
                            performSearch()
                        }

                    if !searchQuery.isEmpty {
                        Button(action: {
                            searchQuery = ""
                            searchResults = []
                            suggestions = []
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)
                        }
                        .buttonStyle(.plain)
                    }

                    if isSearching {
                        ProgressView()
                            .scaleEffect(0.7)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color.white.opacity(0.08))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(Color.white.opacity(0.15), lineWidth: 1)
                        )
                )

                // Category Chips
                HStack(spacing: 8) {
                    ForEach(SearchCategory.allCases) { cat in
                        let isSelected = (selectedCategory == cat)
                        Button(action: {
                            selectedCategory = cat
                            if !searchQuery.isEmpty {
                                performSearch()
                            }
                        }) {
                            Text(cat.rawValue)
                                .font(.system(size: 12, weight: isSelected ? .semibold : .regular))
                                .foregroundColor(isSelected ? .white : .secondary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 6)
                                .background(
                                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                                        .fill(isSelected ? Color.accentColor : Color.white.opacity(0.08))
                                )
                        }
                        .buttonStyle(.plain)
                    }
                    Spacer()
                }
            }
            .padding(.horizontal, 28)
            .padding(.top, 20)
            .padding(.bottom, 16)

            Divider()
                .opacity(0.15)

            // Results Content
            ScrollView(showsIndicators: false) {
                if !suggestions.isEmpty && searchResults.isEmpty && !searchQuery.isEmpty {
                    // Typeahead suggestions
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Sugerencias de búsqueda")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 28)
                            .padding(.top, 16)

                        ForEach(suggestions, id: \.self) { sug in
                            Button(action: {
                                searchQuery = sug
                                suggestions = []
                                performSearch()
                            }) {
                                HStack(spacing: 12) {
                                    Image(systemName: "magnifyingglass")
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                    Text(sug)
                                        .font(.system(size: 14))
                                        .foregroundColor(.primary)
                                    Spacer()
                                }
                                .padding(.horizontal, 28)
                                .padding(.vertical, 10)
                                .background(Color.white.opacity(0.03))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                } else if !searchResults.isEmpty {
                    LazyVGrid(columns: columns, spacing: 20) {
                        ForEach(searchResults) { track in
                            TrackCardView(track: track, allTracks: searchResults)
                        }
                    }
                    .padding(28)
                } else if !searchQuery.isEmpty && !isSearching {
                    VStack(spacing: 12) {
                        Spacer().frame(height: 80)
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 44))
                            .foregroundColor(.secondary.opacity(0.5))
                        Text("No se encontraron resultados para \"\(searchQuery)\"")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    // Empty state / prompts
                    VStack(spacing: 14) {
                        Spacer().frame(height: 90)
                        Image(systemName: "music.note.magnifyingglass")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary.opacity(0.5))
                        Text("Encuentra lo que deseas escuchar")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.primary)
                        Text("Escribe un tema, artista, género o álbum para comenzar.")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func onQueryChanged(_ query: String) {
        searchTask?.cancel()
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            suggestions = []
            searchResults = []
            return
        }

        searchTask = Task {
            try? await Task.sleep(nanoseconds: 200_000_000) // 200ms debounce
            guard !Task.isCancelled else { return }
            let sugs = await YouTubeMusicClient.shared.getSearchSuggestions(query: query)
            DispatchQueue.main.async {
                self.suggestions = sugs
            }
        }
    }

    private func performSearch() {
        searchTask?.cancel()
        suggestions = []
        guard !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty else { return }

        isSearching = true
        Task {
            do {
                let results = try await YouTubeMusicClient.shared.search(query: searchQuery, category: selectedCategory)
                DispatchQueue.main.async {
                    self.searchResults = results
                    self.isSearching = false
                }
            } catch {
                DispatchQueue.main.async {
                    self.isSearching = false
                }
            }
        }
    }
}
