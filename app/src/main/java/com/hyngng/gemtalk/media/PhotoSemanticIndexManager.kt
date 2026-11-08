package com.hyngng.gemtalk.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.net.Uri
import android.provider.MediaStore
import android.util.JsonReader
import androidx.core.app.NotificationCompat
import com.hyngng.gemtalk.MainActivity
import com.hyngng.gemtalk.R
import com.hyngng.gemtalk.ui.i18n.tr
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.embedding.Content
import com.google.mlkit.genai.embedding.EmbeddingGeneration
import com.google.mlkit.genai.embedding.EmbeddingModel
import com.google.mlkit.genai.embedding.EmbeddingOptions
import com.google.mlkit.genai.embedding.EmbeddingRequest
import com.google.mlkit.genai.embedding.ImagePart
import com.google.mlkit.genai.embedding.Modality
import com.google.mlkit.genai.embedding.TextPart
import com.google.mlkit.genai.embedding.TaskConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * 768-dimensional on-device semantic record for a photo in MediaStore.
 * Aligns with Google's EmbeddingGemma 2 (`embedding_gemma_v2`) dimension spec.
 */
data class PhotoRecord(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val dateTaken: Long,
    val width: Int,
    val height: Int,
    val embedding: FloatArray,
    val modelSignature: String = "embedding_gemma_v2",
) {
    val uri: Uri get() = Uri.parse(uriString)

    val formattedDate: String get() = if (dateTaken > 0) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(dateTaken))
    } else {
        tr("날짜 미상", "Unknown date")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotoRecord) return false
        return id == other.id && uriString == other.uriString
    }

    override fun hashCode(): Int = id.hashCode()
}

data class PhotoSearchResult(
    val photo: PhotoRecord,
    val score: Float,
)

data class IndexingState(
    val isIndexing: Boolean = false,
    val isStopping: Boolean = false,
    // True until the saved index has been read once; the settings screen shows
    // a loading state instead of a misleading 0/0 while that happens.
    val isLoading: Boolean = false,
    val indexedCount: Int = 0,
    val totalDeviceCount: Int = 0,
    val excludedCount: Int = 0,
    val retryableFailureCount: Int = 0,
    val currentProgress: Float = 0f,
    val statusMessage: String = tr("대기 중", "Idle"),
    // True only when the embedding model itself could not be initialized, so
    // the UI can show the message without string matching.
    val modelUnavailable: Boolean = false,
    // True when the user stopped the run, so the caller can skip the
    // "some photos could not be analyzed" message for an intentional stop.
    val wasStopped: Boolean = false,
    val lastIndexedTime: Long = 0L,
)

private data class ExcludedPhoto(
    val id: Long,
    val fingerprint: String,
    val reason: String,
)

private sealed interface ImageEmbeddingOutcome {
    data class Indexed(val vector: FloatArray) : ImageEmbeddingOutcome
    data class Unindexable(val reason: String) : ImageEmbeddingOutcome
    data class Deferred(val reason: String, val stopRun: Boolean = false) : ImageEmbeddingOutcome
}

/**
 * Manages on-device photo semantic indexing and vector similarity search.
 * Connects MediaStore images, 768-dimensional EmbeddingGemma 2 vectors via ML Kit Embeddings API,
 * and vector storage with cosine similarity search.
 */
class PhotoSemanticIndexManager(context: Context) {

    companion object {
        private const val NOTIFICATION_CHANNEL = "photo_indexing"
        const val NOTIFICATION_ID = 4101
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 250L
        private const val JOURNAL_BATCH_SIZE = 20
        private const val INDEX_COMPACT_THRESHOLD_BYTES = 8L * 1024L * 1024L
        private const val MAX_BUSY_RETRIES = 4
        private const val EMBEDDING_TASK_PROFILE = "retrieval-document-v1"
        const val ACTION_STOP_INDEXING = "com.hyngng.gemtalk.STOP_PHOTO_INDEXING"
        const val EMBEDDING_DIMENSION = 768
        private const val EMBEDDING_IMAGE_SIZE = 768
        @Volatile
        private var activeManager = WeakReference<PhotoSemanticIndexManager>(null)

        fun stopActiveIndexing() {
            activeManager.get()?.stopIndexing()
        }

        /**
         * Whether this process is currently running (or stopping) an index pass.
         * A stop click from the notification can outlive the process that owned
         * the run; callers use this to tell a live stop request from a stale one.
         */
        fun hasActiveIndexing(): Boolean {
            val state = activeManager.get()?.indexingState?.value ?: return false
            return state.isIndexing || state.isStopping
        }
    }

    private val appContext = context.applicationContext
    private val notifications = appContext.getSystemService(NotificationManager::class.java)
    private val indexFile = File(appContext.filesDir, "photo_semantic_index_v2.json")
    private val journalFile = File(appContext.filesDir, "photo_semantic_index_v2.journal")
    private val lock = Any()
    private val modelMutex = Mutex()
    private val indexLoadMutex = Mutex()
    private val indexingMutex = Mutex()

