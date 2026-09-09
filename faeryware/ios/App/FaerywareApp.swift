import SwiftUI
import ActivityKit
import AppIntents
import UserNotifications

@main
struct FaerywareApp: App {
    @StateObject private var core = GoblinCore()

    var body: some Scene {
        WindowGroup {
            ColonyHomeView()
                .environmentObject(core)
                .task {
                    core.reload()
                    await core.requestNotificationPermission()
                }
        }
    }
}

@MainActor
final class GoblinCore: ObservableObject {
    @Published var state: ColonyState = FaerywareStore.load()
    @Published var perchStatus = "not perched"

    func reload() {
        state = FaerywareStore.load()
    }

    func summon(_ fae: Fae) {
        state = FaerywareStore.mutate {
            $0.activeFae = fae
            $0.lastEvent = "\(fae.rawValue) summoned from habitat"
        }
        synchronizePerch()
    }

    func cycleFae() {
        state = FaerywareStore.mutate {
            $0.activeFae = FaerywareStore.nextFae(after: $0.activeFae)
            $0.lastEvent = "resident rotated from habitat"
        }
        synchronizePerch()
    }

    func moreHaunted() {
        state = FaerywareStore.mutate {
            $0.hauntLevel = $0.hauntLevel.next
            $0.lastEvent = "haunt level → \($0.hauntLevel.rawValue)"
        }
        synchronizePerch()
    }

    func enter(_ room: FaeRoom) {
        state = FaerywareStore.mutate {
            $0.room = room
            $0.lastEvent = "entered \(room.rawValue)"
        }
        synchronizePerch()
    }

    func startPerch() {
        Task {
            do {
                _ = try await LiveActivityManager.startOrRefresh()
                perchStatus = "resident perch active"
            } catch {
                perchStatus = "perch held: \(error.localizedDescription)"
            }
        }
    }

    func banishPerch() {
        Task {
            await LiveActivityManager.endAll()
            perchStatus = "perch banished"
        }
    }

    func synchronizePerch() {
        let snapshot = state
        Task {
            await FaerywareStore.refreshLiveActivities(with: snapshot)
        }
    }

    func requestNotificationPermission() async {
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
    }
}

enum LiveActivityManager {
    @discardableResult
    static func startOrRefresh() async throws -> Activity<FaeryActivityAttributes> {
        let state = FaerywareStore.load()
        let content = ActivityContent(
            state: FaerywareStore.contentState(from: state),
            staleDate: nil
        )

        if let existing = Activity<FaeryActivityAttributes>.activities.first {
            await existing.update(content)
            return existing
        }

        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            throw PerchError.liveActivitiesDisabled
        }

        return try Activity.request(
            attributes: FaeryActivityAttributes(),
            content: content,
            pushType: nil
        )
    }

    static func endAll() async {
        let state = FaerywareStore.load()
        let content = ActivityContent(
            state: FaerywareStore.contentState(from: state),
            staleDate: nil
        )
        for activity in Activity<FaeryActivityAttributes>.activities {
            await activity.end(content, dismissalPolicy: .immediate)
        }
    }

    enum PerchError: LocalizedError {
        case liveActivitiesDisabled

        var errorDescription: String? {
            "Live Activities are disabled for Faeryware."
        }
    }
}

struct FaerywareShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: CycleFaeIntent(),
            phrases: [
                "Cycle the fae in \(.applicationName)",
                "Who is out in \(.applicationName)"
            ],
            shortTitle: "Cycle Fae",
            systemImageName: "sparkles"
        )

        AppShortcut(
            intent: CycleHauntIntent(),
            phrases: [
                "Make \(.applicationName) more haunted",
                "Change haunt level in \(.applicationName)"
            ],
            shortTitle: "More Haunted",
            systemImageName: "wand.and.stars"
        )
    }
}

struct ColonyHomeView: View {
    @EnvironmentObject private var core: GoblinCore

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color.black,
                    core.state.activeFae.color.opacity(0.42),
                    Color.black
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 18) {
                    header
                    residentCard
                    hauntControls
                    faeGrid
                    roomGrid
                    perchControls
                    continuityCard
                }
                .padding()
            }
        }
        .preferredColorScheme(.dark)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("FAERYWARE")
                .font(.system(size: 34, weight: .black, design: .rounded))
            Text("iOS has developed a distributed tenant problem")
                .font(.caption.weight(.bold))
                .foregroundStyle(core.state.activeFae.color)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var residentCard: some View {
        VStack(spacing: 10) {
            Text(core.state.activeFae.emoji)
                .font(.system(size: 58))
            Text(core.state.activeFae.rawValue)
                .font(.title.bold())
            Text(core.state.activeFae.whisper)
                .font(.headline)
            Text("\(core.state.room.rawValue) • \(core.state.hauntLevel.rawValue)")
                .font(.caption.monospaced().bold())
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .background(core.state.activeFae.color.opacity(0.2), in: RoundedRectangle(cornerRadius: 28))
        .overlay {
            RoundedRectangle(cornerRadius: 28)
                .stroke(core.state.activeFae.color.opacity(0.8), lineWidth: 1)
        }
    }

    private var hauntControls: some View {
        HStack {
            Button("Cycle Fae") { core.cycleFae() }
            Button("More Haunted") { core.moreHaunted() }
        }
        .buttonStyle(.borderedProminent)
        .tint(core.state.activeFae.color)
    }

    private var faeGrid: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("SUMMON")
                .font(.caption.monospaced().bold())
                .foregroundStyle(.secondary)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(Fae.allCases) { fae in
                    Button {
                        core.summon(fae)
                    } label: {
                        HStack {
                            Text(fae.emoji)
                            Text(fae.rawValue)
                                .font(.subheadline.bold())
                            Spacer()
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity)
                        .background(fae.color.opacity(core.state.activeFae == fae ? 0.36 : 0.12), in: RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var roomGrid: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ROOMS")
                .font(.caption.monospaced().bold())
                .foregroundStyle(.secondary)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(FaeRoom.allCases, id: \.self) { room in
                    Button(room.rawValue) {
                        core.enter(room)
                    }
                    .buttonStyle(.bordered)
                    .tint(core.state.room == room ? core.state.activeFae.color : .secondary)
                }
            }
        }
    }

    private var perchControls: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("RESIDENT PERCH")
                .font(.caption.monospaced().bold())
                .foregroundStyle(.secondary)
            HStack {
                Button("Haunt Dynamic Island") { core.startPerch() }
                    .buttonStyle(.borderedProminent)
                    .tint(core.state.activeFae.color)
                Button("Banish") { core.banishPerch() }
                    .buttonStyle(.bordered)
            }
            Text(core.perchStatus)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20))
    }

    private var continuityCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("GOBLIN CORE")
                .font(.caption.monospaced().bold())
            Text("event #\(core.state.eventCount) • \(core.state.lastEvent)")
            Text(core.state.updatedAt, style: .relative)
                .foregroundStyle(.secondary)
        }
        .font(.caption)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18))
    }
}
