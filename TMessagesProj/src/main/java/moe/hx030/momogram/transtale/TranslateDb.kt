package moe.hx030.momogram.transtale

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import org.dizitart.no2.Nitrite
import org.dizitart.no2.filters.FluentFilter
import org.dizitart.no2.repository.ObjectRepository
import org.telegram.messenger.LocaleController
import org.telegram.ui.LaunchActivity
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.database.mkDatabase
import moe.hx030.momogram.utils.StrUtil
import moe.hx030.momogram.utils.UIUtil
import java.util.*
import kotlin.collections.HashMap

class TranslateDb(val code: String) {

    val conn: ObjectRepository<TransItem?>?
        get() = db?.getRepository(TransItem::class.java, code)

    companion object {

        private const val RECOVERY_INTERVAL_MS = 5000L

        @Volatile
        var db: Nitrite? = mkDatabase("translate_caches")
            private set

        val repo = HashMap<Locale, TranslateDb>()

        val chat: ObjectRepository<ChatLanguage>?
            get() = db?.getRepository(ChatLanguage::class.java, "chat")

        val ccTarget: ObjectRepository<ChatCCTarget>?
            get() = db?.getRepository(ChatCCTarget::class.java, "opencc")

        lateinit var fallbackPrefs: SharedPreferences

        private val recoverLock = Any()

        @Volatile
        private var lastRecovery = 0L

        /**
         * The translation cache is not critical. If the underlying Nitrite/MVStore
         * database is corrupt it must never break the translation itself.
         */
        fun <T> runWithRecovery(block: () -> T): T? {
            return try {
                block()
            } catch (t: Exception) {
                t.message?.contains("Could not deserialize")?.let { if (!it) throw t }
                Log.e(StrUtil.get030Tag(TranslateDb), "translate db error, rebuilding cache", t)
                recover()
                try {
                    block()
                } catch (t2: Exception) {
                    Log.e(StrUtil.get030Tag(TranslateDb), "translate db still failing", t2)
                    null
                }
            }
        }

        @JvmStatic
        fun recover() {
            if (Build.VERSION.SDK_INT < 26) return
            synchronized(recoverLock) {
                val now = System.currentTimeMillis()
                if (now - lastRecovery < RECOVERY_INTERVAL_MS) return
                lastRecovery = now
                rebuildLocked()
            }
        }

        @JvmStatic
        fun clearCache() {
            if (Build.VERSION.SDK_INT < 26) return
            synchronized(recoverLock) {
                lastRecovery = System.currentTimeMillis()
                rebuildLocked()
            }
        }

        private fun rebuildLocked() {
            Log.w(StrUtil.get030Tag(TranslateDb), "rebuilding translate database")
            runCatching { db?.close() }
            db = null
            repo.clear()
            db = try {
                mkDatabase("translate_caches", true)
            } catch (t: Throwable) {
                Log.e(StrUtil.get030Tag(TranslateDb), "failed to rebuild translate database", t)
                null
            }
        }

        @JvmStatic
        fun getChatLanguage(chatId: Long, default: Locale): Locale? {
            if (Build.VERSION.SDK_INT < 26) return getChatLanguageFallback(chatId, default)
            return runWithRecovery {
                val repository = chat ?: return@runWithRecovery default
                val cursor = repository.find(FluentFilter.where("chatId").eq(chatId))
                var result = default
                cursor.forEach { result = it.language.code2Locale }
                result
            } ?: default
        }

        @JvmStatic
        fun getChatLanguage(chatId: Long): ChatLanguage? {
            if (Build.VERSION.SDK_INT < 26) {
                val loc = getChatLanguageFallback(chatId, null)
                return ChatLanguage(chatId, loc?.locale2code, false)
            }
            return runWithRecovery {
                val repository = chat ?: return@runWithRecovery null
                val cursor = repository.find(FluentFilter.where("chatId").eq(chatId))
                var result: ChatLanguage? = null
                cursor.forEach { result = it }
                result
            }
        }

        @JvmStatic
        private fun getChatLanguageFallback(chatId: Long, default: Locale?): Locale? {
            initFallbackPrefs()
            return fallbackPrefs.getString("chat_lang_$chatId", default?.locale2code)?.code2Locale
        }

        @JvmStatic
        fun saveChatLanguage(chatId: Long, locale: Locale) = UIUtil.runOnIoDispatcher {
            if (Build.VERSION.SDK_INT < 26) {
                return@runOnIoDispatcher saveChatLanguageFallback(chatId, locale)
            }
            Log.d(StrUtil.get030Tag(TranslateDb), "saveLang: ${locale.locale2code}")
            runWithRecovery {
                val repository = chat ?: return@runWithRecovery
                val lang = getChatLanguage(chatId)
                val alwaysTranslateBeforeSend = lang?.alwaysTranslateBeforeSend

                repository.update(ChatLanguage(chatId, locale.locale2code, alwaysTranslateBeforeSend == true), true)
            }
        }

        @JvmStatic
        fun saveChatLanguageFallback(chatId: Long, locale: Locale) = UIUtil.runOnIoDispatcher {
            Log.d(StrUtil.get030Tag(TranslateDb), "saveLang: ${locale.locale2code}")
            initFallbackPrefs()
            fallbackPrefs.edit { putString("chat_lang_$chatId", locale.locale2code) }
        }

        @JvmStatic
        fun getTranslateBeforeSend(chatId: Long): Boolean {
            return runWithRecovery {
                val repository = chat ?: return@runWithRecovery false
                val cursor = repository.find(FluentFilter.where("chatId").eq(chatId))
                var result = false
                cursor.forEach { result = it.alwaysTranslateBeforeSend }
                result
            } ?: false
        }

        @JvmStatic
        fun setTranslateBeforeSend(chatId: Long, value: Boolean) = UIUtil.runOnIoDispatcher {
            if (Build.VERSION.SDK_INT < 26) return@runOnIoDispatcher
            runWithRecovery {
                val repository = chat ?: return@runWithRecovery
                val cursor = repository.find(FluentFilter.where("chatId").eq(chatId))
                var updated = false
                cursor.forEach {
                    it.alwaysTranslateBeforeSend = value
                    repository.update(it, true)
                    updated = true
                }
                if (!updated) repository.update(ChatLanguage(chatId, "en", value), true)
            }
        }

        @JvmStatic
        fun getChatCCTarget(chatId: Long, default: String?): String? {
            if (Build.VERSION.SDK_INT < 26) return null
            return runWithRecovery {
                val repository = ccTarget ?: return@runWithRecovery default
                val cursor = repository.find(FluentFilter.where("chatId").eq(chatId))
                var result = default
                cursor.forEach { result = it.ccTarget }
                result
            }
        }

        @JvmStatic
        fun saveChatCCTarget(chatId: Long, target: String) = UIUtil.runOnIoDispatcher {
            if (Build.VERSION.SDK_INT < 26) return@runOnIoDispatcher
            runWithRecovery {
                val repository = ccTarget ?: return@runWithRecovery
                repository.update(ChatCCTarget(chatId, target), true)
            }
        }

        @JvmStatic
        fun currentTarget(): TranslateDb? {
            if (Build.VERSION.SDK_INT < 26) return null
            return MomoConfig.translateToLang.String()?.transDbByCode
                ?: LocaleController.getInstance().currentLocale.transDb
        }

        @JvmStatic
        fun currentTargetLocale(): Locale {
            return MomoConfig.translateToLang.String()?.code2Locale
                ?: LocaleController.getInstance().currentLocale
        }

        @JvmStatic
        fun forLocale(locale: Locale): TranslateDb? {
            if (Build.VERSION.SDK_INT < 26) return null
            return locale.transDb
        }

        @JvmStatic
        fun currentInputTarget() = MomoConfig.translateInputLang.String().transDbByCode

        @JvmStatic
        fun clearAll() {
            if (Build.VERSION.SDK_INT < 26 && TranslateDb::fallbackPrefs.isInitialized) {
                initFallbackPrefs()
                fallbackPrefs.edit { clear() }
            }

            if (Build.VERSION.SDK_INT < 26) return
            runWithRecovery {
                val database = db ?: return@runWithRecovery
                database.listRepositories()
                        .filter { it != "chat" }
                        .map { database.getCollection(it) }
                        .forEach { it.drop() }

                repo.clear()
            }
        }

        @JvmStatic
        private fun initFallbackPrefs() {
            if (Build.VERSION.SDK_INT >= 26 || TranslateDb::fallbackPrefs.isInitialized) return
            fallbackPrefs = LaunchActivity.instance.getSharedPreferences("nitrites_fallback", Context.MODE_PRIVATE)
        }

    }

    fun clear() = synchronized(this) {
        if (Build.VERSION.SDK_INT < 26) return@synchronized
        runWithRecovery { conn?.drop() }
    }

    fun contains(text: String): Boolean = synchronized(this) {
        if (Build.VERSION.SDK_INT < 26) return false
        runWithRecovery {
            (conn?.find(FluentFilter.where("text").eq(text))?.count() ?: 0) > 0
        } ?: false
    }

    fun save(text: String, trans: String) {
        synchronized(this) {
            if (Build.VERSION.SDK_INT < 26) return
            runWithRecovery { conn?.update(TransItem(text, trans), true) }
        }
    }

    fun query(text: String): String? = synchronized(this) {
        if (MomoConfig.ignoreTranslatorCache.Bool() || Build.VERSION.SDK_INT < 26) return null
        runWithRecovery {
            val cursor = conn?.find(FluentFilter.where("text").eq(text))
            var result: String? = null
            cursor?.forEach { result = it?.trans }
            result
        }
    }

}