    private val indexedPhotos = mutableMapOf<Long, PhotoRecord>()
    private val excludedPhotos = mutableMapOf<Long, ExcludedPhoto>()

    private val _indexingState = MutableStateFlow(IndexingState())
    val indexingState: StateFlow<IndexingState> = _indexingState.asStateFlow()

    @Volatile
    private var stopRequested = false
    @Volatile
    private var indexLoaded = false
    private var cachedEmbeddingModel: EmbeddingModel? = null
    private var lastNotificationPercent = -1
    private var lastNotificationStatus = ""
    private var lastNotificationAt = 0L

    init {
        activeManager = WeakReference(this)
        notifications.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                tr("사진 색인", "Photo indexing"),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        // Indexing runs inside this process. If we are being constructed, the
        // process that may have posted this notification is gone, so anything
        // still in the shade belongs to a dead run and can no longer be stopped.
        notifications.cancel(NOTIFICATION_ID)
    }

    fun getIndexedCount(): Int = synchronized(lock) { indexedPhotos.size }

    suspend fun loadIndex(): Boolean = indexLoadMutex.withLock {
        if (indexLoaded) return@withLock true
        _indexingState.value = _indexingState.value.copy(isLoading = true)
        try {
            val loaded = withContext(Dispatchers.IO) {
                loadIndexFromDisk()
            }
            indexLoaded = loaded
            if (!loaded) {
                _indexingState.value = _indexingState.value.copy(
                    statusMessage = tr("저장된 색인을 읽지 못했습니다. 다시 시도해 주세요.", "Could not read the saved index. Please retry."),
                )
            }
            loaded
        } finally {
            // A parse failure must not leave the settings screen spinning
            // forever: always drop the flag and let `indexLoaded` decide whether
            // the next visit retries the read.
            _indexingState.value = _indexingState.value.copy(isLoading = false)
        }
    }

    fun refreshDevicePhotoCount() {
        val total = queryMediaStorePhotoCount()
        if (total != null) {
            val excludedCount = synchronized(lock) { excludedPhotos.size.coerceAtMost(total) }
            val eligibleTotal = (total - excludedCount).coerceAtLeast(0)
            val indexedCount = getIndexedCount()
            _indexingState.value = _indexingState.value.copy(
                totalDeviceCount = eligibleTotal,
                excludedCount = excludedCount,
                currentProgress = progressFor(indexedCount, eligibleTotal, total),
            )
        }
    }

    fun getAllIndexedPhotos(): List<PhotoRecord> = synchronized(lock) {
        indexedPhotos.values.sortedByDescending { it.dateTaken }
    }

    /**
     * Resolves or initializes the on-device EmbeddingGemma 2 model from AICore.
     */
    private suspend fun getOrInitModel(): EmbeddingModel? = modelMutex.withLock {
        cachedEmbeddingModel?.let { return it }
        var candidate: EmbeddingModel? = null
        runCatching {
            val models = EmbeddingGeneration.getEmbeddingModels(EmbeddingOptions())
            val model = models.firstOrNull() ?: return null
            candidate = model
            models.drop(1).forEach { it.close() }

            val status = model.checkStatus()
            if (status != FeatureStatus.AVAILABLE) {
                model.close()
                candidate = null
                return null
            }

            val metadata = runCatching { model.getMetadata() }.getOrNull()
            if (metadata != null) {
                val supportsImage = metadata.supportedModalities.contains(Modality.IMAGE)
                val supportsText = metadata.supportedModalities.contains(Modality.TEXT)
                if (!supportsImage) {
                    model.close()
                    candidate = null
                    return null
                }
            }

            model.warmup()
            cachedEmbeddingModel = model
            candidate = null
            model
        }.onFailure { e ->
            runCatching { candidate?.close() }
            candidate = null
            if (e is CancellationException) throw e
        }.getOrNull()
    }

    /**
     * Search indexed photos using natural language query via 768-dim vector cosine similarity.
     * Encodes query using EmbeddingGemma 2 TextPart and computes similarity in multimodal space.
     */
    suspend fun searchPhotos(query: String, limit: Int = 3): List<PhotoSearchResult> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        if (!loadIndex()) throw IllegalStateException("The saved photo index could not be loaded")
        val model = getOrInitModel() ?: throw IllegalStateException("EmbeddingGemma is unavailable")
        val modelSignature = "${model.getModelSignature()}:$EMBEDDING_TASK_PROFILE"
        val queryVector = generateTextEmbedding(model, query)
        val photos = synchronized(lock) {
            indexedPhotos.values.filter { it.modelSignature == modelSignature }
        }

