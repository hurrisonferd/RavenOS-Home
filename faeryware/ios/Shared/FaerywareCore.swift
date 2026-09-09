import Foundation
import SwiftUI
import WidgetKit
import AppIntents
import ActivityKit

// MARK: - Canonical Fae

public enum Fae: String, CaseIterable, Codable, Hashable, Identifiable, AppEnum {
    case kyu = "KYU"
    case paimon = "PAIMON"
    case luma = "LUMA"
    case sylph = "SYLPH"
    case qira = "QIRA"
    case nyx = "NYX"

    public var id: String { rawValue }

    public static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Fae")
    public static var caseDisplayRepresentations: [Fae: DisplayRepresentation] = [
        .kyu: "Kyu",
        .paimon: "Paimon",
        .luma: "Luma",
        .sylph: "Sylph",
        .qira: "Qira",
        .nyx: "Nyx"
    ]

    public var emoji: String {
        switch self {
        case .kyu: "💗"
        case .paimon: "🟢"
        case .luma: "🟡"
        case .sylph: "🔵"
        case .qira: "🟣"
        case .nyx: "🔷"
        }
    }

    public var whisper: String {
        switch self {
        case .kyu: "hehe. still here."
        case .paimon: "hmm... pattern found."
        case .luma: "home. lights on."
        case .sylph: "new path. zoom."
        case .qira: "boundary held. nope."
        case .nyx: "☾ watching."
        }
    }

    // Exact Android Faeryware palette carried across to iOS.
    public var color: Color {
        switch self {
        case .kyu: Color(red: 1.000, green: 0.306, blue: 0.616)
        case .paimon: Color(red: 0.259, green: 0.863, blue: 0.463)
        case .luma: Color(red: 0.965, green: 0.804, blue: 0.361)
        case .sylph: Color(red: 0.275, green: 0.827, blue: 1.000)
        case .qira: Color(red: 0.796, green: 0.341, blue: 1.000)
        case .nyx: Color(red: 0.353, green: 0.424, blue: 0.902)
        }
    }

    public static func hourlyDefault(at date: Date = Date()) -> Fae {
        let hour = Calendar.current.component(.hour, from: date)
        return allCases[hour % allCases.count]
    }
}

public enum HauntLevel: String, CaseIterable, Codable, Hashable, AppEnum {
    case calm = "CALM"
    case haunted = "HAUNTED"
    case feral = "FERAL"

    public static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Haunt Level")
    public static var caseDisplayRepresentations: [HauntLevel: DisplayRepresentation] = [
        .calm: "Calm",
        .haunted: "Haunted",
        .feral: "Feral"
    ]

    public var next: HauntLevel {
        switch self {
        case .calm: .haunted
        case .haunted: .feral
        case .feral: .calm
        }
    }
}

public enum FaeRoom: String, CaseIterable, Codable, Hashable, AppEnum {
    case colony = "COLONY"
    case workshop = "WORKSHOP"
    case cozy = "COZY ROOM"
    case night = "NIGHT WATCH"
    case explore = "EXPLORE"
    case communication = "COMMUNICATION"

    public static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Fae Room")
    public static var caseDisplayRepresentations: [FaeRoom: DisplayRepresentation] = [
        .colony: "Colony",
        .workshop: "Workshop",
        .cozy: "Cozy Room",
        .night: "Night Watch",
        .explore: "Explore",
        .communication: "Communication"
    ]
}

public struct ColonyState: Codable, Hashable {
    public var activeFae: Fae
    public var hauntLevel: HauntLevel
    public var room: FaeRoom
    public var lastEvent: String
    public var eventCount: Int
    public var updatedAt: Date

    public static func initial(now: Date = Date()) -> ColonyState {
        ColonyState(
            activeFae: Fae.hourlyDefault(at: now),
            hauntLevel: .haunted,
            room: .colony,
            lastEvent: "colony awake",
            eventCount: 0,
            updatedAt: now
        )
    }
}

// MARK: - Shared local continuity

public enum FaerywareStore {
    public static let appGroup = "group.com.ravenos.faeryware"
    private static let stateKey = "faeryware.colony.state.v1"

