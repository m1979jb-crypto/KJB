package kr.co.ucube.avw

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 15분마다 서버에 읽지 않은 메신저·메일 수를 물어 새 것이 있으면 휴대폰 알림을 띄운다. */
class NotifyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val base = Prefs.server(ctx).trimEnd('/')
        if (base.isBlank()) return Result.success()
        val cookie = android.webkit.CookieManager.getInstance().getCookie(base) ?: return Result.success()
        return try {
            val con = (URL("$base/api/chat/poll?v=0&t=0").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 15000
                setRequestProperty("Cookie", cookie)
                setRequestProperty("User-Agent", "AVWApp/2.0")
            }
            if (con.responseCode != 200) return Result.success()
            val j = JSONObject(con.inputStream.bufferedReader().readText())
            val chat = j.optInt("chat_unread"); val mail = j.optInt("mail_unread")
            if (chat > Prefs.lastChat(ctx)) notify(ctx, 1, "💬 새 메시지", "읽지 않은 메시지 ${chat}개", "#/messenger")
            if (mail > Prefs.lastMail(ctx)) notify(ctx, 2, "✉️ 새 메일", "읽지 않은 메일 ${mail}통", "#/mail")
            Prefs.setLast(ctx, chat, mail)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notify(ctx: Context, id: Int, title: String, text: String, route: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val pi = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java).putExtra("route", route).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title).setContentText(text)
            .setColor(0xFF3182F6.toInt())
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    companion object {
        const val CHANNEL = "avw_messages"
        fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "메신저·메일 알림", NotificationManager.IMPORTANCE_DEFAULT))
            }
        }
    }
}
