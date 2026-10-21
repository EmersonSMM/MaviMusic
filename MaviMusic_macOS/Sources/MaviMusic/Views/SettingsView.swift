import SwiftUI

public struct SettingsView: View {
    @ObservedObject var settings = SettingsManager.shared

    public init() {}

    public var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 28) {
                // Header
                VStack(alignment: .leading, spacing: 4) {
                    Text("Configuración de MaviMusic")
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)

                    Text("Personaliza la apariencia, el sonido y el comportamiento en tu Mac.")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 28)
                .padding(.top, 24)

                VStack(spacing: 20) {
                    // Section: Apariencia & Cristal Glass
                    settingsGroup(title: "APARIENCIA Y CRISTAL GLASS") {
                        VStack(spacing: 16) {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Efecto Cristal Glass (Translucidez Nativa)")
                                        .font(.system(size: 13, weight: .medium))
                                    Text("Ajusta la intensidad de vibración de materiales Liquid Glass en macOS.")
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Slider(value: $settings.glassIntensity, in: 0.3...1.0)
                                    .frame(width: 140)
                                    .accentColor(.white)
                            }

                            Divider().opacity(0.1)

                            Toggle(isOn: $settings.blurArtworkBackground) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Fondo Difuminado Dinámico")
                                        .font(.system(size: 13, weight: .medium))
                                    Text("Usa la carátula de la canción en reproducción como fondo difuso (estilo Apple Music).")
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                }
                            }
                            .toggleStyle(.switch)
                        }
                    }

                    // Section: Calidad de Audio & Descargas
                    settingsGroup(title: "SONIDO Y DESCARGAS") {
                        VStack(spacing: 16) {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Calidad de Transmisión y Descarga")
                                        .font(.system(size: 13, weight: .medium))
                                    Text("Máxima fidelidad en formato MP3 (320 kbps) o AAC.")
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Picker("", selection: $settings.audioQualityKbps) {
                                    Text("320 kbps (Máxima fidelidad)").tag(320)
                                    Text("256 kbps (Alta calidad)").tag(256)
                                    Text("160 kbps (Estándar)").tag(160)
                                }
                                .frame(width: 200)
                            }

                            Divider().opacity(0.1)

                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Carpeta de Descargas")
                                        .font(.system(size: 13, weight: .medium))
                                    Text(settings.downloadDirectory)
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Button("Cambiar...") {
                                    selectDownloadFolder()
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    }

                    // Section: Sistema & Integración
                    settingsGroup(title: "INTEGRACIÓN EN EL SISTEMA") {
                        VStack(spacing: 16) {
                            Toggle(isOn: $settings.launchAtLogin) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Iniciar con el Sistema")
                                        .font(.system(size: 13, weight: .medium))
                                    Text("Abrir MaviMusic automáticamente al iniciar sesión en tu Mac.")
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                }
                            }
                            .toggleStyle(.switch)

                            Divider().opacity(0.1)

                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Idioma de la Aplicación")
                                        .font(.system(size: 13, weight: .medium))
                                    Text("Idioma predeterminado para la interfaz y recomendaciones.")
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Picker("", selection: $settings.defaultLanguage) {
                                    Text("Español (Predeterminado)").tag("es")
                                    Text("English").tag("en")
                                }
                                .frame(width: 190)
                            }
                        }
                    }

                    // Section: Acerca de MaviMusic
                    settingsGroup(title: "ACERCA DE MAVIMUSIC") {
                        HStack(spacing: 14) {
                            Image("MenuBarIcon")
                                .renderingMode(.template)
                                .resizable()
                                .frame(width: 28, height: 28)
                                .foregroundColor(.accentColor)

                            VStack(alignment: .leading, spacing: 2) {
                                Text("MaviMusic para macOS")
                                    .font(.system(size: 13, weight: .semibold))
                                Text("Versión 2.0 Nativa Swift (Sin dependencia de Java) • 120 FPS Metal")
                                    .font(.system(size: 11))
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                        }
                    }
                }
                .padding(.horizontal, 28)
            }
            .padding(.bottom, 40)
        }
    }

    @ViewBuilder
    private func settingsGroup<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(.secondary.opacity(0.7))
                .padding(.horizontal, 4)

            content()
                .padding(16)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color.white.opacity(0.06))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
                        )
                )
        }
    }

    private func selectDownloadFolder() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.allowsMultipleSelection = false
        panel.prompt = "Seleccionar Carpeta"
        if panel.runModal() == .OK, let url = panel.url {
            settings.downloadDirectory = url.path
        }
    }
}
