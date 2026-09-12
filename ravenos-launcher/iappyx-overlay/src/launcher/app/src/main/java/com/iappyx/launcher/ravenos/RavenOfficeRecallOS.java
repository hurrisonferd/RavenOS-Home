package com.iappyx.launcher.ravenos;

import android.content.Context;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.localstorage.LocalStorage;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Local-only searchable index of structural Machine Kingdom history.
 *
 * The index deliberately excludes spoken dialogue, author notes, raw detail, OCR text, screenshots,
 * editable values and notification bodies. It indexes owner/signal/writer-family/canonical motif and
 * episode metadata so Raven can ask structural questions about the show without creating a screen
 * transcript archive.
 */
public final class RavenOfficeRecallOS {
    private static final String DB = "ravenos_office_recall";
    private static final String NS = "office";
    private static final String SCHEMA = "OfficeMoment";
    private static volatile ListenableFuture<AppSearchSession> sessionFuture;
    private static volatile boolean schemaReady = false;

    private RavenOfficeRecallOS() {}

    public static void indexReaction(Context context, RavenReactionPacket packet) {
        if (packet.getDialogue().isBlank()) return;
        Context app = context.getApplicationContext();
        ListenableFuture<AppSearchSession> future = sessionFuture(app);
        future.addListener(() -> {
            try {
                AppSearchSession session = future.get();
                ListenableFuture<SetSchemaResponse> schema = ensureSchema(session);
                schema.addListener(() -> {
                    try {
                        schema.get();
                        String motif = canonicalMotif(field(packet.getDetail(), "script_motif"));
                        String scene = field(packet.getDetail(), "screen_kind");
                        if (scene.isBlank()) scene = packet.getSignal();
                        String id = packet.getMarkerId() + "-" + packet.getUpdatedAt();
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        GenericDocument doc = new GenericDocument.Builder(NS, id, SCHEMA)
                            .setPropertyString("owner", safe(packet.getOwner(), 32))
                            .setPropertyString("signal", safe(packet.getSignal(), 32))
                            .setPropertyString("family", safe(packet.getDialogueFamily(), 180))
                            .setPropertyString("scene", safe(scene, 40))
                            .setPropertyString("motif", motif.isBlank() ? "NONE" : motif)
                            .setPropertyString("episode", safe(packet.getEpisode(), 40))
                            .setPropertyLong("at", packet.getUpdatedAt())
                            .build();
                        session.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(doc).build());
                    } catch (Throwable ignored) {
                        // Recall enrichment never blocks presentation.
                    }
                }, Runnable::run);
            } catch (Throwable ignored) {
                // Recall is enrichment. It must never block Goblin Vision presentation.
            }
        }, Runnable::run);
    }

    /** User-invoked structural recall search. Bounded blocking is acceptable for explicit command use. */
    public static String searchBlocking(Context context, String query) {
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) return "OFFICE RECALL: try `recall KYU`, `recall callback`, `recall smart capture`, or `recall music`.";
        try {
            AppSearchSession session = sessionFuture(context.getApplicationContext()).get(2500, TimeUnit.MILLISECONDS);
            ensureSchema(session).get(2500, TimeUnit.MILLISECONDS);
            SearchSpec spec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setResultCountPerPage(8)
                .addFilterSchemas(SCHEMA)
                .build();
            SearchResults results = session.search(q, spec);
            try {
                List<SearchResult> page = results.getNextPageAsync().get(2500, TimeUnit.MILLISECONDS);
                if (page.isEmpty()) return "OFFICE RECALL: no structural matches for `" + q + "`.";
                StringBuilder out = new StringBuilder("OFFICE RECALL · ").append(q).append('\n');
                int i = 0;
                for (SearchResult result : page) {
                    GenericDocument d = result.getGenericDocument();
                    if (i++ > 0) out.append('\n');
                    out.append(d.getPropertyString("owner"))
                        .append(" · ").append(d.getPropertyString("scene"))
                        .append(" · ").append(d.getPropertyString("motif"))
                        .append(" · ").append(shortFamily(d.getPropertyString("family")));
                    if (i >= 8) break;
                }
                return out.toString();
            } finally {
                results.close();
            }
        } catch (Throwable t) {
            return "OFFICE RECALL: index warming or unavailable; try again after the office speaks.";
        }
    }

    public static String compact(Context context) {
        return schemaReady ? "OFFICE_RECALL=READY" : "OFFICE_RECALL=WARMING";
    }

    private static ListenableFuture<AppSearchSession> sessionFuture(Context context) {
        ListenableFuture<AppSearchSession> existing = sessionFuture;
        if (existing != null) return existing;
        synchronized (RavenOfficeRecallOS.class) {
            if (sessionFuture == null) {
                LocalStorage.SearchContext searchContext = new LocalStorage.SearchContext.Builder(context, DB).build();
                sessionFuture = LocalStorage.createSearchSessionAsync(searchContext);
            }
            return sessionFuture;
        }
    }

    private static ListenableFuture<SetSchemaResponse> ensureSchema(AppSearchSession session) {
        AppSearchSchema schema = new AppSearchSchema.Builder(SCHEMA)
            .addProperty(stringProp("owner"))
            .addProperty(stringProp("signal"))
            .addProperty(stringProp("family"))
            .addProperty(stringProp("scene"))
            .addProperty(stringProp("motif"))
            .addProperty(stringProp("episode"))
            .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("at").build())
            .build();
        ListenableFuture<SetSchemaResponse> future = session.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build());
        future.addListener(() -> schemaReady = true, Runnable::run);
        return future;
    }

    private static AppSearchSchema.StringPropertyConfig stringProp(String name) {
        return new AppSearchSchema.StringPropertyConfig.Builder(name)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .build();
    }

    private static String field(String detail, String name) {
        if (detail == null || detail.isBlank()) return "";
        String needle = name + ":";
        for (String part : detail.split("\\|")) {
            if (part.startsWith(needle)) return part.substring(needle.length()).trim();
        }
        return "";
    }

    private static String canonicalMotif(String raw) {
        String v = raw == null ? "" : raw.toUpperCase(Locale.US).replaceAll("[^A-Z0-9_]+", "_");
        return switch (v) {
            case "SELF_AWARE_OFFICE", "SELF_REVIEW_SCREENSHOT", "SOUNDTRACK_MONTAGE", "MUSIC_ROOM",
                 "CHATGPT_SELF_DEBUG", "CALLBACK_ABOUT_CALLBACKS", "SELECTING_MEDIA", "SCROLLING_THREAD",
                 "UI_SELECTION", "CAMEO_SMART_CAPTURE", "CAMEO_SYSTEM_UI", "CAMEO_NOTIFICATION_SHADE",
                 "CAMEO_NOTIFICATION", "CAMEO_KEYBOARD" -> v;
            default -> "";
        };
    }

    private static String safe(String raw, int max) {
        if (raw == null) return "NONE";
        String v = raw.replaceAll("\\s+", " ").trim();
        if (v.isBlank()) return "NONE";
        return v.substring(0, Math.min(v.length(), max));
    }

    private static String shortFamily(String family) {
        String f = safe(family, 180);
        String[] parts = f.split("\\+");
        return parts.length == 0 ? f : parts[0];
    }
}
