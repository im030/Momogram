package moe.hx030.momogram.transtale

import android.os.Build
import android.text.SpannableStringBuilder
import android.view.View
import org.apache.commons.lang3.LocaleUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.Bulletin
import org.telegram.ui.Components.TranslateAlert2
import moe.hx030.momogram.util.ArrayUtil
import okhttp3.OkHttpClient
import org.apache.commons.lang3.StringUtils
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.cc.CCConverter
import moe.hx030.momogram.cc.CCTarget
import moe.hx030.momogram.parts.translatedTexts
import moe.hx030.momogram.transtale.source.*
import moe.hx030.momogram.ui.PopupBuilder
import moe.hx030.momogram.utils.UIUtil
import moe.hx030.momogram.utils.receive
import moe.hx030.momogram.utils.receiveLazy
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap


val String.code2Locale: Locale by receiveLazy<String, Locale> {
    var ret: Locale
    if (this == null || this.isBlank()) {
        ret = LocaleController.getInstance().currentLocale
    } else {
        val args = replace('-', '_').split('_')

        if (args.size == 1) {
            ret = Locale(args[0])
        } else {
            ret = Locale(args[0], args[1])
        }
    }
    ret
}

val Locale.locale2code by receiveLazy<Locale, String> {

    if (StringUtils.isBlank(country)) {
        language
    } else {
        "$language-$country"
    }

}

val LocaleController.LocaleInfo.locale by receiveLazy<LocaleController.LocaleInfo, Locale> { pluralLangCode.code2Locale }

val Locale.transDb by receive<Locale, TranslateDb?> {
    if (Build.VERSION.SDK_INT < 26) return@receive null

    TranslateDb.repo[this] ?: TranslateDb(locale2code).also {

        TranslateDb.repo[this] = it

    }

}

val String.transDbByCode by receive<String, TranslateDb?> { code2Locale.transDb }

interface Translator {

    suspend fun doTranslate(from: String, to: String, query: String): String

