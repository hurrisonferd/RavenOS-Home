import WidgetKit
import SwiftUI
import ActivityKit
import AppIntents

@main
struct FaerywareWidgetBundle: WidgetBundle {
    var body: some Widget {
        ColonyWidget()
        FaerywareLiveActivity()
        HauntControl()
    }
}

// MARK: - Home / Lock Screen colony widget

struct ColonyEntry: TimelineEntry {
    let date: Date
    let state: ColonyState
}

struct ColonyProvider: TimelineProvider {
    func placeholder(in context: Context) -> ColonyEntry {
        ColonyEntry(date: Date(), state: .initial())
    }

    func getSnapshot(in context: Context, completion: @escaping (ColonyEntry) -> Void) {
        completion(ColonyEntry(date: Date(), state: FaerywareStore.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ColonyEntry>) -> Void) {
        let entry = ColonyEntry(date: Date(), state: FaerywareStore.load())
        completion(Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(15 * 60))))
    }
}

struct ColonyWidget: Widget {
    let kind = "com.ravenos.faeryware.colony"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ColonyProvider()) { entry in
            ColonyWidgetView(entry: entry)
                .containerBackground(for: .widget) {
                    LinearGradient(
                        colors: [Color.black, entry.state.activeFae.color.opacity(0.52)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                }
        }
        .configurationDisplayName("Faeryware Colony")
        .description("A window into whichever fae currently has the phone.")
        .supportedFamilies([
            .systemSmall,
            .systemMedium,
            .accessoryCircular,
            .accessoryRectangular,
            .accessoryInline
        ])
    }
}

struct ColonyWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: ColonyEntry

    var body: some View {
        switch family {
        case .accessoryInline:
            Text("\(entry.state.activeFae.emoji) \(entry.state.activeFae.rawValue) • \(entry.state.hauntLevel.rawValue)")
        case .accessoryCircular:
            VStack(spacing: 0) {
                Text(entry.state.activeFae.emoji)
                    .font(.title2)
                Text(entry.state.activeFae.rawValue.prefix(3))
                    .font(.caption2.bold())
            }
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 2) {
                Text("\(entry.state.activeFae.emoji) \(entry.state.activeFae.rawValue)")
                    .font(.headline)
                Text(entry.state.activeFae.whisper)
                    .font(.caption)
                    .lineLimit(1)
                Text(entry.state.room.rawValue)
                    .font(.caption2.monospaced())
                    .foregroundStyle(.secondary)
            }
        default:
            standardWidget
        }
    }

    private var standardWidget: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(entry.state.activeFae.emoji)
                    .font(.title)
                VStack(alignment: .leading, spacing: 0) {
                    Text(entry.state.activeFae.rawValue)
                        .font(.headline.bold())
                    Text(entry.state.hauntLevel.rawValue)
                        .font(.caption2.monospaced().bold())
                        .foregroundStyle(entry.state.activeFae.color)
                }
                Spacer()
            }

            Text(entry.state.activeFae.whisper)
                .font(.subheadline.weight(.semibold))
                .lineLimit(2)

            if family == .systemMedium {
                Text("\(entry.state.room.rawValue) • event #\(entry.state.eventCount)")
                    .font(.caption2.monospaced())
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 0)

            Button(intent: CycleHauntIntent()) {
                Label("More haunted", systemImage: "wand.and.stars")
                    .font(.caption.bold())
            }
            .buttonStyle(.plain)
            .tint(entry.state.activeFae.color)
        }
    }
}

// MARK: - Dynamic Island / Live Activity resident perch

struct FaerywareLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: FaeryActivityAttributes.self) { context in
            HStack(spacing: 12) {
                Text(context.state.fae.emoji)
                    .font(.largeTitle)
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(context.state.fae.rawValue) • \(context.state.hauntLevel.rawValue)")
                        .font(.headline)
                    Text(context.state.whisper)
                        .font(.subheadline)
                    Text(context.state.room.rawValue)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .padding()
            .activityBackgroundTint(Color.black.opacity(0.9))
            .activitySystemActionForegroundColor(context.state.fae.color)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text(context.state.fae.emoji)
                        .font(.title)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.state.hauntLevel.rawValue)
                        .font(.caption2.monospaced().bold())
                        .foregroundStyle(context.state.fae.color)
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.fae.rawValue)
                        .font(.headline.bold())
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(spacing: 2) {
                        Text(context.state.whisper)
                            .font(.caption)
                        Text(context.state.room.rawValue)
                            .font(.caption2.monospaced())
                            .foregroundStyle(.secondary)
                    }
                }
            } compactLeading: {
                Text(context.state.fae.emoji)
            } compactTrailing: {
                Text(context.state.fae.rawValue.prefix(1))
                    .font(.caption.bold())
                    .foregroundStyle(context.state.fae.color)
            } minimal: {
                Text(context.state.fae.emoji)
            }
            .keylineTint(context.state.fae.color)
        }
    }
}

// MARK: - Control Center / Lock Screen / Action Button control

struct HauntControl: ControlWidget {
    static let kind = "com.ravenos.faeryware.more-haunted"

    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: Self.kind) {
            ControlWidgetButton(action: CycleHauntIntent()) {
                Label("More Haunted", systemImage: "wand.and.stars")
            }
        }
        .displayName("More Haunted")
        .description("Cycle the Faeryware colony through Calm, Haunted, and Feral.")
    }
}
