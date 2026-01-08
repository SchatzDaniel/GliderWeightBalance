# Weight & Balance Calculator

[![Release](https://img.shields.io/github/v/release/SchatzDaniel/GliderWeightBalance?include_prereleases&label=Version&color=blue)](https://github.com/SchatzDaniel/GliderWeightBalance/releases/latest)

Eine Android-App zur einfachen Berechnung und Verwaltung von Gewicht und Schwerpunkt (Weight & Balance) für eine Flotte von Segelflugzeugen. Diese App wurde als Übungsprojekt entwickelt, um moderne Android-Entwicklungspraktiken anzuwenden.

![Screenshot der App](https://raw.githubusercontent.com/SchatzDaniel/GliderWeightBalance/main/screenshots/app_screenshot_1.jpg)

---

## ⚠️ Haftungsausschluss (Disclaimer)

Diese App dient ausschließlich als Hilfsmittel zur Unterstützung von Massen- und Schwerpunktberechnungen.
Die Ergebnisse ersetzen nicht die Berechnung und Angaben im offiziellen Flughandbuch des jeweiligen Luftfahrzeugs.
Der Nutzer ist selbst verantwortlich für die korrekte Eingabe der Daten sowie für die Überprüfung der Ergebnisse anhand der originalen Flugzeugdokumentation.
Der Entwickler übernimmt keine Haftung für Schäden, die aus der Nutzung dieser App entstehen.
---

## ✨ Features

*   **Flotten-Management:** Lege eine ganze Flotte von Flugzeugen an und verwalte sie.
*   **Persistente Datenspeicherung:** Alle Flugzeug-Stammdaten werden in einer lokalen Datenbank gespeichert und bleiben erhalten.
*   **Dynamische Berechnung:** Gib die aktuelle Beladung (Pilot, Passagier, Wasserballast etc.) ein und erhalte eine sofortige Live-Berechnung der Gesamtmasse und des Schwerpunkts.
*   **Anpassbare Flugzeug-Parameter:** Definiere für jedes Flugzeug individuelle Stammdaten, inklusive minimaler und maximaler Schwerpunktgrenzen und der spezifischen Hebelarme.
*   **Moderne & intuitive UI:** Eine klare, aufgeräumte Benutzeroberfläche basierend auf Material Design 3.
*   **Einfache Bedienung:** Flugzeuge können durch langes gedrückthalten gelöscht werden.

---

## 🛠️ Technische Umsetzung & Architektur

Dieses Projekt wurde mit einem Fokus auf eine saubere, skalierbare und wartbare Architektur entwickelt.

*   **Sprache:** [Kotlin](https://kotlinlang.org/)
*   **Architektur:** MVVM (Model-View-ViewModel)
*   **UI:** XML-Layouts mit [Material Design 3](https://m3.material.io/) Komponenten.
*   **Asynchrone Programmierung:** Kotlin Coroutines für nebenläufige Operationen.
*   **Datenpersistenz:** [Room Persistence Library](https://developer.android.com/training/data-storage/room) als Abstraktion über SQLite.
*   **Navigation:** [Android Navigation Component](https://developer.android.com/guide/navigation) zur Steuerung der App-Flows.
*   **UI-Komponenten:** `RecyclerView` zur effizienten Darstellung von Listen (Flugzeugflotte, Hebelarme).
*   **Dependency Management:** [Gradle](https://gradle.org/)

---

## 🚀 Erste Schritte

1.  Klone dieses Repository:
    ```bash git clone https://github.com/SchatzDaniel/GliderWeightBalance.git```
2.  Öffne das Projekt in [Android Studio](https://developer.android.com/studio).
3.  Lasse Gradle die Abhängigkeiten synchronisieren.
4.  Führe die App auf einem Emulator oder einem physischen Gerät aus.

---

## 📄 Lizenz

Dieses Projekt steht unter der [MIT-Lizenz](https://github.com/SchatzDaniel/GliderWeightBalance/blob/main/LICENSE).