    companion object {

        val httpClient = OkHttpClient()

        @Throws(Exception::class)
        suspend fun translate(query: String) = translate(
            MomoConfig.translateToLang.String()?.code2Locale
                ?: LocaleController.getInstance().currentLocale, query)

        const val providerGoogle = 1
        const val providerGoogleCN = 2
        const val providerYandex = 3
        const val providerLingo = 4
        const val providerMicrosoft = 5
        const val providerYouDao = 6
        const val providerDeepLX = 7
        const val providerTelegram = 8
        const val providerLingva = 9
        const val providerFirefox = 10

        @Throws(Exception::class)
        suspend fun translate(to: Locale, query: String): String {

            var language = to.language
            var country = to.country

            if (language == "in") language = "id"
            if (country.lowercase() == "duang") country = "CN"

            val provider = MomoConfig.translationProvider.Int()
            when (provider) {
                providerYouDao -> if (language == "zh") {
                    language = "zh-CHS"
                }
                providerDeepLX -> language = language.uppercase()
                providerMicrosoft,
                providerGoogle,
                providerGoogleCN -> if (language == "zh") {
                    val countryUpperCase = country.uppercase()
                    if (countryUpperCase == "CN" || countryUpperCase == "DUANG") {
                        language = if (provider == providerMicrosoft) "zh-Hans" else "zh-CN"
                    } else if (countryUpperCase == "TW" || countryUpperCase == "HK") {
                        language = if (provider == providerMicrosoft) "zh-HanT" else "zh-TW"
                    }
                }

            }
            val translator = when (provider) {
                providerGoogle, providerGoogleCN -> GoogleAppTranslator
                providerYandex -> YandexTranslator
                providerLingo -> LingoTranslator
                providerMicrosoft -> MicrosoftTranslator
                providerYouDao -> YouDaoTranslator
                providerDeepLX -> DeepLXTranslator
                providerTelegram -> TelegramAPITranslator
                providerLingva -> LingvaTranslator
                providerFirefox -> FirefoxLocalTranslator
                else -> throw IllegalArgumentException()
            }

            // FileLog.d("[Trans] use provider ${translator.javaClass.simpleName}, toLang: $toLang, query: $query")

            val result = translator.doTranslate("auto", language, query).also {
                if (Build.VERSION.SDK_INT < 26) {
                    translatedTexts.computeIfAbsent(to) { HashMap() }[query] = it
                } else {
                    to.transDb?.save(query, it)
                }

            }

            if (language == "zh") {
                val countryUpperCase = country.uppercase()
                if (countryUpperCase == "CN") {
                    return CCConverter.get(CCTarget.SP).convert(result)
                } else if (countryUpperCase == "TW") {
                    return CCConverter.get(CCTarget.TT).convert(result)
                }
            }

            return result

        }

        @JvmStatic
        @JvmOverloads
        fun showTargetLangSelect(anchor: View, input: Boolean = false, full: Boolean = false, callback: (Locale) -> Unit) {

            val builder = PopupBuilder(anchor)

            var locales = (if (full) LocaleUtils.availableLocaleList().filter { it.variant.isBlank() }
                    else LocaleController.getInstance().languages
                    .map { it.pluralLangCode }
                    .toSet()
                    .filter { !it.lowercase().contains("duang") }
                    .filter { MomoConfig.preferredTranslateTargetLangList.isEmpty() ||
                                MomoConfig.preferredTranslateTargetLangList.contains(it.lowercase()) }
                    .map { it.code2Locale })
                    .toTypedArray()

            val currLocale = LocaleController.getInstance().currentLocale

            for (i in locales.indices) {

                val defLang = if (!input) currLocale else Locale.ENGLISH

                if (locales[i] == defLang) {

                    locales = ArrayUtil.remove(locales, i) as Array<Locale>
                    locales = ArrayUtil.insert(locales, 0, defLang) as Array<Locale>

                    break

                }

            }

            val localeNames = arrayOfNulls<String>(if (full) locales.size else locales.size + 1)

            for (i in locales.indices) {

                localeNames[i] = if (!full && i == 0) {

                    LocaleController.getString(R.string.Default) + " ( " + locales[i].getDisplayName(currLocale) + " )"

                } else {

                    locales[i].getDisplayName(currLocale)

                }

            }

            if (!full) {

                localeNames[localeNames.size - 1] = LocaleController.getString(R.string.More)

            }

            builder.setItems(localeNames.filterIsInstance<CharSequence>().toTypedArray()) { index: Int, _ ->

                if (index == locales.size) {

                    showTargetLangSelect(anchor, input, true, callback)

                } else {

                    callback(locales[index])

                }

            }

            builder.show()

        }

        @JvmStatic
        @JvmOverloads
        fun showCCTargetSelect(anchor: View, input: Boolean = true, callback: (String) -> Unit) {

            val builder = PopupBuilder(anchor)

            builder.setItems(arrayOf(
                    if (!input) LocaleController.getString(R.string.CCNo) else null,
                    LocaleController.getString(R.string.CCSC),
                    LocaleController.getString(R.string.CCSP),
                    LocaleController.getString(R.string.CCTC),
                    LocaleController.getString(R.string.CCHK),
                    LocaleController.getString(R.string.CCTT),
                    LocaleController.getString(R.string.CCJP)
            )) { index: Int, _ ->
                callback(when (index) {
                    1 -> CCTarget.SC.name
                    2 -> CCTarget.SP.name
                    3 -> CCTarget.TC.name
                    4 -> CCTarget.HK.name
                    5 -> CCTarget.TT.name
                    6 -> CCTarget.JP.name
                    else -> ""
                })
            }

            builder.show()

        }

        @JvmStatic
        @JvmOverloads
        fun translate(to: Locale = MomoConfig.translateToLang.String()?.code2Locale
                ?: LocaleController.getInstance().currentLocale, query: String, translateCallBack: TranslateCallBack) {

            UIUtil.runOnIoDispatcher {

                runCatching {

                    val result = translate(to, query)

                    UIUtil.runOnUIThread(Runnable {

                        translateCallBack.onSuccess(result)

                    })

                }.onFailure {

                    translateCallBack.onFailed(it is UnsupportedOperationException, it.message
                            ?: it.javaClass.simpleName)

                }

            }

        }

        interface TranslateCallBack {

            fun onSuccess(translation: String)
            fun onFailed(unsupported: Boolean, message: String)

        }

        var transReqId: Int? = null

        @JvmStatic
        fun translateMessageBeforeSent(currentAccount: Int, text: CharSequence?) {
            translateMessageBeforeSent(currentAccount, text, "en")
        }

        @JvmStatic
        fun translateMessageBeforeSent(currentAccount: Int, text: CharSequence?, targetLang: String) {
            translateMessageBeforeSent(currentAccount, text, targetLang, true, null)
        }

        @JvmStatic
        fun translateMessageBeforeSent(currentAccount: Int, text: CharSequence?, targetLang: String, isSelfOutgoingMessage: Boolean, maybeChatActivity: ChatActivity?, uuid: String? = null) {
            if (transReqId != null) {
                ConnectionsManager.getInstance(currentAccount).cancelRequest(transReqId!!, true)
                transReqId = null
            }

            var localIsSelfOutgoing = isSelfOutgoingMessage
            val pendingCount = AtomicInteger(0)
            val posted = AtomicBoolean(false)
            var result = "" as CharSequence
            fun postNotification() {
                val notiType = if (localIsSelfOutgoing) NotificationCenter.outgoingMessageTranslated
                                else NotificationCenter.forwardingMessageTranslated
                AndroidUtilities.runOnUIThread {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(notiType, result, uuid)
                }
            }
            if (!isSelfOutgoingMessage) {
                val chatActivity = maybeChatActivity!!
                if (chatActivity.messagePreviewParams == null) {
                    FileLog.d("030-tx: not forwarding, fix state")
                    localIsSelfOutgoing = true
                } else if (chatActivity.messagePreviewParams.forwardMessages == null || chatActivity.messagePreviewParams.forwardMessages.messages == null) {
                    FileLog.d("030-tx: forwardMessages is null, fix state")
                    localIsSelfOutgoing = true
                } else {
                    chatActivity.messagePreviewParamsForTranslate = chatActivity.messagePreviewParams

                    val target = ArrayList<MessageObject>()
                    chatActivity.messagePreviewParams.forwardMessages.getSelectedMessages(target)
                    target.forEach {
                        val media = it.messageOwner?.media
                        val isMedia = media != null && media !is TLRPC.TL_messageMediaWebPage
                        var isMsgText = false
                        var targetText = it.caption
                        if (targetText == null || targetText.trim().isBlank()) {
                            if (isMedia) {
                                FileLog.d("030-tx: media without caption")
                                return@forEach
                            }
                            isMsgText = true
                            targetText = it.messageText
                        }

                        // stickers can't carry a translated caption
                        if (targetText.isNullOrBlank() || it.isAnyKindOfSticker()) {
                            FileLog.d("030-tx: null text or sticker")
                            return@forEach
                        }

                        pendingCount.incrementAndGet()
                        doTranslateWithOfficialApi(currentAccount, targetText, targetLang, { result ->
                            // Log.d("030-tx", "fwd: $text -> $result")
                            if (isMsgText) it.messageText = result
                            else it.caption = result
                            if (pendingCount.decrementAndGet() == 0 && posted.compareAndSet(false, true)) {
                                postNotification()
                            }
                        }, {
                            // Log.d("030-tx", "fwd: $text -> <FAILED>")
                            if (pendingCount.decrementAndGet() == 0 && posted.compareAndSet(false, true)) {
                                postNotification()
                            }
                        })
                    }
                }
            }

            fun maybePost() {
                if (pendingCount.decrementAndGet() == 0 && posted.compareAndSet(false, true)) {
                    postNotification()
                }
            }

            if (text.isNullOrBlank()) {
                if (pendingCount.get() == 0 && posted.compareAndSet(false, true)) {
                    postNotification()
                }
                return
            }

            pendingCount.incrementAndGet()
            doTranslateWithOfficialApi(currentAccount, text, targetLang, {
                result = it
                maybePost()
            }, {
                if (localIsSelfOutgoing) {
                    FileLog.d("030-tx: showing err toast")
                    AndroidUtilities.runOnUIThread {
                        NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.showBulletin,
                            Bulletin.TYPE_ERROR_SUBTITLE,
                            LocaleController.getString(R.string.TranslationFailedAlert2),
                            it.replaceFirst(Regex(" "), "")
                        )
                    }
                }
                result = it
                maybePost()
            })

        }

