# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

RomRaider is a free, open-source tuning suite for viewing, editing, logging, and reflashing
Subaru (and some Mitsubishi/BMW) Engine Control Units. It is a desktop Java Swing application
made of two cooperating apps: the **ECU Editor** (opens ROM images, displays/edits maps) and the
**Logger** (real-time datalogging over OBD/serial/J2534). Java source lives under
`src/main/java/com/romraider`.

## Build / Test (Apache Ant + JDK 8)

The project builds with **Apache Ant** and **must target a 32-bit JDK 8** — RomRaider links
native libraries (J2534, COM4J, Phidget, Java3D) that only load in a 32-bit JVM. Java source/target
is hardcoded to `1.8` in `build.xml`. `ant help` lists all targets.

```
ant build       # compile + build RomRaider.jar for both windows and linux into build/<os>/lib
ant rebuild     # clean + build
ant unittest    # compile everything incl. src/test, then run JUnit tests (haltonfailure=true)
ant all         # rebuild + installer + standalone ZIPs (full release packaging)
ant clean       # delete build/
ant standalone  # build the distributable ZIPs (build/dist/<os>/)
```

- **Build prerequisite:** `prepare` generates `src/main/java/com/romraider/Version.java` from
  `Version.java.template` using tokens in `version.properties`. The generated `Version.java` is
  checked in but is overwritten on every build — **edit the `.template`, never `Version.java`**.
  Likewise `version.properties` is the source of truth for version/URLs/JVM args.
- **Running tests:** there is no per-test Ant target. `ant unittest` runs every class matching
  `**/*Test*` under `src/test`. To run a single test, run JUnit directly, e.g.
  `java -cp <junit + build/linux/lib/RomRaider.jar + lib/common/*:lib/linux/*> org.junit.runner.JUnitCore com.romraider.xml.TableScaleUnmarshallerTest`.
  Note that classes named `Test*` under `src/main/.../io/...` (e.g. `TestJ2534`, `TestSerialConnection`)
  are **manual hardware test harnesses with `main()` methods, not JUnit tests** — real unit tests
  live under `src/test/java`.
- **Docker:** `Dockerfile` builds the canonical i386 Ubuntu 18.04 + OpenJDK-8 build environment
  (published as `romraider/builder`). Use it if you don't have a local 32-bit JDK 8.
- **Building with only a JRE 8 (no `javac`/`tools.jar`)?** Cross-compile with a newer JDK while
  keeping `-source/-target 1.8`: `build.xml`'s bootclasspath already points at JRE 8's `rt.jar`, so
  `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ant build-linux` yields a runnable jar. Dev
  convenience only — use the 32-bit JDK 8 / Docker image for release packaging.
