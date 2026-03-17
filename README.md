# Rentasad FTP & SFTP Utility Library

Eine Java-Bibliothek zur Vereinfachung von FTP- und SFTP-Operationen, einschließlich Datei-Uploads, Downloads, Verzeichnisverwaltung und Archiv-Handling (ZIP/GZIP).

## Features

### FTP (File Transfer Protocol)
- **Verbindungsmanagement**: Einfacher Verbindungsaufbau und Authentifizierung.
- **Dateioperationen**: Upload, Download, Löschen und Umbenennen von Dateien.
- **Verzeichnis-Handling**: Auflisten von Dateien, Prüfen auf Existenz.
- **Monitoring**: `FtpCheckUploadTool` zur Überprüfung von Datei-Uploads basierend auf Konfigurationsparametern.
- **Semaphoren**: Unterstützung für FTP-basierte Semaphoren zur Synchronisation.
- **Alerting**: E-Mail-Benachrichtigungen bei FTP-Statusänderungen (via `FtpStatusEmailAlertProvider`).

### SFTP (SSH File Transfer Protocol)
- **Moderner Wrapper**: Basiert auf JSch, refaktorisiert für Java 17 und moderne APIs.
- **Metadaten**: Abruf detaillierter Dateiinformationen (`FileData`) mit Unterstützung der Java Time API (`Instant`).
- **Archiv-Support**: Integrierte Methoden zum Entpacken von ZIP- und GZIP-Dateien direkt vom/auf dem Server via `ZipUtils`.
- **Pfad-Handling**: Nutzung der `java.nio.file.Path` API für robuste, plattformunabhängige Pfadoperationen.

## Anforderungen
- Java 17 oder höher
- Maven

## Abhängigkeiten
Die Bibliothek nutzt unter anderem folgende Frameworks:
- **Lombok**: Zur Reduzierung von Boilerplate-Code.
- **Commons Net**: Für FTP-Funktionalität.
- **JSch**: Für SFTP-Funktionalität.
- **Apache Commons Email**: Für das Versenden von Status-E-Mails.
- **JUnit 5**: Für automatisierte Tests.

## Installation

Klonen Sie das Repository und installieren Sie die Bibliothek in Ihrem lokalen Maven-Repository:

```bash
mvn clean install
```

Oder nutzen Sie die bereitgestellten Skripte:
- `installWithoutTest.sh` / `installWithoutTest.cmd`

## Nutzung

### SFTP Beispiel
```java
SFtpWrapper sftp = new SFtpWrapper("user", "password", "host", 22);
sftp.uploadFile("local/path/file.txt", "remote/path/file.txt");
List<SFtpWrapper.FileData> files = sftp.getFileDataList("remote/path");
sftp.close();
```

### FTP Beispiel
```java
FTPConnection ftp = new FTPConnection("host", "user", "password");
ftp.connect();
ftp.uploadFile(new File("test.txt"), "test.txt");
ftp.disconnect();
```

## Entwicklung & Tests

Für Integrationstests steht eine `docker-compose.yml` im Verzeichnis `docker/` zur Verfügung, die sowohl einen FTP- als auch einen SFTP-Server bereitstellt.

### Test-Infrastruktur starten
```bash
cd docker
docker-compose up -d
```

### Tests ausführen
```bash
mvn test
```

Die Tests erwarten standardmäßig:
- **FTP**: `127.0.0.1:21` (user/123)
- **SFTP**: `127.0.0.1:2222` (user/123)

## Lizenz
Dieses Projekt steht unter der [MIT Lizenz](LICENSE).

---
*Erstellt von Junie (Autonomous Programmer)*


