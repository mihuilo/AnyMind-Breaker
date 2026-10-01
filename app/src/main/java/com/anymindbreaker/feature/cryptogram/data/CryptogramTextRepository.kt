package com.anymindbreaker.feature.cryptogram.data

import android.content.res.AssetManager
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.Language
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/** A phrase from the content files. The engine never refers to particular phrases. */
@Serializable
data class CryptogramText(
    val id: String,
    val difficulty: Difficulty,
    val text: String,
)

private val contentJson = Json { ignoreUnknownKeys = true }

fun parseCryptogramTexts(json: String): List<CryptogramText> = contentJson.decodeFromString(json)

fun cryptogramAssetPath(language: Language): String = when (language) {
    Language.RU -> "puzzles/cryptograms_ru.json"
    Language.EN -> "puzzles/cryptograms_en.json"
}

interface CryptogramTextSource {
    suspend fun texts(language: Language): List<CryptogramText>
}

class CryptogramTextRepository(
    private val assets: AssetManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CryptogramTextSource {

    private val cache = ConcurrentHashMap<Language, List<CryptogramText>>()

    override suspend fun texts(language: Language): List<CryptogramText> {
        cache[language]?.let { return it }
        val loaded = withContext(ioDispatcher) {
            assets.open(cryptogramAssetPath(language)).use { parseCryptogramTexts(it.readBytes().decodeToString()) }
        }
        cache[language] = loaded
        return loaded
    }
}
