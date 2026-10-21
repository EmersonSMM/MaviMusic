import Foundation
import Combine

public final class SettingsManager: ObservableObject {
    public static let shared = SettingsManager()

    @Published public var glassIntensity: Double {
        didSet { UserDefaults.standard.set(glassIntensity, forKey: "glassIntensity") }
    }
    @Published public var blurArtworkBackground: Bool {
        didSet { UserDefaults.standard.set(blurArtworkBackground, forKey: "blurArtworkBackground") }
    }
    @Published public var audioQualityKbps: Int {
        didSet { UserDefaults.standard.set(audioQualityKbps, forKey: "audioQualityKbps") }
    }
    @Published public var launchAtLogin: Bool {
        didSet {
            UserDefaults.standard.set(launchAtLogin, forKey: "launchAtLogin")
            AutoStartManager.setLaunchAtLogin(launchAtLogin)
        }
    }
    @Published public var downloadDirectory: String {
        didSet { UserDefaults.standard.set(downloadDirectory, forKey: "downloadDirectory") }
    }
    @Published public var defaultLanguage: String {
        didSet { UserDefaults.standard.set(defaultLanguage, forKey: "defaultLanguage") }
    }

    private init() {
        self.glassIntensity = UserDefaults.standard.object(forKey: "glassIntensity") as? Double ?? 0.85
        self.blurArtworkBackground = UserDefaults.standard.object(forKey: "blurArtworkBackground") as? Bool ?? true
        self.audioQualityKbps = UserDefaults.standard.object(forKey: "audioQualityKbps") as? Int ?? 320
        self.launchAtLogin = UserDefaults.standard.bool(forKey: "launchAtLogin")
        
        let defaultMusicFolder = (FileManager.default.urls(for: .musicDirectory, in: .userDomainMask).first?.appendingPathComponent("MaviMusic").path) ?? "~/Music/MaviMusic"
        self.downloadDirectory = UserDefaults.standard.string(forKey: "downloadDirectory") ?? defaultMusicFolder
        self.defaultLanguage = UserDefaults.standard.string(forKey: "defaultLanguage") ?? "es"
    }
}

public final class AutoStartManager {
    private static let label = "com.music.mavimusic"

    public static func setLaunchAtLogin(_ enabled: Bool) {
        let launchAgentDir = FileManager.default.homeDirectoryForCurrentUser.appendingPathComponent("Library/LaunchAgents")
        let plistUrl = launchAgentDir.appendingPathComponent("\(label).plist")

        if enabled {
            try? FileManager.default.createDirectory(at: launchAgentDir, withIntermediateDirectories: true)
            let bundleUrl = Bundle.main.bundleURL
            let plistContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>\(label)</string>
                <key>ProgramArguments</key>
                <array>
                    <string>/usr/bin/open</string>
                    <string>\(bundleUrl.path)</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
            </dict>
            </plist>
            """
            try? plistContent.write(to: plistUrl, atomically: true, encoding: .utf8)
        } else {
            try? FileManager.default.removeItem(at: plistUrl)
        }
    }
}
