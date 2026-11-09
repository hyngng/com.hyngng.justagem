package com.hyngng.justagem.ai.tools

import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.schema.tools.FunctionDeclaration
import com.google.mlkit.genai.schema.tools.FunctionSchema
import com.google.mlkit.genai.schema.tools.FunctionType
import com.hyngng.justagem.media.PhotoSemanticIndexManager
import com.hyngng.justagem.ui.i18n.tr
import kotlinx.coroutines.CancellationException

private const val MAX_PHOTO_QUERY_LENGTH = 120

internal fun searchDevicePhotosTool(
    indexManager: PhotoSemanticIndexManager,
    previousPhotoUris: List<String> = emptyList(),
) = LocalTool(
    declaration = FunctionDeclaration(
        name = "search_images",
        description = "Search the local image index by text query. If the user refers to a photo from the most recent search results, use previous_result_index to retrieve that exact photo again.",
        parameters = FunctionSchema(
            type = FunctionType.OBJECT,
            properties = mapOf(
                "query" to FunctionSchema(
                    type = FunctionType.STRING,
                    description = "Short text query or description, at most 120 characters. Required even when previous_result_index is provided.",
                    maxLength = MAX_PHOTO_QUERY_LENGTH.toLong(),
                ),
                "previous_result_index" to FunctionSchema(
                    type = FunctionType.INTEGER,
                    description = "One-based position in the most recent photo search results. Use only when the user refers to one of those exact photos.",
                    minimum = 1.0,
                    maximum = 3.0,
                ),
                "limit" to FunctionSchema(
                    type = FunctionType.INTEGER,
                    description = "Maximum number of results to return, from 1 to 3. Defaults to 3.",
                    minimum = 1.0,
                    maximum = 3.0,
                ),
            ),
            required = listOf("query"),
        ),
    ),
    timeoutMillis = null,
    execute = { args ->
        args.int("previous_result_index")?.let { resultIndex ->
            val previousUri = previousPhotoUris.getOrNull(resultIndex - 1)
                ?: return@LocalTool ToolOutcome("error: no photo exists at that position in the most recent search results.")
            return@LocalTool ToolOutcome(
                "Retrieved photo $resultIndex from the most recent local photo search.",
                photoUris = listOf(previousUri),
            )
        }
        val query = args.string("query")
            ?: return@LocalTool ToolOutcome(tr("오류: 검색어가 필요합니다.", "error: query is required."))
        if (query.length > MAX_PHOTO_QUERY_LENGTH) {
            return@LocalTool ToolOutcome(
                tr(
                    "오류: 사진 검색어는 120자 이내로 입력해 주세요.",
                    "error: the photo search query must be 120 characters or fewer.",
                ),
            )
        }
        val limit = (args.int("limit") ?: 3).coerceIn(1, 3)
        val results = try {
            indexManager.searchPhotos(query, limit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: GenAiException) {
            return@LocalTool ToolOutcome(
                tr(
                    "오류: 온디바이스 임베딩 모델이 검색어를 처리하지 못해 사진 검색에 실패했어요. 일치하는 사진이 없다는 뜻은 아니에요. 같은 검색을 반복하지 말고 검색 실패를 사용자에게 알려주세요.",
                    "error: Photo search failed because the on-device embedding model could not process the query (error ${error.errorCode}). This does not mean there were no matches. Do not repeat the same search; tell the user photo search failed.",
                ),
            )
        } catch (_: Exception) {
            return@LocalTool ToolOutcome(
                tr(
                    "오류: 사진 색인 또는 온디바이스 임베딩 문제로 사진 검색을 완료하지 못했어요. 같은 검색을 반복하지 말고 검색 실패를 사용자에게 알려주세요.",
                    "error: Photo search could not complete because of a local index or on-device embedding error. Do not repeat the same search; tell the user photo search failed.",
                ),
            )
        }
        if (results.isEmpty()) {
            val count = indexManager.getIndexedCount()
            return@LocalTool if (count == 0) {
                ToolOutcome(
                    tr(
                        "기기에 색인된 사진이 없습니다. 생성 설정에서 사진 색인을 먼저 만들어 주세요.",
                        "No photos are indexed on this device. Create a photo index in generation settings first.",
                    ),
                )
            } else {
                ToolOutcome(
                    tr(
                        "'$query'와 일치하는 색인 사진을 찾지 못했습니다.",
                        "No indexed photos matched '$query'.",
                    ),
                )
            }
        }

        val summary = buildString {
            appendLine(tr("검색된 사진 (${results.size}장):", "Matching photos (${results.size}):"))
            results.forEachIndexed { index, result ->
                appendLine("${index + 1}. ${result.photo.displayName}")
                appendLine(tr("   촬영일시: ${result.photo.formattedDate}", "   Taken: ${result.photo.formattedDate}"))
                appendLine(tr("   유사도: ${(result.score * 100).toInt()}%", "   Similarity: ${(result.score * 100).toInt()}%"))
            }
        }
        ToolOutcome(summary, photoUris = results.map { it.photo.uriString })
    },
)
