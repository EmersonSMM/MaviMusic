import Foundation

public final class LyricsService {
    public static let shared = LyricsService()

    private let session = URLSession.shared
    private var cache: [String: LyricsData] = [:]

    private init() {}

    public func fetchLyrics(for track: Track) async -> LyricsData? {
        if let cached = cache[track.id] {
            return cached
        }

        // Clean title and artist for query (remove feat, official audio, etc.)
        let cleanTitle = cleanTrackTitle(track.title)
        let cleanArtist = cleanArtistName(track.artist)

        guard let encodedTitle = cleanTitle.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let encodedArtist = cleanArtist.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            return nil
        }

        let urlString = "https://lrclib.net/api/get?track_name=\(encodedTitle)&artist_name=\(encodedArtist)"
        guard let url = URL(string: urlString) else { return nil }

        var request = URLRequest(url: url)
        request.setValue("MaviMusic/1.0 (macOS Native)", forHTTPHeaderField: "User-Agent")
        request.timeoutInterval = 8.0

        do {
            let (data, response) = try await session.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                return nil
            }

            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return nil
            }

            if let synced = json["syncedLyrics"] as? String, !synced.isEmpty {
                let parsed = LyricsData.parseLRC(synced, trackId: track.id)
                cache[track.id] = parsed
                return parsed
            } else if let plain = json["plainLyrics"] as? String, !plain.isEmpty {
                let lines = plain.components(separatedBy: .newlines).map {
                    LyricLine(timeSeconds: 0, text: $0)
                }
                let data = LyricsData(trackId: track.id, lines: lines, isSynced: false, plainText: plain)
                cache[track.id] = data
                return data
            }
        } catch {
            return nil
        }

        return nil
    }

    private func cleanTrackTitle(_ title: String) -> String {
        var clean = title
        let patterns = [
            "\\(Official.*\\)",
            "\\[Official.*\\]",
            "\\(Audio.*\\)",
            "\\[Audio.*\\]",
            "\\(Video.*\\)",
            "\\[Video.*\\]",
            "\\(Letra.*\\)",
            "\\[Letra.*\\]",
            "\\(Lyric.*\\)",
            "\\[Lyric.*\\]",
            "\\(Remix.*\\)",
            "\\(ft\\..*?\\)",
            "\\(feat\\..*?\\)"
        ]
        for p in patterns {
            clean = clean.replacingOccurrences(of: p, with: "", options: [.regularExpression, .caseInsensitive])
        }
        return clean.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func cleanArtistName(_ artist: String) -> String {
        return artist.components(separatedBy: ",").first?.trimmingCharacters(in: .whitespaces) ?? artist
    }
}
