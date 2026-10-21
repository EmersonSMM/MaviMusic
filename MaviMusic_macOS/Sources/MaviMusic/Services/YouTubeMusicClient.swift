import Foundation

public final class YouTubeMusicClient {
    public static let shared = YouTubeMusicClient()

    private let session: URLSession
    private let baseUrl = "https://music.youtube.com/youtubei/v1"
    private let userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15.0
        config.timeoutIntervalForResource = 30.0
        self.session = URLSession(configuration: config)
    }

    private func createBasePayload() -> [String: Any] {
        return [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20250101.01.00",
                    "hl": "es",
                    "gl": "ES"
                ]
            ]
        ]
    }

    private func post(endpoint: String, body: [String: Any]) async throws -> [String: Any] {
        guard let url = URL(string: "\(baseUrl)/\(endpoint)") else {
            throw URLError(.badURL)
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")
        request.setValue("https://music.youtube.com/", forHTTPHeaderField: "Referer")
        request.setValue("es-ES,es;q=0.9", forHTTPHeaderField: "Accept-Language")

        let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
        request.httpBody = jsonData

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw URLError(.badServerResponse)
        }

        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw URLError(.cannotParseResponse)
        }
        return json
    }

    // MARK: - Search
    public func search(query: String, category: SearchCategory = .all) async throws -> [Track] {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else { return [] }

        var body = createBasePayload()
        body["query"] = query

        // Apply Innertube filter params for specific categories if selected
        switch category {
        case .songs:
            body["params"] = "Eg-KAQwIARAAGAAgACgAMABqChAEEAMQCRAFEAo%3D"
        case .albums:
            body["params"] = "Eg-KAQwBAhABGAEgASgBMAhqChAEEAMQCRAFEAo%3D"
        case .artists:
            body["params"] = "Eg-KAQwBAhABGAEgASgBMAhqChAEEAMQCRAFEAo%3D"
        default:
            break
        }

        let json = try await post(endpoint: "search", body: body)
        return extractTracks(from: json)
    }

    // MARK: - Search Suggestions
    public func getSearchSuggestions(query: String) async -> [String] {
        guard !query.isEmpty else { return [] }
        var body = createBasePayload()
        body["input"] = query

        do {
            let json = try await post(endpoint: "music/get_search_suggestions", body: body)
            var results: [String] = []
            if let contents = json["contents"] as? [[String: Any]] {
                for item in contents {
                    if let renderer = item["searchSuggestionRenderer"] as? [String: Any],
                       let suggestion = renderer["suggestion"] as? [String: Any],
                       let runs = suggestion["runs"] as? [[String: Any]] {
                        let text = runs.compactMap { $0["text"] as? String }.joined()
                        if !text.isEmpty {
                            results.append(text)
                        }
                    }
                }
            }
            return results
        } catch {
            return []
        }
    }

    // MARK: - Home Feed
    public func getHomeFeed() async throws -> [MusicShelf] {
        var body = createBasePayload()
        body["browseId"] = "FEmusic_home"

        let json = try await post(endpoint: "browse", body: body)
        return extractShelves(from: json)
    }

    // MARK: - Explore / Trending Feed
    public func getExploreFeed() async throws -> [MusicShelf] {
        var body = createBasePayload()
        body["browseId"] = "FEmusic_explore"

        let json = try await post(endpoint: "browse", body: body)
        return extractShelves(from: json)
    }

    // MARK: - Parsers
    private func extractTracks(from json: [String: Any]) -> [Track] {
        var tracks: [Track] = []
        var seenIds = Set<String>()

        func scan(object: Any) {
            if let dict = object as? [String: Any] {
                if let renderer = dict["musicResponsiveListItemRenderer"] as? [String: Any] {
                    if let track = parseResponsiveListItem(renderer), !seenIds.contains(track.id) {
                        seenIds.insert(track.id)
                        tracks.append(track)
                    }
                } else if let renderer = dict["musicTwoRowItemRenderer"] as? [String: Any] {
                    if let track = parseTwoRowItem(renderer), !seenIds.contains(track.id) {
                        seenIds.insert(track.id)
                        tracks.append(track)
                    }
                }
                for (_, val) in dict {
                    scan(object: val)
                }
            } else if let array = object as? [Any] {
                for item in array {
                    scan(object: item)
                }
            }
        }

        scan(object: json)
        return tracks
    }

    private func extractShelves(from json: [String: Any]) -> [MusicShelf] {
        var shelves: [MusicShelf] = []

        func scanShelves(object: Any) {
            if let dict = object as? [String: Any] {
                if let shelf = dict["musicCarouselShelfRenderer"] as? [String: Any] {
                    var title = "Música Recomendada"
                    var subtitle: String? = nil

                    if let header = shelf["header"] as? [String: Any],
                       let basicHeader = header["musicCarouselShelfBasicHeaderRenderer"] as? [String: Any] {
                        if let titleRuns = (basicHeader["title"] as? [String: Any])?["runs"] as? [[String: Any]] {
                            title = titleRuns.compactMap { $0["text"] as? String }.joined()
                        }
                        if let straplineRuns = (basicHeader["strapline"] as? [String: Any])?["runs"] as? [[String: Any]] {
                            subtitle = straplineRuns.compactMap { $0["text"] as? String }.joined()
                        }
                    }

                    var shelfTracks: [Track] = []
                    if let contents = shelf["contents"] as? [[String: Any]] {
                        for item in contents {
                            if let twoRow = item["musicTwoRowItemRenderer"] as? [String: Any],
                               let track = parseTwoRowItem(twoRow) {
                                shelfTracks.append(track)
                            } else if let responsive = item["musicResponsiveListItemRenderer"] as? [String: Any],
                                      let track = parseResponsiveListItem(responsive) {
                                shelfTracks.append(track)
                            }
                        }
                    }

                    if !shelfTracks.isEmpty {
                        shelves.append(MusicShelf(title: title, subtitle: subtitle, tracks: shelfTracks))
                    }
                }
                for (_, val) in dict {
                    scanShelves(object: val)
                }
            } else if let array = object as? [Any] {
                for item in array {
                    scanShelves(object: item)
                }
            }
        }

        scanShelves(object: json)
        return shelves
    }

    private func parseResponsiveListItem(_ renderer: [String: Any]) -> Track? {
        guard let flexColumns = renderer["flexColumns"] as? [[String: Any]],
              flexColumns.count >= 2 else { return nil }

        // Video ID
        var videoId: String? = nil
        if let playlistItemData = renderer["playlistItemData"] as? [String: Any],
           let vid = playlistItemData["videoId"] as? String {
            videoId = vid
        } else if let overlay = renderer["overlay"] as? [String: Any],
                  let thumbOverlay = overlay["musicItemThumbnailOverlayRenderer"] as? [String: Any],
                  let content = thumbOverlay["content"] as? [String: Any],
                  let playBtn = content["musicPlayButtonRenderer"] as? [String: Any],
                  let navEndpoint = playBtn["playNavigationEndpoint"] as? [String: Any],
                  let watchEndpoint = navEndpoint["watchEndpoint"] as? [String: Any],
                  let vid = watchEndpoint["videoId"] as? String {
            videoId = vid
        }

        guard let id = videoId, !id.isEmpty else { return nil }

        // Title
        let titleRuns = ((flexColumns[0]["musicResponsiveListItemFlexColumnRenderer"] as? [String: Any])?["text"] as? [String: Any])?["runs"] as? [[String: Any]] ?? []
        let title = titleRuns.compactMap { $0["text"] as? String }.joined()
        guard !title.isEmpty else { return nil }

        // Artist & Album
        let artistRuns = ((flexColumns[1]["musicResponsiveListItemFlexColumnRenderer"] as? [String: Any])?["text"] as? [String: Any])?["runs"] as? [[String: Any]] ?? []
        let artistText = artistRuns.compactMap { $0["text"] as? String }.joined(separator: " ")
        let parts = artistText.components(separatedBy: "•").map { $0.trimmingCharacters(in: .whitespaces) }
        let artist = parts.first ?? "Artista Desconocido"
        let album = parts.count > 1 ? parts[1] : nil

        // Thumbnail
        var artworkUrl: String? = nil
        if let thumbDict = renderer["thumbnail"] as? [String: Any],
           let thumbRenderer = thumbDict["musicThumbnailRenderer"] as? [String: Any],
           let thumbInner = thumbRenderer["thumbnail"] as? [String: Any],
           let thumbs = thumbInner["thumbnails"] as? [[String: Any]],
           let lastThumb = thumbs.last,
           let url = lastThumb["url"] as? String {
            artworkUrl = url
        }

        return Track(
            id: id,
            title: title,
            artist: artist,
            album: album,
            artworkUrl: artworkUrl
        )
    }

    private func parseTwoRowItem(_ renderer: [String: Any]) -> Track? {
        var videoId: String? = nil
        if let navEndpoint = renderer["navigationEndpoint"] as? [String: Any],
           let watchEndpoint = navEndpoint["watchEndpoint"] as? [String: Any],
           let vid = watchEndpoint["videoId"] as? String {
            videoId = vid
        } else if let overlay = renderer["thumbnailOverlay"] as? [String: Any],
                  let thumbOverlay = overlay["musicItemThumbnailOverlayRenderer"] as? [String: Any],
                  let content = thumbOverlay["content"] as? [String: Any],
                  let playBtn = content["musicPlayButtonRenderer"] as? [String: Any],
                  let navEndpoint = playBtn["playNavigationEndpoint"] as? [String: Any],
                  let watchEndpoint = navEndpoint["watchEndpoint"] as? [String: Any],
                  let vid = watchEndpoint["videoId"] as? String {
            videoId = vid
        }

        guard let id = videoId, !id.isEmpty else { return nil }

        let titleRuns = ((renderer["title"] as? [String: Any])?["runs"] as? [[String: Any]]) ?? []
        let title = titleRuns.compactMap { $0["text"] as? String }.joined()
        guard !title.isEmpty else { return nil }

        let subtitleRuns = ((renderer["subtitle"] as? [String: Any])?["runs"] as? [[String: Any]]) ?? []
        let artist = subtitleRuns.compactMap { $0["text"] as? String }.joined()

        var artworkUrl: String? = nil
        if let thumbDict = renderer["thumbnailRenderer"] as? [String: Any],
           let thumbInner = thumbDict["musicThumbnailRenderer"] as? [String: Any],
           let thumbObj = thumbInner["thumbnail"] as? [String: Any],
           let thumbs = thumbObj["thumbnails"] as? [[String: Any]],
           let lastThumb = thumbs.last,
           let url = lastThumb["url"] as? String {
            artworkUrl = url
        }

        return Track(
            id: id,
            title: title,
            artist: artist.isEmpty ? "Artista" : artist,
            artworkUrl: artworkUrl
        )
    }
}