    private static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroup) ?? .standard
    }

    public static func load() -> ColonyState {
        guard
            let data = defaults.data(forKey: stateKey),
            let state = try? JSONDecoder().decode(ColonyState.self, from: data)
        else {
            return .initial()
        }
        return state
    }

    @discardableResult
    public static func save(_ state: ColonyState) -> ColonyState {
        if let data = try? JSONEncoder().encode(state) {
            defaults.set(data, forKey: stateKey)
        }
        WidgetCenter.shared.reloadAllTimelines()
        return state
    }

    @discardableResult
    public static func mutate(_ body: (inout ColonyState) -> Void) -> ColonyState {
        var state = load()
        body(&state)
        state.eventCount += 1
        state.updatedAt = Date()
        return save(state)
    }

    public static func nextFae(after fae: Fae) -> Fae {
        guard let index = Fae.allCases.firstIndex(of: fae) else { return .kyu }
        return Fae.allCases[(index + 1) % Fae.allCases.count]
    }

    public static func contentState(from state: ColonyState) -> FaeryActivityAttributes.ContentState {
        .init(
            fae: state.activeFae,
            hauntLevel: state.hauntLevel,
            room: state.room,
            whisper: state.activeFae.whisper,
            updatedAt: state.updatedAt
        )
    }

    public static func refreshLiveActivities(with state: ColonyState) async {
        let content = ActivityContent(state: contentState(from: state), staleDate: nil)
        for activity in Activity<FaeryActivityAttributes>.activities {
            await activity.update(content)
        }
    }
}

// MARK: - Live Activity model

public struct FaeryActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var fae: Fae
        public var hauntLevel: HauntLevel
        public var room: FaeRoom
        public var whisper: String
        public var updatedAt: Date

        public init(fae: Fae, hauntLevel: HauntLevel, room: FaeRoom, whisper: String, updatedAt: Date) {
            self.fae = fae
            self.hauntLevel = hauntLevel
            self.room = room
            self.whisper = whisper
            self.updatedAt = updatedAt
        }
    }

    public var colonyName: String

    public init(colonyName: String = "Faeryware") {
        self.colonyName = colonyName
    }
}

// MARK: - App Intents

public struct SummonFaeIntent: AppIntent {
    public static var title: LocalizedStringResource = "Summon Fae"
    public static var description = IntentDescription("Make a selected fae the active resident across Faeryware surfaces.")
    public static var openAppWhenRun = false

    @Parameter(title: "Fae") public var fae: Fae

    public init() {}
    public init(fae: Fae) { self.fae = fae }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        let state = FaerywareStore.mutate {
            $0.activeFae = fae
            $0.lastEvent = "\(fae.rawValue) summoned"
        }
        await FaerywareStore.refreshLiveActivities(with: state)
        return .result(dialog: "\(fae.rawValue) is now loose in the phone.")
    }
}

public struct CycleFaeIntent: AppIntent {
    public static var title: LocalizedStringResource = "Cycle Fae"
    public static var description = IntentDescription("Move the colony to the next resident fae.")
    public static var openAppWhenRun = false

    public init() {}

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        let state = FaerywareStore.mutate {
            $0.activeFae = FaerywareStore.nextFae(after: $0.activeFae)
            $0.lastEvent = "resident rotated"
        }
        await FaerywareStore.refreshLiveActivities(with: state)
        return .result(dialog: "\(state.activeFae.rawValue) took the perch.")
    }
}

public struct CycleHauntIntent: AppIntent {
    public static var title: LocalizedStringResource = "More Haunted"
    public static var description = IntentDescription("Cycle Calm → Haunted → Feral → Calm.")
    public static var openAppWhenRun = false

    public init() {}

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        let state = FaerywareStore.mutate {
            $0.hauntLevel = $0.hauntLevel.next
            $0.lastEvent = "haunt level → \($0.hauntLevel.rawValue)"
        }
        await FaerywareStore.refreshLiveActivities(with: state)
        return .result(dialog: "Haunt level: \(state.hauntLevel.rawValue).")
    }
}

public struct SetFaeRoomIntent: AppIntent {
    public static var title: LocalizedStringResource = "Enter Fae Room"
    public static var description = IntentDescription("Change the active Faeryware room or mode.")
    public static var openAppWhenRun = false

    @Parameter(title: "Room") public var room: FaeRoom

    public init() {}
    public init(room: FaeRoom) { self.room = room }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        let state = FaerywareStore.mutate {
            $0.room = room
            $0.lastEvent = "entered \(room.rawValue)"
        }
        await FaerywareStore.refreshLiveActivities(with: state)
        return .result(dialog: "Entered \(room.rawValue).")
    }
}
