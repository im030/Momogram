package moe.hx030.momogram.utils

import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.TextView
import org.apache.commons.lang3.StringUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.URLSpanNoUnderline
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.config.ConfigItem
import java.util.TreeSet
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.random.Random

object StrUtil {

    private val urlPattern = Pattern.compile("@[a-zA-Z\\d_]{1,32}")
    private val groupVarPattern = Pattern.compile("\\$(\\d+)")
    private val RE_KEYS = setOf('$', '(', ')', '*', '+', '.', '[', ']', '?', '\\', '^', '{', '}', '|')

    @JvmStatic
    fun setText(fragment: BaseFragment?, textView: TextView, text: String) {

        var stringBuilder: SpannableStringBuilder? = null

        if (fragment != null) {

            try {
                val matcher: Matcher = urlPattern.matcher(text)
                while (matcher.find()) {
                    if (stringBuilder == null) {
                        stringBuilder = SpannableStringBuilder(text)
                        textView.movementMethod = AndroidUtilities.LinkMovementMethodMy()
                    }
                    var start = matcher.start()
                    val end = matcher.end()
                    if (text.get(start) != '@') {
                        start++
                    }
                    val url: URLSpanNoUnderline = object : URLSpanNoUnderline(text.subSequence(start + 1, end).toString()) {
                        override fun onClick(widget: View) {
                            fragment.messagesController.openByUserName(url, fragment, 1)
                        }
                    }
                    stringBuilder.setSpan(url, start, end, 0)
                }
            } catch (e: Exception) {
                FileLog.e(e)
            }
        }

        textView.text = stringBuilder ?: text
    }

    @JvmStatic
    @JvmOverloads
    fun getSubString(text: String, left: String?, right: String?, lastOrFirst: Boolean = false): String {
        val llen = if (left.isNullOrEmpty()) {
            0
        } else {
            val i = if (lastOrFirst) text.lastIndexOf(left) else text.indexOf(left)
            if (i >= 0) i + left.length else 0
        }

        val rlen = if (right.isNullOrEmpty()) {
            text.length
        } else {
            val i = if (lastOrFirst) text.lastIndexOf(right) else text.indexOf(right, llen)
            if (i >= 0) i else text.length
        }

        val ret = text.substring(llen, rlen)
        return ret
    }

    @JvmStatic
    fun isInteger(str: String): Boolean {
        return str.toIntOrNull() != null
    }

    @JvmStatic
    fun replaceAllRegex(
        content: CharSequence,
        patternStr: String,
        replacementTemplate: String?
    ): String {
        if (StringUtils.isEmpty(content)) {
            return ""
        }

        val pattern = Pattern.compile(patternStr, Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        var result = matcher.find()
        if (result) {
            val groupVarMatcher = replacementTemplate?.let { groupVarPattern.matcher(it) }
            val varNums = TreeSet(Comparator<CharSequence> { a, b ->
                var ret = b.length.compareTo(a.length)
                if (ret == 0) {
                    ret = b.toString().compareTo(a.toString())
                }
                ret
            })
            if (groupVarMatcher != null) {
                while (groupVarMatcher.find()) {
                    val groupStr = groupVarMatcher.group(1)
                    if (!groupStr.isNullOrEmpty()) {
                        varNums.add(groupStr)
                    }
                }
            }

            val sb = StringBuffer()
            do {
                var replacement = replacementTemplate
                for (`var` in varNums) {
                    val group = `var`.toString().toInt()
                    replacement = matcher.group(group)?.let { replacement!!.replace("$$`var`", it) }
                }
                replacement?.let { escape(it) }?.let { matcher.appendReplacement(sb, it) }
                result = matcher.find()
            } while (result)
            matcher.appendTail(sb)
            return sb.toString()
        }
        return content.toString()
    }

    fun escape(content: String): String {
        if (StringUtils.isBlank(content)) {
            return content
        }

        val builder = StringBuilder()
        val len = content.length
        var current: Char
        for (i in 0..<len) {
            current = content[i]
            if (RE_KEYS.contains(current)) {
                builder.append('\\')
            }
            builder.append(current)
        }
        return builder.toString()
    }

    @JvmStatic
    fun getSimpleUUID(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    @JvmStatic
    fun isRTLString(str: String?): Boolean {
        if (str == null) {
            return false
        }

        for (i in str.indices) {
            val c = str.codePointAt(i)
            val directionality = Character.getDirectionality(c)
            if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
            ) {
                return true
            }
        }

        return false
    }

    @JvmStatic
    fun firstCharUpper(str: String?): String? {
        if (str == null || StringUtils.isBlank(str)) return str
        return "${str[0].uppercaseChar()}${str.substring(1)}"
    }

    @JvmStatic
    fun get030Tag(obj: Any): String {
        return "030-${obj.javaClass.simpleName}"
    }

    val appNames = listOf(
        LocaleController.getString(R.string.NekoX),
        LocaleController.getString(R.string.AppNameShort),
        LocaleController.getString(R.string.Momogram),
    )

    @JvmStatic
    fun getAppName(): String {
        if (MomoConfig.useOldName.Bool())
            return appNames[0]

        return appNames[2]
    }

    @JvmStatic
    fun getShortAppName(): String {
        if (MomoConfig.useOldName.Bool())
            return appNames[1]

        return appNames[2]
    }

    @JvmStatic
    fun isAppName(s: String): Boolean {
        return appNames.contains(s)
    }

    @JvmStatic
    fun appendToCSConfigString(cfg: ConfigItem, s: Collection<String>) {
        val orig = cfg.String()
        if (orig.isNullOrBlank()) cfg.setConfigString(s.joinToString(","))
        else cfg.setConfigString("$orig,${s.joinToString(",")}")
    }

    @JvmStatic
    fun randomizeFileName(n: String): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val dot = n.lastIndexOf('.')
        val ext = if (dot != -1) n.substring(dot) else ""
        if (!MomoConfig.randomizeFilenameOnSend.Bool() || n.endsWith(".m0m0-crash.txt")) {
            return n
        }

        val random = (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")

        return random + ext
    }

}