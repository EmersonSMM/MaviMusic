import Foundation

public struct LyricLine: Identifiable, Equatable {
    public let id = UUID()
    public let timeSeconds: Double
    public let text: String

    public init(timeSeconds: Double, text: String) {
        self.timeSeconds = timeSeconds
        self.text = text
    }
}

public struct LyricsData: Equatable {
    public let trackId: String
    public let lines: [LyricLine]
    public let isSynced: Bool
    public let plainText: String

    public init(trackId: String, lines: [LyricLine], isSynced: Bool = true, plainText: String = "") {
        self.trackId = trackId
        self.lines = lines
        self.isSynced = isSynced
        self.plainText = plainText
    }

    public static func parseLRC(_ lrcContent: String, trackId: String) -> LyricsData {
        var parsedLines: [LyricLine] = []
        let pattern = "\\[(\\d{2}):(\\d{2})(?:[\\.:](\\d{2,3}))?\\](.*)"
        guard let regex = try? NSRegularExpression(pattern: pattern) else {
            return LyricsData(trackId: trackId, lines: [], isSynced: false, plainText: lrcContent)
        }

        let lines = lrcContent.components(separatedBy: .newlines)
        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            guard !trimmed.isEmpty else { continue }

            let range = NSRange(trimmed.startIndex..<trimmed.endIndex, in: trimmed)
            if let match = regex.firstMatch(in: trimmed, range: range) {
                if let minRange = Range(match.range(at: 1), in: trimmed),
                   let secRange = Range(match.range(at: 2), in: trimmed),
                   let textRange = Range(match.range(at: 4), in: trimmed) {
                    let min = Double(trimmed[minRange]) ?? 0
                    let sec = Double(trimmed[secRange]) ?? 0
                    var frac = 0.0
                    if match.range(at: 3).location != NSNotFound,
                       let fracRange = Range(match.range(at: 3), in: trimmed) {
                        let fracStr = String(trimmed[fracRange])
                        let divisor = pow(10.0, Double(fracStr.count))
                        frac = (Double(fracStr) ?? 0) / divisor
                    }
                    let totalSeconds = (min * 60.0) + sec + frac
                    let text = String(trimmed[textRange]).trimmingCharacters(in: .whitespaces)
                    parsedLines.append(LyricLine(timeSeconds: totalSeconds, text: text))
                }
            }
        }

        parsedLines.sort { $0.timeSeconds < $1.timeSeconds }
        let isSynced = !parsedLines.isEmpty
        return LyricsData(
            trackId: trackId,
            lines: parsedLines,
            isSynced: isSynced,
            plainText: lrcContent
        )
    }
}
