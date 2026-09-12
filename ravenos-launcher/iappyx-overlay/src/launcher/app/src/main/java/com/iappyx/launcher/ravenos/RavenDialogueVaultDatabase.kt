package com.iappyx.launcher.ravenos

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local structural store for the RavenOS writers' room.
 *
 * Privacy boundary: this database stores authored templates, structural fingerprints, expression
 * usage and typed scene metadata. It MUST NOT store screenshots, raw OCR transcripts, editable
 * values, password text, notification bodies, or arbitrary visible-screen phrases.
 */
@Entity(tableName = "dialogue_templates")
data class RavenDialogueTemplateEntity(
    @PrimaryKey val lineId: String,
    val owner: String,
    val family: String,
    val form: String,
    val sceneClass: String,
    val template: String,
    val weight: Int = 10,
    val intensity: Int = 1,
    val metaLevel: Int = 0,
    val useCount: Int = 0,
    val lastAt: Long = 0L,
)

@Entity(tableName = "dialogue_fingerprints")
data class RavenDialogueFingerprintEntity(
    @PrimaryKey val fingerprint: String,
    val owner: String,
    val form: String,
    val motif: String,
    val sceneClass: String,
    val partner: String,
    val closingShape: String,
    val useCount: Int,
    val lastAt: Long,
)

@Entity(tableName = "expression_usage")
data class RavenExpressionUseEntity(
    @PrimaryKey val expressionId: String,
    val owner: String,
    val mood: String,
    val useCount: Int,
    val lastAt: Long,
    val lastTurn: Int,
)

@Entity(tableName = "scene_history")
data class RavenSceneHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val signature: String,
    val appClass: String,
    val sceneType: String,
    val task: String,
    val motif: String,
    val selectedClass: String,
    val interaction: String,
    val durationMs: Long,
    val returnCount: Int,
    val at: Long,
)

@Dao
interface RavenDialogueVaultDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTemplates(rows: List<RavenDialogueTemplateEntity>)

    @Query("SELECT COUNT(*) FROM dialogue_templates")
    fun templateCount(): Int

    @Query("SELECT * FROM dialogue_templates WHERE (owner = :owner OR owner = '*') AND (form = :form OR form = '*') AND (sceneClass = :sceneClass OR sceneClass = '*') ORDER BY useCount ASC, lastAt ASC LIMIT 96")
    fun candidateTemplates(owner: String, form: String, sceneClass: String): List<RavenDialogueTemplateEntity>

    @Query("UPDATE dialogue_templates SET useCount = useCount + 1, lastAt = :at WHERE lineId = :lineId")
    fun markTemplateUsed(lineId: String, at: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putFingerprint(row: RavenDialogueFingerprintEntity)

    @Query("SELECT * FROM dialogue_fingerprints WHERE fingerprint = :fingerprint LIMIT 1")
    fun fingerprint(fingerprint: String): RavenDialogueFingerprintEntity?

    @Query("SELECT * FROM dialogue_fingerprints WHERE owner = :owner ORDER BY lastAt DESC LIMIT :limit")
    fun recentFingerprints(owner: String, limit: Int): List<RavenDialogueFingerprintEntity>

    @Query("DELETE FROM dialogue_fingerprints WHERE lastAt < :before")
    fun pruneFingerprints(before: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putExpression(row: RavenExpressionUseEntity)

    @Query("SELECT * FROM expression_usage WHERE expressionId = :id LIMIT 1")
    fun expression(id: String): RavenExpressionUseEntity?

    @Query("SELECT * FROM expression_usage WHERE owner = :owner AND mood = :mood ORDER BY lastAt DESC LIMIT :limit")
    fun recentExpressions(owner: String, mood: String, limit: Int): List<RavenExpressionUseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScene(row: RavenSceneHistoryEntity)

    @Query("SELECT * FROM scene_history ORDER BY at DESC LIMIT :limit")
    fun recentScenes(limit: Int): List<RavenSceneHistoryEntity>

    @Query("SELECT COUNT(*) FROM scene_history")
    fun sceneCount(): Int

    @Query("DELETE FROM scene_history WHERE id NOT IN (SELECT id FROM scene_history ORDER BY at DESC LIMIT :keep)")
    fun pruneScenes(keep: Int)

    @Query("DELETE FROM dialogue_fingerprints")
    fun clearFingerprints()

    @Query("DELETE FROM expression_usage")
    fun clearExpressions()

    @Query("DELETE FROM scene_history")
    fun clearScenes()
}

@Database(
    entities = [
        RavenDialogueTemplateEntity::class,
        RavenDialogueFingerprintEntity::class,
        RavenExpressionUseEntity::class,
        RavenSceneHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class RavenDialogueVaultDatabase : RoomDatabase() {
    abstract fun dao(): RavenDialogueVaultDao

    companion object {
        @Volatile private var INSTANCE: RavenDialogueVaultDatabase? = null

        fun get(context: Context): RavenDialogueVaultDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                RavenDialogueVaultDatabase::class.java,
                "ravenos_dialogue_vault.db",
            )
                // Reaction settlement is currently synchronous; records are tiny and bounded.
                // If the launcher later moves the writers' room to coroutines, remove this seam.
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