        var translationDisabled = false

        @JvmStatic
        fun doTranslateWithOfficialApi(currentAccount: Int, text: CharSequence?, targetLang: String, onSuccess: ((CharSequence) -> Unit)?, onFailure: ((CharSequence) -> Unit)?) {
            if (text.isNullOrBlank()) return
            if (translationDisabled) {
                useAlternativeTranslate(text, targetLang, onSuccess, onFailure)
                return
            }

            val req: TLRPC.TL_messages_translateText = TLRPC.TL_messages_translateText()
            val textWithEntities: TLRPC.TL_textWithEntities = TLRPC.TL_textWithEntities()
            textWithEntities.text = text.toString()
            req.flags = req.flags or 2
            req.text.add(textWithEntities)
            req.to_lang = targetLang
            transReqId = ConnectionsManager.getInstance(currentAccount).sendRequest(
                req
            ) { res: TLObject?, err: TLRPC.TL_error? ->
                AndroidUtilities.runOnUIThread {
                    transReqId = null
                    if (res != null && (res is TLRPC.TL_messages_translateResult &&
                                !(res as TLRPC.TL_messages_translateResult).result.isEmpty()) &&
                        (res as TLRPC.TL_messages_translateResult).result.get(0) != null &&
                        ((res as TLRPC.TL_messages_translateResult).result.get(0).text != null)
                    ) {
                        val processedText: TLRPC.TL_textWithEntities = TranslateAlert2.preprocess(
                            textWithEntities, (res as TLRPC.TL_messages_translateResult).result.get(0))
                        val translated: CharSequence? = SpannableStringBuilder.valueOf(processedText.text)
                        MessageObject.addEntitiesToText(
                            translated,
                            processedText.entities,
                            false,
                            true,
                            false,
                            false
                        )

                        if (translated != null) {
                            if (onSuccess != null) {
                                onSuccess(translated)
                            }
                        } else {
                            throw NullPointerException("null translated string")
                        }
                    } else {
                        FileLog.d("030-tx: response exists? ${res != null}")
                        var errMsg = " NULL"
                        if (err != null) {
                            errMsg = " ${err.code} - ${err.text}"
                            FileLog.e("030-tx $errMsg")

                            if (err.code == 406 || err.text == "TRANSLATIONS_DISABLED") {
                                translationDisabled = true
                                useAlternativeTranslate(text, targetLang, onSuccess, onFailure)
                                return@runOnUIThread
                            }
                        }

                        if (onFailure != null) {
                            onFailure(errMsg)
                        }
                    }
                }
            }
        }

        private fun useAlternativeTranslate(text: CharSequence?, targetLang: String, onSuccess: ((CharSequence) -> Unit)?, onFailure: ((CharSequence) -> Unit)?) {
            TranslateAlert2.alternativeTranslate(text.toString(), "auto", targetLang) { result, rateLimit ->
                AndroidUtilities.runOnUIThread {
                    if (result != null) {
                        onSuccess?.invoke(result)
                    } else {
                        onFailure?.invoke(LocaleController.getString(if (rateLimit) R.string.TranslationFailedAlert1 else R.string.TranslationFailedAlert2))
                    }
                }
            }
        }

        fun maybeStripCOT(result: String?): String? {
            if (result == null || !MomoConfig.trimCOTFromTranslateResult.Bool() ||
                (MomoConfig.translationProvider.Int() != providerTelegram)) {
                return result
            }
            val rebuild = StringBuilder()
            val spl = result.split("\n")
            for (i in 0..<spl.size) {
                val line = spl[i]
                if (line.trimStart().startsWith("[COT] ")) break
                if (i != 0) rebuild.append("\n")
                rebuild.append(line)
            }
            return rebuild.toString()
        }
    }

}
