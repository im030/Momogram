@file:Suppress("UNCHECKED_CAST")

package moe.hx030.momogram.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.setPadding
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import moe.hx030.momogram.MomoConfig
import org.telegram.messenger.*
import org.telegram.messenger.browser.Browser
import org.telegram.utils.proxy.ProxySettings
import moe.hx030.momogram.ui.BottomBuilder
import moe.hx030.momogram.utils.AlertUtil.showToast
import java.io.File
import java.util.*
import androidx.core.graphics.set


object ProxyUtil {

    @JvmStatic
    fun importProxy(ctx: Activity, link: String): Boolean {
        runCatching {
            val settings = ProxySettings.fromUri(Uri.parse(link)) ?: return false
            if (!settings.isValid) return false
            AndroidUtilities.showProxyAlert(ctx, settings)
            return true
        }.onFailure {
            FileLog.e(it)
            if (BuildVars.LOGS_ENABLED) {
                AlertUtil.showSimpleAlert(ctx, it)
            } else {
                showToast("${LocaleController.getString(R.string.BrokenLink)}: ${it.message}")
            }

        }

        return false

    }

    @JvmStatic
    fun shareProxy(ctx: Activity, info: SharedConfig.ProxyInfo, type: Int) {

        val url = info.link;

        if (type == 1) {

            AndroidUtilities.addToClipboard(url)

            Toast.makeText(ctx, LocaleController.getString(R.string.LinkCopied), Toast.LENGTH_LONG).show()

        } else if (type == 0) {

            val shareIntent = Intent(Intent.ACTION_SEND)

            shareIntent.type = "text/plain"

            shareIntent.putExtra(Intent.EXTRA_TEXT, url)

            val chooserIntent = Intent.createChooser(shareIntent, LocaleController.getString(R.string.ShareLink))

            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            ctx.startActivity(chooserIntent)

        } else {

            showQrDialog(ctx, url)

        }

    }

    @JvmStatic
    fun getOwnerActivity(ctx: Context): Activity {

        if (ctx is Activity) return ctx

        if (ctx is ContextWrapper) return getOwnerActivity(ctx.baseContext)

        error("unable cast ${ctx.javaClass.name} to activity")

    }

    @JvmStatic
    @JvmOverloads
    fun showQrDialog(ctx: Context, text: String, icon: ((Int) -> Bitmap)? = null): AlertDialog {

        val code = createQRCode(text, icon = icon)

        ctx.setTheme(R.style.Theme_TMessages)

        return AlertDialog.Builder(ctx).setView(LinearLayout(ctx).apply {

            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)

            addView(LinearLayout(ctx).apply {
                val root = this

                gravity = Gravity.CENTER
                setBackgroundColor(Color.WHITE)
                setPadding(AndroidUtilities.dp(16f))

                val width = AndroidUtilities.dp(260f)

                addView(ImageView(ctx).apply {

                    setImageBitmap(code)

                    scaleType = ImageView.ScaleType.FIT_XY

                    setOnLongClickListener {

                        val builder = BottomBuilder(ctx)

                        builder.addItems(arrayOf(

                                LocaleController.getString(R.string.SaveToGallery),
                                LocaleController.getString(R.string.Cancel)

                        ), intArrayOf(

                                R.drawable.baseline_image_24,
                                R.drawable.baseline_cancel_24

                        )) { i, _, _ ->

                            if (i == 0) {

                                if (Build.VERSION.SDK_INT >= 23 && ctx.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                                    getOwnerActivity(ctx).requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 4)

                                    return@addItems

                                }

                                val saveTo = File(Environment.getExternalStorageDirectory(),
                                    "${if (MomoConfig.alwaysSaveToDownloads.Bool()) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES}/share_${text.hashCode()}.jpg")

                                saveTo.parentFile?.mkdirs()

                                runCatching {

                                    saveTo.createNewFile()

                                    saveTo.outputStream().use {

                                        loadBitmapFromView(root).compress(Bitmap.CompressFormat.JPEG, 100, it);

                                    }

                                    AndroidUtilities.addMediaToGallery(saveTo.path)
                                    showToast(LocaleController.getString(R.string.PhotoSavedHint))

                                }.onFailure {
                                    FileLog.e(it)
                                    showToast(it)
                                }

                            }

                        }

                        builder.show()

                        return@setOnLongClickListener true

                    }

                }, LinearLayout.LayoutParams(width, width))

            }, LinearLayout.LayoutParams(-2, -2).apply {

                gravity = Gravity.CENTER

            })

        }).create().apply {

            show()
            window?.setBackgroundDrawableResource(android.R.color.transparent)

        }

    }

    private fun loadBitmapFromView(v: View): Bitmap {
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    @JvmStatic
    fun createQRCode(text: String, size: Int = 768, icon: ((Int) -> Bitmap)? = null): Bitmap {
        return try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            val canvas = Canvas(bitmap)

            val iconSize = 180

            val dst = Rect(
                (size - iconSize) / 2,
                (size - iconSize) / 2,
                (size + iconSize) / 2,
                (size + iconSize) / 2
            )

            if (icon != null) canvas.drawBitmap(icon(size), dst, dst, Paint(Paint.ANTI_ALIAS_FLAG))
            return bitmap
        } catch (e: WriterException) {
            FileLog.e(e);
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }
    }

    val qrReader = QRCodeReader()

    @JvmStatic
    fun tryReadQR(ctx: Activity, bitmap: Bitmap) {

        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)

        try {

            val result = try {
                qrReader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), mapOf(
                        DecodeHintType.TRY_HARDER to true
                ))
            } catch (e: NotFoundException) {
                qrReader.decode(BinaryBitmap(GlobalHistogramBinarizer(source.invert())), mapOf(
                        DecodeHintType.TRY_HARDER to true
                ))
            }

            showLinkAlert(ctx, result.text)

        } catch (e: Throwable) {

            showToast(LocaleController.getString(R.string.NoQrFound))

        }

    }

    @JvmStatic
    @JvmOverloads
    fun showLinkAlert(ctx: Activity, text: String, tryInternal: Boolean = true) {

        val builder = BottomBuilder(ctx)

        if (tryInternal) {
            runCatching {
                if (Browser.isInternalUrl(text, booleanArrayOf(false))) {
                    Browser.openUrl(ctx, text)
                    return
                }
            }
        }

        builder.addTitle(text)

        builder.addItems(arrayOf(
                LocaleController.getString(R.string.Open),
                LocaleController.getString(R.string.Copy),
                LocaleController.getString(R.string.ShareQRCode)
        ), intArrayOf(
                R.drawable.baseline_open_in_browser_24,
                R.drawable.baseline_content_copy_24,
                R.drawable.wallet_qr
        )) { which, _, _ ->
            when (which) {
                0 -> Browser.openUrl(ctx, text)
                1 -> {
                    AndroidUtilities.addToClipboard(text)
                    showToast(LocaleController.getString(R.string.LinkCopied))
                }

                else -> showQrDialog(ctx, text)
            }
        }

        builder.show()

    }

}