- **Running from source:** entry point is `com.romraider.ECUExec` (pass `-logger` /
  `-logger.fullscreen` / `-logger.touch` to start the Logger instead of the Editor). VS Code launch
  configs are in `.vscode/launch.json`; set `LD_LIBRARY_PATH`/`java.library.path` to `lib/linux/32`
  (or `lib/linux/64` on a 64-bit JVM) so native libs resolve. Running the jar out of `build/` needs
  an explicit `-cp "build/linux/lib/RomRaider.jar:lib/common/*"` (the manifest's relative `Class-Path`
  won't resolve). See `docs/Building_RomRaider_VSCode.md` and `docs/Building_RomRaider.txt`.
- **Logger first-run gotcha:** the Logger won't open its main window until a logger definition file
  is set via `Settings > Logger Definition Location` (`log_defs.xml`); otherwise it only shows
  configuration dialogs.

## Architecture

### Two apps, one process boundary
`ECUExec.main()` is the single entry point for both Editor and Logger. `EditorLoggerCommunication`
uses a localhost socket (port 23272) so a running instance can hand off launch requests rather than
starting a second process — this is why the Editor and Logger feel like one app. `ECUEditorManager`
and the logger expose singleton-style accessors to share the currently open ROM/definition state
between the two.

### Definitions are data, not code
RomRaider knows nothing about specific cars — everything is driven by external XML definition files
(DTDs and samples in `definitions/`):
- **`ecu_defs.xml`** — describes every ROM: its tables (maps), addresses, scalings, axes. Parsed by
  `xml/DOMRomUnmarshaller` (+ `TableScaleUnmarshaller`, `RomAttributeParser`) into the `maps` model.
  Definitions use **inheritance** (`base`/derived ECU IDs) resolved at load time.
- **`log_defs.xml`** — logger parameters/switches/DTCs and how to query them per protocol.
- **`cars_def.xml`**, **`profile.dtd`** — car metadata and saved logger profiles.

Users normally download these from romraider.com; URLs live in `version.properties`. When changing
parsing code, keep it in sync with the DTDs in `definitions/`.

### `maps` package — the ROM data model (Model/View split)
Each table type has a **model class and a paired `*View` class**: `Table`/`TableView`,
`Table1D`/`Table1DView`, `Table2D`/`Table2DView`, `Table3D`/`Table3DView`, `TableSwitch`/`TableSwitchView`,
`TableBitwiseSwitch`/...; cells are `DataCell`/`DataCellView`. Model holds bytes + scaling math;
View is the Swing widget. `Rom` aggregates tables + `RomID`; `Scale` defines unit conversions;
`RomChecksum`/`maps/checksum` validate/repair ROM checksums (vendor-specific algorithms).
`UserLevelException` enforces the user-level (beginner/advanced) gating on editing dangerous tables.

### `xml/ConversionLayer` — importing foreign definition formats
`ConversionLayer` is an abstract SPI (`ConversionLayerFactory` picks the right one by file regex) that
converts third-party definition formats into a RomRaider-compatible DOM before unmarshalling —
e.g. `XDFConversionLayer` (TunerPro XDF), `BMWCodingConversionLayer`. Add a new format by subclassing
`ConversionLayer` and registering it in the factory.

### `io` — ECU communication stack (layered)
Three layers, kept separate:
- **connection** (`io/connection`, `io/serial`, `io/j2534`, `io/elm327`) — physical transport: serial
  (jSerialComm), J2534 pass-through devices (via JNA/COM4J native libs), ELM327.
- **protocol** (`io/protocol/{ssm,ds2,obd,ncs}`) — message framing per ECU protocol family, each with
  transport sub-variants (`iso9141`, `iso14230`, `iso15765`/CAN). SSM is the classic Subaru protocol;
  NCS/OBD/DS2 cover newer Subaru, generic OBD-II, and BMW.
- **ramtune** (`ramtune/test`) — live RAM tuning command executors/generators built on top of the io stack.

### `logger/ecu` — the datalogger
`EcuLogger` is the Logger UI/controller. `logger/ecu/comms` runs the query loop:
`comms/manager` + `comms/controller` drive polling; `comms/query` builds/batches parameter reads;
`comms/learning` reads ECU learning tables (AF learning, knock), `comms/reset`, `comms/readcodes`,
`comms/globaladjust` implement those ECU operations. `logger/ecu/definition` holds the in-memory logger
param model; `logger/ecu/profile` saves/loads which params are active; `logger/ecu/ui` is the gauge/graph UI.

### `logger/external` — third-party sensor/gauge plugins
Each external device (AEM, Innovate, PLX, Phidget, Ecotrons, etc.) is a self-contained plugin under its
own folder, conventionally split into an `io/` package (device connection: `*Connector`, `*Manager`,
`*Runner`, `*Sensor`) and a `plugin/` package (logger integration: `*DataSource`, `*DataItem`,
`*TableModel`, `*ConvertorPanel`, `*PluginMenuAction`). The contract is the SPI in
`logger/external/core` (`ExternalDataSource`, `ExternalDataItem`, loaded via `ExternalDataSourceLoader`).
**To add a logging device, copy an existing plugin folder** (e.g. `phidget/vnt`) and implement these
interfaces — no core changes needed beyond the loader picking it up.

### `swing`, `util`, `net`
`swing` — shared Swing components, look-and-feel (`LookAndFeelManager`), dialogs. `util` — settings
persistence (`SettingsManager`, serialized via `xml/DOMSettings*`), logging setup (`LogManager`, log4j),
i18n (`ResourceUtil`). `net` — update checks / network helpers.

### i18n
All user-facing strings are externalized to `ResourceBundle` `.properties` files under **`i18n/`**,
mirroring the package path of the class that uses them (e.g. `i18n/com/romraider/maps/Rom.properties`,
`_fr_CA` for locale variants). When adding UI text, add the key to the matching properties file rather
than hardcoding the string; load it via `ResourceUtil().getBundle(<class>.getName())`.

## Conventions

- **Logging:** log4j (`org.apache.log4j.Logger`), configured by `lib/log4j.properties`; init via
  `LogManager.initDebugLogging()`. Runtime logs go to `$HOME/.RomRaider/`.
- **License header:** every source file starts with the GPL-2.0 header block (`Copyright (C) 2006-<year>
  RomRaider.com`). Preserve it on new files.
- **Native-lib bitness:** any code touching J2534/COM4J/Phidget/Java3D assumes a 32-bit JVM and
  `java.library.path` pointing at `lib/<os>/32`. Don't introduce 64-bit-only assumptions.
- Dependencies are **vendored jars** in `lib/common`, `lib/windows`, `lib/linux` (no Maven/Gradle);
  the classpath is assembled in `build.xml` and `.classpath` (Eclipse). Add a dependency by dropping the
  jar in the right `lib/` folder and registering it in both.
