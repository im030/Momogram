package moe.hx030.momogram.utils

import android.os.Build
import android.text.TextUtils
import kotlinx.coroutines.runBlocking
import moe.hx030.momogram.utils.FileUtil.delete
import moe.hx030.momogram.utils.FileUtil.initDir
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.LocaleController.LocaleInfo
import java.io.File
import java.util.Locale

object LocaleUtil {

    @JvmField
    val cacheDir = File(ApplicationLoader.applicationContext.cacheDir, "builtIn_lang_export")

    @JvmStatic
    fun fetchAndExportLang() = runBlocking {

        delete(cacheDir)
        initDir(cacheDir)

        for (localeInfo in LocaleController.getInstance().languages) {

            if (!localeInfo.builtIn || localeInfo.pathToFile != "unofficial") continue

            if (localeInfo.hasBaseLang()) {

                localeInfo.pathToBaseFile.takeIf { it.isFile }?.copyTo(File(cacheDir, localeInfo.pathToBaseFile.name))

            }

            localeInfo.getPathToFile()?.takeIf { it.isFile }?.copyTo(File(cacheDir, localeInfo.getPathToFile().name))

        }


    }

    @JvmStatic
    fun getFixedLocale(
        localeInfo: LocaleInfo,
        packLanguageCode: String?
    ): Locale {
        val args: Array<String?>?
        if (!TextUtils.isEmpty(localeInfo.pluralLangCode)) {
            args = localeInfo.pluralLangCode.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else if (!TextUtils.isEmpty(localeInfo.baseLangCode)) {
            args = localeInfo.baseLangCode.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else {
            args = localeInfo.shortName.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }
        var locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                    (if (args.size == 1) Locale.of(args[0]!!) else Locale.of(args[0]!!, args[1]!!))
            else
                    (if (args.size == 1) Locale(args[0]!!) else Locale(args[0]!!, args[1]!!))

        val tag = if (!TextUtils.isEmpty(packLanguageCode)) packLanguageCode else localeInfo.shortName

        // The composite code may carry a script subtag or more than a language and a region, so it
        // has to be parsed as a whole BCP 47 tag. Keeping only the first two underscore separated
        // parts turned zh_Hant_TW and zh_Hant into the country "HANT", and because
        // Locale.toLanguageTag() drops an unknown country the tag silently degraded to a bare "zh",
        // which the string tables answer with zh_cn - Simplified. Note this branch must not be
        // limited to single part codes, that is exactly where the truncation used to bite.
        if (!TextUtils.isEmpty(tag) && !TextUtils.isEmpty(args[0]) &&
            (tag!!.indexOf('_') > 0 || tag.indexOf('-') > 0)
        ) {
            val parsed = Locale.forLanguageTag(tag.replace('_', '-'))

            if (!TextUtils.isEmpty(parsed.language) && args[0].equals(parsed.language, ignoreCase = true)) {
                val builder = Locale.Builder().setLanguage(parsed.language)
                if (parsed.script.length == 4) {
                    builder.setScript(parsed.script)
                }
                if (parsed.country.length == 2 || parsed.country.all { it.isDigit() }) {
                    builder.setRegion(parsed.country)
                }
                locale = builder.build()
            }
        }
        return fixChineseLocale(locale)
    }

    @JvmStatic
    fun fixChineseLocale(loc: Locale): Locale {
        if (!"zh".equals(loc.language)) {
            return loc;
        }
        val region = loc.country;
        val script = loc.script.lowercase()
        // A code that was split by "_" and rebuilt through new Locale() can end up with a script
        // subtag sitting in the country slot ("HANT"/"HANS"), which no region check above matches.
        val scriptInCountry = if (region.length == 4) region.lowercase() else script
        if ("TW" == region) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Locale.of("zh", "TW")
            } else {
                Locale("zh", "TW")
            }
        }
        if ("HK" == region || "MO".equals(region)) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Locale.of("zh", "HK")
            } else {
                Locale("zh", "HK")
            }
        }
        if ("hant" == scriptInCountry) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Locale.of("zh", "TW")
            } else {
                Locale("zh", "TW")
            }
        }
        if ("hans" == scriptInCountry) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Locale.of("zh", "CN")
            } else {
                Locale("zh", "CN")
            }
        }
        return loc;
    }

    @JvmStatic
    fun getSystemDefaultLocaleInfo(fallback: LocaleInfo?): LocaleInfo? {
        val controller = LocaleController.getInstance()
        val locale: Locale? = controller.systemDefaultLocale
        var info: LocaleInfo? = null
        if (locale != null) {
            info = controller.getLanguageFromDict(controller.getLocaleString(locale))
            if (info == null && "zh" == locale.language) {
                val parsed = Locale.forLanguageTag(controller.getLocaleString(locale).replace('_', '-'))
                if ("hant" == parsed.script.lowercase()) {
                    for (key in arrayOf(
                        locale.language + "_hant_" + parsed.country.lowercase(Locale.getDefault()),
                        locale.language + "_hant",
                        locale.language + "_" + parsed.country.lowercase(Locale.getDefault())
                    )) {
                        info = controller.getLanguageFromDict(key)
                        if (info != null) {
                            break
                        }
                    }
                }
            }
        }
        if (info == null && locale != null && !TextUtils.isEmpty(locale.language)) {
            info = controller.getLanguageFromDict(locale.language)
        }
        if (info != null && info !== fallback) {
            return info
        }
        return fallback ?: controller.getLanguageFromDict("en")
    }

}