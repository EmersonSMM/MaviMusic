import Foundation

public struct Track: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public var title: String
    public var artist: String
    public var album: String?
    public var durationSeconds: Double
    public var artworkUrl: String?
    public var isLocal: Bool
    public var localPath: String?
    public var isLiked: Bool

    public init(
        id: String,
        title: String,
        artist: String,
        album: String? = nil,
        durationSeconds: Double = 0,
        artworkUrl: String? = nil,
        isLocal: Bool = false,
        localPath: String? = nil,
        isLiked: Bool = false
    ) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.durationSeconds = durationSeconds
        self.artworkUrl = artworkUrl
        self.isLocal = isLocal
        self.localPath = localPath
        self.isLiked = isLiked
    }

    public var highResArtworkUrl: String? {
        guard let url = artworkUrl else { return nil }
        // Replace small thumbnail dimension parameters with high-res 800x800
        if url.contains("=w") || url.contains("=s") {
            let pattern = "=w\\d+-h\\d+"
            if let regex = try? NSRegularExpression(pattern: pattern) {
                let range = NSRange(url.startIndex..<url.endIndex, in: url)
                return regex.stringByReplacingMatches(in: url, range: range, withTemplate: "=w800-h800")
            }
        }
        return url
    }

    public static func formatSeconds(_ seconds: Double) -> String {
        guard !seconds.isNaN && !seconds.isInfinite && seconds >= 0 else { return "0:00" }
        let total = Int(seconds)
        let mins = total / 60
        let secs = total % 60
        return String(format: "%d:%02d", mins, secs)
    }

    public var formattedDuration: String {
        Self.formatSeconds(durationSeconds)
    }
}

public struct MusicShelf: Identifiable {
    public let id = UUID()
    public let title: String
    public let subtitle: String?
    public let tracks: [Track]

    public init(title: String, subtitle: String? = nil, tracks: [Track]) {
        self.title = title
        self.subtitle = subtitle
        self.tracks = tracks
    }
}

public enum SearchCategory: String, CaseIterable, Identifiable {
    case all = "Todo"
    case songs = "Canciones"
    case albums = "Álbumes"
    case artists = "Artistas"
    case playlists = "Listas de reproducción"

    public var id: String { rawValue }
}