        photos.map { photo ->
            val score = cosineSimilarity(queryVector, photo.embedding)
            PhotoSearchResult(photo, score)
        }
            .filter { it.score > 0.15f }
            .sortedByDescending { it.score }
            .take(limit.coerceIn(1, 10))
    }

    /**
     * Starts background scanning and indexing of device photos via MediaStore.
     * Uses EmbeddingGemma 2 ImagePart for each photo.
     */
    suspend fun startIndexing(onDone: () -> Unit = {}) = indexingMutex.withLock {
        try {
            withContext(Dispatchers.IO) {
            if (!loadIndex()) return@withContext
            if (_indexingState.value.isIndexing) return@withContext

            stopRequested = false

            val model = getOrInitModel()
            if (model == null) {
                _indexingState.value = _indexingState.value.copy(
                    isIndexing = false,
                    modelUnavailable = true,
                    statusMessage = tr(
                        "EmbeddingGemma 2 모델을 초기화할 수 없습니다 (AICore 확인 필요).",
                        "EmbeddingGemma 2 model could not be initialized (check AICore).",
                    ),
                )
                return@withContext
            }

            val modelSignature = "${model.getModelSignature()}:$EMBEDDING_TASK_PROFILE"

            val devicePhotos = queryMediaStorePhotos()
            val pendingDeletedIds = synchronized(lock) {
                val currentById = devicePhotos.associateBy(MediaItem::id)
                val staleIds = (indexedPhotos.keys + excludedPhotos.keys).filter { id ->
                    val item = currentById[id]
                    item == null || excludedPhotos[id]?.fingerprint?.let { it != item.fingerprint } == true
                }
                staleIds.forEach {
                    indexedPhotos.remove(it)
                    excludedPhotos.remove(it)
                }
                staleIds.toMutableList()
            }
            val total = devicePhotos.size
            var excludedCount = synchronized(lock) {
                devicePhotos.count { item -> excludedPhotos[item.id]?.fingerprint == item.fingerprint }
            }
            var eligibleTotal = total - excludedCount
            var indexedDeviceCount = synchronized(lock) {
                devicePhotos.count { item ->
                    excludedPhotos[item.id]?.fingerprint != item.fingerprint &&
                        indexedPhotos[item.id]?.modelSignature == modelSignature
                }
            }

            _indexingState.value = IndexingState(
                isIndexing = true,
                isStopping = false,
                indexedCount = indexedDeviceCount,
                totalDeviceCount = eligibleTotal,
                excludedCount = excludedCount,
                currentProgress = if (eligibleTotal > 0) indexedDeviceCount.toFloat() / eligibleTotal else 1f,
                statusMessage = tr("사진 분석 시작 (분석 가능 ${eligibleTotal}장)", "Started analyzing photos ($eligibleTotal eligible)"),
            )
            publishIndexingNotification(force = true)

            val pendingJournalRecords = mutableListOf<PhotoRecord>()
            val pendingExcludedPhotos = mutableListOf<ExcludedPhoto>()
            var deferredFailures = 0
            var stopForQuota = false
            for (item in devicePhotos) {
                if (!isActive || stopRequested) break

                val isExcluded = synchronized(lock) {
                    excludedPhotos[item.id]?.fingerprint == item.fingerprint
                }
                if (isExcluded) continue

                // Incremental: skip if already indexed with the same model signature
                val alreadyIndexed = synchronized(lock) {
                    val existing = indexedPhotos[item.id]
                    existing != null && existing.modelSignature == modelSignature
                }

                if (!alreadyIndexed) {
                    when (val outcome = computeImageEmbedding(model, item.uri, item.displayName)) {
                        is ImageEmbeddingOutcome.Indexed -> {
                            val record = PhotoRecord(
                                id = item.id,
                                uriString = item.uri.toString(),
                                displayName = item.displayName,
                                dateTaken = item.dateTaken,
                                width = item.width,
                                height = item.height,
                                embedding = outcome.vector,
                                modelSignature = modelSignature,
                            )
                            synchronized(lock) {
                                excludedPhotos.remove(item.id)
                                indexedPhotos[item.id] = record
                            }
                            pendingJournalRecords += record
                            indexedDeviceCount++
                        }
                        is ImageEmbeddingOutcome.Unindexable -> {
                            val excluded = ExcludedPhoto(item.id, item.fingerprint, outcome.reason)
                            synchronized(lock) {
                                indexedPhotos.remove(item.id)
                                excludedPhotos[item.id] = excluded
                            }
                            pendingExcludedPhotos += excluded
                            excludedCount++
                            eligibleTotal--
                        }
                        is ImageEmbeddingOutcome.Deferred -> {
                            deferredFailures++
                            if (outcome.stopRun) stopForQuota = true
                        }
                    }
                }

                val progress = if (eligibleTotal > 0) (indexedDeviceCount.toFloat() / eligibleTotal).coerceIn(0f, 1f) else 1f
                val percent = if (eligibleTotal > 0) (indexedDeviceCount * 100 / eligibleTotal).coerceIn(0, 100) else 100
                _indexingState.value = _indexingState.value.copy(
                    indexedCount = indexedDeviceCount,
                    totalDeviceCount = eligibleTotal,
                    excludedCount = excludedCount,
                    currentProgress = progress,
                    statusMessage = if (stopRequested) {
                        tr("중지하는 중", "Stopping")
                    } else {
                        tr(
                            "사진 분석 중 $indexedDeviceCount/$eligibleTotal ($percent%) · 제외 ${excludedCount}장",
                            "Analyzing photos $indexedDeviceCount/$eligibleTotal ($percent%) · $excludedCount excluded",
                        )
                    },
                )
                publishIndexingNotification()

                if (pendingJournalRecords.size + pendingExcludedPhotos.size >= JOURNAL_BATCH_SIZE) {
                    if (appendJournal(pendingJournalRecords, pendingDeletedIds, pendingExcludedPhotos)) {
                        pendingJournalRecords.clear()
                        pendingExcludedPhotos.clear()
                        pendingDeletedIds.clear()
                    }
                }
                if (stopForQuota) break
            }

            val journalSaved = appendJournal(pendingJournalRecords, pendingDeletedIds, pendingExcludedPhotos)
            val shouldCompact = !journalSaved || !indexFile.exists() || journalFile.length() >= INDEX_COMPACT_THRESHOLD_BYTES
            val compactSaved = shouldCompact && saveIndexToDisk()
            val persisted = journalSaved || compactSaved

            val finalCount = indexedDeviceCount.coerceAtMost(eligibleTotal)
            if (!persisted) {
                _indexingState.value = _indexingState.value.copy(
                    isIndexing = false,
                    isStopping = false,
                    retryableFailureCount = 1,
                    statusMessage = tr("색인 결과를 저장하지 못했습니다. 다시 시도해 주세요.", "Could not save the index. Please retry."),
                )
                cancelIndexingNotification()
            } else if (stopRequested) {
                _indexingState.value = _indexingState.value.copy(
                    isIndexing = false,
                    isStopping = false,
                    indexedCount = finalCount,
                    totalDeviceCount = eligibleTotal,
                    excludedCount = excludedCount,
                    wasStopped = true,
                    statusMessage = tr(
                        "색인 중지됨 ${finalCount}/$eligibleTotal (${(finalCount * 100 / eligibleTotal.coerceAtLeast(1)).coerceIn(0, 100)}%)",
                        "Indexing stopped ${finalCount}/$eligibleTotal (${(finalCount * 100 / eligibleTotal.coerceAtLeast(1)).coerceIn(0, 100)}%)",
                    ),
                )
                cancelIndexingNotification()
            } else if (finalCount == eligibleTotal) {
                _indexingState.value = IndexingState(
                    isIndexing = false,
                    isStopping = false,
                    indexedCount = finalCount,
                    totalDeviceCount = eligibleTotal,
                    excludedCount = excludedCount,
                    currentProgress = 1f,
                    statusMessage = tr(
                        "색인 완료 ${finalCount}/$eligibleTotal (100%) · 제외 ${excludedCount}장",
                        "Indexing complete ${finalCount}/$eligibleTotal (100%) · $excludedCount excluded",
                    ),
                    lastIndexedTime = System.currentTimeMillis(),
                )
                cancelIndexingNotification()
                onDone()
            } else {
                val progress = if (eligibleTotal > 0) finalCount.toFloat() / eligibleTotal else 1f
                _indexingState.value = IndexingState(
                    isIndexing = false,
                    isStopping = false,
                    indexedCount = finalCount,
                    totalDeviceCount = eligibleTotal,
                    excludedCount = excludedCount,
                    retryableFailureCount = deferredFailures,
                    currentProgress = progress,
                    statusMessage = if (stopForQuota) {
                        tr(
                            "AICore 사용 한도로 색인을 잠시 멈췄어요. GemTalk을 열어둔 상태에서 다시 시도해 주세요. ${finalCount}/$eligibleTotal (${(finalCount * 100 / eligibleTotal.coerceAtLeast(1)).coerceIn(0, 100)}%).",
                            "Indexing paused because AICore reached a quota or foreground-use limit. Resume while GemTalk is open. ${finalCount}/$eligibleTotal (${(finalCount * 100 / eligibleTotal.coerceAtLeast(1)).coerceIn(0, 100)}%).",
                        )
                    } else {
                        tr(
                            "색인 미완료 ${finalCount}/$eligibleTotal (${(finalCount * 100 / eligibleTotal.coerceAtLeast(1)).coerceIn(0, 100)}%). 재시도 대상 ${deferredFailures}장. 다시 시도해 주세요.",
                            "Indexing incomplete ${finalCount}/$eligibleTotal (${(finalCount * 100 / eligibleTotal.coerceAtLeast(1)).coerceIn(0, 100)}%). $deferredFailures photos need retry.",
                        )
                    },
                    lastIndexedTime = System.currentTimeMillis(),
                )
                cancelIndexingNotification()
            }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _indexingState.value = _indexingState.value.copy(
                isIndexing = false,
                isStopping = false,
                modelUnavailable = false,
                retryableFailureCount = 1,
                statusMessage = tr(
                    "색인에 실패했습니다. 저장된 색인을 유지했습니다.",
                    "Indexing failed. The saved index was kept.",
                ),
            )
            cancelIndexingNotification()
        } finally {
            if (!currentCoroutineContext().isActive && _indexingState.value.isIndexing) {
                _indexingState.value = _indexingState.value.copy(
                    isIndexing = false,
                    isStopping = false,
                    statusMessage = tr("색인이 중단되었습니다.", "Indexing was interrupted."),
                )
                cancelIndexingNotification()
            } else if (!_indexingState.value.isIndexing) {
                cancelIndexingNotification()
            }
        }
    }

    fun stopIndexing() {
        if (!_indexingState.value.isIndexing || _indexingState.value.isStopping) return
        stopRequested = true
        _indexingState.value = _indexingState.value.copy(
            isStopping = true,
            statusMessage = tr("중지하는 중", "Stopping"),
        )
        publishIndexingNotification(force = true)
    }

    /**
     * Removes the progress notification once a run reaches a terminal state.
     * A finished or failed run must not leave a stale entry in the shade: the
     * result is already visible in the settings screen and, for a partial run,
     * as a toast.
     */
    private fun cancelIndexingNotification() {
        lastNotificationAt = 0L
        lastNotificationPercent = -1
        lastNotificationStatus = ""
        notifications.cancel(NOTIFICATION_ID)
    }

    private fun publishIndexingNotification(force: Boolean = false) {
        val state = _indexingState.value
        val percent = (state.currentProgress * 100).toInt().coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastNotificationAt < NOTIFICATION_UPDATE_INTERVAL_MS) {
            return
        }
        if (!force && percent == lastNotificationPercent && state.statusMessage == lastNotificationStatus) {
            return
        }
        lastNotificationAt = now
        lastNotificationPercent = percent
        lastNotificationStatus = state.statusMessage

        val total = state.totalDeviceCount
        val countText = if (total > 0) {
            tr(
                "${"%,d".format(state.indexedCount)} / ${"%,d".format(total)}장",
                "${"%,d".format(state.indexedCount)} / ${"%,d".format(total)} photos",
            )
        } else {
            tr("${"%,d".format(state.indexedCount)}장", "${"%,d".format(state.indexedCount)} photos")
        }
        // The system controls its own line spacing, so the only way to give the
        // rows room is to keep the collapsed view to one short line and put the
        // detail on separate lines that BigTextStyle shows when expanded. Only
        // in-progress runs reach this point; terminal states cancel the entry.
        val title = if (state.isStopping) {
            tr("사진 색인 중지 중", "Stopping photo indexing")
        } else {
            tr("사진 색인", "Photo indexing")
        }
        val contentText = when {
            state.isStopping -> tr("잠시만 기다려 주세요…", "Just a moment…")
            state.isIndexing && total > 0 -> tr("사진 분석 중 · $percent%", "Analyzing photos · $percent%")
            state.isIndexing -> tr("사진 분석 중", "Analyzing photos")
            else -> state.statusMessage
        }
        val bigText = if (state.isIndexing) {
            buildString {
                append(tr("사진 분석 중", "Analyzing photos"))
                append("\n")
                append(countText)
                if (total > 0) append(" · $percent%")
            }
        } else {
            state.statusMessage
        }
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(
                PendingIntent.getActivity(
                    appContext,
                    NOTIFICATION_ID,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isIndexing)
            .setAutoCancel(!state.isIndexing)
            .setProgress(100, percent, state.isIndexing && total == 0)
        if (state.isIndexing) {
            val stopIntent = Intent(appContext, PhotoIndexingNotificationReceiver::class.java)
                .setAction(ACTION_STOP_INDEXING)
            notification.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                tr("중지", "Stop"),
                PendingIntent.getBroadcast(
                    appContext,
                    NOTIFICATION_ID,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        val built = notification.build()
        try {
            notifications.notify(NOTIFICATION_ID, built)
        } catch (_: SecurityException) { }
    }

    suspend fun clearIndex(): Boolean = indexingMutex.withLock {
        stopIndexing()
        withContext(Dispatchers.IO) {
            val files = listOf(
                indexFile,
                journalFile,
                File(appContext.filesDir, "photo_semantic_index_v2.json.tmp"),
            )
            val removed = runCatching {
                files.all { !it.exists() || it.delete() }
            }.getOrDefault(false)
            if (!removed) {
                _indexingState.value = _indexingState.value.copy(
                    statusMessage = tr("색인을 완전히 초기화하지 못했습니다.", "Could not fully clear the index."),
                )
                return@withContext false
            }
            synchronized(lock) {
                indexedPhotos.clear()
                excludedPhotos.clear()
            }
            indexLoaded = true
            _indexingState.value = IndexingState(
                isIndexing = false,
                indexedCount = 0,
                totalDeviceCount = queryMediaStorePhotoCount() ?: _indexingState.value.totalDeviceCount,
                currentProgress = 0f,
                statusMessage = tr("색인이 초기화되었습니다.", "The index was cleared."),
            )
            cancelIndexingNotification()
            true
        }
    }

    private fun queryMediaStorePhotoCount(): Int? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        return runCatching {
            appContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { it.count } ?: error("MediaStore returned no cursor")
        }.getOrNull()
    }

    private data class MediaItem(
        val id: Long,
        val uri: Uri,
        val displayName: String,
        val dateTaken: Long,
        val width: Int,
        val height: Int,
        val dateModified: Long,
        val size: Long,
) {
        val fingerprint: String get() = "$dateModified:$size:$width:$height:$displayName"
    }

    private fun queryMediaStorePhotos(): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
        )
        val items = mutableListOf<MediaItem>()
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor = appContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        ) ?: error("MediaStore returned no cursor")
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = it.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val widthCol = it.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightCol = it.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val modifiedCol = it.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = it.getColumnIndex(MediaStore.Images.Media.SIZE)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = if (nameCol >= 0) it.getString(nameCol) ?: tr("사진", "Photo") else tr("사진", "Photo")
                val date = if (dateCol >= 0) it.getLong(dateCol) else 0L
                val w = if (widthCol >= 0) it.getInt(widthCol) else 0
                val h = if (heightCol >= 0) it.getInt(heightCol) else 0
                val modified = if (modifiedCol >= 0) it.getLong(modifiedCol) else 0L
                val size = if (sizeCol >= 0) it.getLong(sizeCol) else 0L
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                items.add(MediaItem(id, uri, name, date, w, h, modified, size))
            }
        }
        return items
    }

    /**
     * Computes the 768-dimensional embedding vector for an image file using EmbeddingGemma 2.
     * Prepares a bounded bitmap before creating ImagePart to avoid repeated full-resolution decoding.
     */
    private suspend fun computeImageEmbedding(
        model: EmbeddingModel,
        uri: Uri,
        title: String,
    ): ImageEmbeddingOutcome {
        val bitmap = try {
            decodeIndexBitmap(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ImageEmbeddingOutcome.Deferred("image read failed: ${e.javaClass.simpleName}")
        } ?: return ImageEmbeddingOutcome.Unindexable("decode_failed")

        try {
            var attempt = 0
            while (true) {
                attempt++
                try {
                    val imagePart = ImagePart(bitmap)
                    val vector = try {
                        model.generateEmbedding(
                            EmbeddingRequest(
                                Content(listOf(imagePart)),
                                TaskConfig.RetrievalDocument(title),
                            ),
                        ).embedding.vector
                    } finally {
                        imagePart.close()
                    }
                    if (vector.size != EMBEDDING_DIMENSION) {
                        return ImageEmbeddingOutcome.Deferred("unexpected embedding dimension ${vector.size}")
                    }
                    normalizeVector(vector)
                    return ImageEmbeddingOutcome.Indexed(vector)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: GenAiException) {
                    when (e.errorCode) {
                        GenAiException.ErrorCode.INVALID_INPUT_IMAGE ->
                            return ImageEmbeddingOutcome.Unindexable("invalid_input_image")
                        GenAiException.ErrorCode.BUSY -> {
                            if (attempt >= MAX_BUSY_RETRIES) {
                                return ImageEmbeddingOutcome.Deferred("AICore busy after $attempt attempts")
                            }
                            val fallbackDelay = 1_000L shl (attempt - 1).coerceAtMost(4)
                            val retryDelayMillis = e.retryDelay.toMillis().coerceAtLeast(fallbackDelay)
                            if (retryDelayMillis > 30_000L) {
                                return ImageEmbeddingOutcome.Deferred("AICore requested a $retryDelayMillis ms retry delay", stopRun = true)
                            }
                            delay(retryDelayMillis)
                        }
                        GenAiException.ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED,
                        GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED ->
                            return ImageEmbeddingOutcome.Deferred("AICore quota or foreground-use restriction (${e.errorCode})", stopRun = true)
                        GenAiException.ErrorCode.REQUEST_PROCESSING_ERROR -> {
                            return ImageEmbeddingOutcome.Deferred("AICore policy check rejected the photo request (REQUEST_PROCESSING_ERROR)")
                        }
                        else -> {
                            return ImageEmbeddingOutcome.Deferred("AICore error ${e.errorCode}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    return ImageEmbeddingOutcome.Deferred("${e.javaClass.simpleName}: ${e.message}")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeIndexBitmap(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.Options().also {
                it.inJustDecodeBounds = true
                BitmapFactory.decodeStream(input, null, it)
            }
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
        val sampleSize = generateSequence(1) { it * 2 }
            .takeWhile { longestSide / it > EMBEDDING_IMAGE_SIZE }
            .lastOrNull() ?: 1
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val sampled = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return null

        val sampledLongestSide = maxOf(sampled.width, sampled.height)
        if (sampledLongestSide <= EMBEDDING_IMAGE_SIZE) return sampled

        val scale = EMBEDDING_IMAGE_SIZE.toFloat() / sampledLongestSide
        val resized = Bitmap.createScaledBitmap(
            sampled,
            (sampled.width * scale).toInt().coerceAtLeast(1),
            (sampled.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (resized !== sampled) sampled.recycle()
        return resized
    }

    /**
     * Converts a natural language query into the 768-dimensional semantic space
     * using the real EmbeddingGemma 2 TextPart.
     */
    private suspend fun generateTextEmbedding(model: EmbeddingModel, text: String): FloatArray {
        val textPart = TextPart(text)
        val request = EmbeddingRequest(
            Content(listOf(textPart)),
            TaskConfig.RetrievalQuery(),
        )
        val vector = model.generateEmbedding(request).embedding.vector
        check(vector.size == EMBEDDING_DIMENSION) {
            "Unexpected query embedding dimension ${vector.size}"
        }
        normalizeVector(vector)
        return vector
    }

    fun close() {
        if (activeManager.get() === this) activeManager.clear()
        cachedEmbeddingModel?.close()
        cachedEmbeddingModel = null
    }

    private fun normalizeVector(v: FloatArray) {
        var sumSquares = 0.0
        for (value in v) {
            sumSquares += value * value
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm > 1e-6f) {
            for (i in v.indices) {
                v[i] /= norm
            }
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot.coerceIn(-1f, 1f)
    }

    private fun progressFor(indexedCount: Int, eligibleCount: Int, rawPhotoCount: Int): Float = when {
        eligibleCount > 0 -> (indexedCount.toFloat() / eligibleCount).coerceIn(0f, 1f)
        rawPhotoCount > 0 -> 1f
        else -> 0f
    }

    private fun saveIndexToDisk(): Boolean {
        val (snapshot, excludedSnapshot) = synchronized(lock) {
            indexedPhotos.values.toList() to excludedPhotos.values.toList()
        }
        return runCatching {
            val tempFile = File(appContext.filesDir, "photo_semantic_index_v2.json.tmp")
            // One nested JSON tree for tens of thousands of 768-float vectors caused large transient heaps.
            tempFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("{\"version\":3,\"updatedAt\":${System.currentTimeMillis()},\"photos\":[")
                snapshot.forEachIndexed { index, record ->
                    if (index > 0) writer.write(",")
                    writer.write(photoRecordToJson(record).toString())
                }
                writer.write("],\"excluded\":[")
                excludedSnapshot.forEachIndexed { index, photo ->
                    if (index > 0) writer.write(",")
                    writer.write(excludedPhotoToJson(photo).toString())
                }
                writer.write("]}")
            }
            Files.move(
                tempFile.toPath(),
                indexFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            journalFile.delete()
            true
        }.getOrDefault(false)
    }

    private fun loadIndexFromDisk(): Boolean = synchronized(lock) {
        val previous = indexedPhotos.toMap()
        val previousExcluded = excludedPhotos.toMap()
        runCatching {
            indexedPhotos.clear()
            excludedPhotos.clear()
            var lastUpdated = 0L
            if (indexFile.exists()) {
                // The v2 file holds one double per embedding component, so even a
                // few thousand photos make a very large JSON string. Building the
                // whole JSONObject graph in memory could OOM (or take minutes),
                // and the failure surfaced as "the saved index never loads".
                // Stream it record by record instead.
                lastUpdated = readIndexFileStreaming(indexFile)
            }
            if (journalFile.exists()) {
                journalFile.forEachLine { line ->
                    runCatching {
                        JSONObject(line).let { entry ->
                            if (entry.has("deletedId")) {
                                val id = entry.getLong("deletedId")
                                indexedPhotos.remove(id)
                                excludedPhotos.remove(id)
                                null
                            } else if (entry.has("excludedId")) {
                                val excluded = parseExcludedPhoto(entry)
                                indexedPhotos.remove(excluded.id)
                                excludedPhotos[excluded.id] = excluded
                                null
                            } else parsePhotoRecord(entry)
                        }
                    }
                        .getOrNull()
                        ?.let {
                            excludedPhotos.remove(it.id)
                            indexedPhotos[it.id] = it
                        }
                }
            }

            val indexedCount = indexedPhotos.size
            val rawPhotoCount = queryMediaStorePhotoCount() ?: _indexingState.value.totalDeviceCount + excludedPhotos.size
            val eligiblePhotoCount = (rawPhotoCount - excludedPhotos.size).coerceAtLeast(0)
            _indexingState.value = IndexingState(
                isIndexing = false,
                indexedCount = indexedCount,
                totalDeviceCount = eligiblePhotoCount,
                excludedCount = excludedPhotos.size,
                currentProgress = progressFor(indexedCount, eligiblePhotoCount, rawPhotoCount),
                statusMessage = tr("색인 로드 완료 (${indexedCount}장)", "Index loaded ($indexedCount photos)"),
                lastIndexedTime = lastUpdated,
            )
        }.onFailure {
            indexedPhotos.clear()
            indexedPhotos.putAll(previous)
            excludedPhotos.clear()
            excludedPhotos.putAll(previousExcluded)
        }.isSuccess
    }

    /**
     * Streams the saved index without materializing the whole JSON tree, and
     * publishes the running count so a long read looks alive rather than stuck.
     */
    private fun readIndexFileStreaming(file: File): Long {
        var lastUpdated = 0L
        var read = 0
        file.reader(Charsets.UTF_8).use { fileReader ->
            JsonReader(fileReader).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "updatedAt" -> lastUpdated = reader.nextLong()
                        "photos" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                runCatching { readPhotoRecordStreaming(reader) }
                                    .getOrNull()
                                    ?.let {
                                        excludedPhotos.remove(it.id)
                                        indexedPhotos[it.id] = it
                                        read++
                                        if (read % 250 == 0) {
                                            _indexingState.value = _indexingState.value.copy(
                                                indexedCount = indexedPhotos.size,
                                            )
                                        }
                                    }
                            }
                            reader.endArray()
                        }
                        "excluded" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                runCatching { readExcludedPhotoStreaming(reader) }
                                    .getOrNull()
                                    ?.let {
                                        indexedPhotos.remove(it.id)
                                        excludedPhotos[it.id] = it
                                    }
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        }
        return lastUpdated
    }

    private fun readPhotoRecordStreaming(reader: JsonReader): PhotoRecord? {
        var id = 0L
        var uri: String? = null
        var name: String? = null
        var date = 0L
        var width = 0
        var height = 0
        var signature = ""
        var embedding: FloatArray? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextLong()
                "uri" -> uri = reader.nextString()
                "name" -> name = reader.nextString()
                "date" -> date = reader.nextLong()
                "w" -> width = reader.nextInt()
                "h" -> height = reader.nextInt()
                "sig" -> signature = reader.nextString()
                "emb" -> {
                    reader.beginArray()
                    val values = FloatArray(EMBEDDING_DIMENSION)
                    var size = 0
                    while (reader.hasNext()) {
                        val value = reader.nextDouble().toFloat()
                        if (size < EMBEDDING_DIMENSION) values[size] = value
                        size++
                    }
                    reader.endArray()
                    embedding = if (size == EMBEDDING_DIMENSION) values else null
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val embeddingValue = embedding ?: return null
        val uriValue = uri ?: return null
        val nameValue = name ?: return null
        return PhotoRecord(
            id = id,
            uriString = uriValue,
            displayName = nameValue,
            dateTaken = date,
            width = width,
            height = height,
            embedding = embeddingValue,
            modelSignature = signature,
        )
    }

    private fun readExcludedPhotoStreaming(reader: JsonReader): ExcludedPhoto {
        var id = 0L
        var fingerprint = ""
        var reason = "unindexable"
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "excludedId" -> id = reader.nextLong()
                "fingerprint" -> fingerprint = reader.nextString()
                "reason" -> reason = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        require(id != 0L && fingerprint.isNotBlank())
        return ExcludedPhoto(id, fingerprint, reason)
    }

    private fun appendJournal(
        records: List<PhotoRecord>,
        deletedIds: List<Long> = emptyList(),
        excluded: List<ExcludedPhoto> = emptyList(),
    ): Boolean = synchronized(lock) {
        if (records.isEmpty() && deletedIds.isEmpty() && excluded.isEmpty()) return@synchronized true
        runCatching {
            journalFile.appendText(
                buildString {
                    deletedIds.forEach { id -> append(JSONObject().put("deletedId", id)).append('\n') }
                    records.forEach { append(photoRecordToJson(it)).append('\n') }
                    excluded.forEach { append(excludedPhotoToJson(it)).append('\n') }
                },
                Charsets.UTF_8,
            )
            true
        }.getOrElse { false }
    }

    private fun parsePhotoRecord(obj: JSONObject): PhotoRecord? {
        val embeddingArray = obj.getJSONArray("emb")
        if (embeddingArray.length() != EMBEDDING_DIMENSION) return null
        val embedding = FloatArray(EMBEDDING_DIMENSION) {
            embeddingArray.getDouble(it).toFloat()
        }
        return PhotoRecord(
            id = obj.getLong("id"),
            uriString = obj.getString("uri"),
            displayName = obj.getString("name"),
            dateTaken = obj.getLong("date"),
            width = obj.optInt("w", 0),
            height = obj.optInt("h", 0),
            embedding = embedding,
            modelSignature = obj.optString("sig", "embedding_gemma_v2"),
        )
    }

    private fun parseExcludedPhoto(obj: JSONObject): ExcludedPhoto = ExcludedPhoto(
        id = obj.getLong("excludedId"),
        fingerprint = obj.getString("fingerprint"),
        reason = obj.optString("reason", "unindexable"),
    )

    private fun excludedPhotoToJson(photo: ExcludedPhoto): JSONObject = JSONObject().apply {
        put("excludedId", photo.id)
        put("fingerprint", photo.fingerprint)
        put("reason", photo.reason)
    }

    private fun photoRecordToJson(record: PhotoRecord): JSONObject = JSONObject().apply {
        put("id", record.id)
        put("uri", record.uriString)
        put("name", record.displayName)
        put("date", record.dateTaken)
        put("w", record.width)
        put("h", record.height)
        put("sig", record.modelSignature)
        put("emb", JSONArray().apply {
            record.embedding.forEach { put(it) }
        })
    }
}
