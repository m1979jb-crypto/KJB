package kr.co.ucube.avw

import android.content.Context

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("avw", Context.MODE_PRIVATE)
    fun server(c: Context): String = p(c).getString("server", "") ?: ""
    fun setServer(c: Context, v: String) = p(c).edit().putString("server", v).apply()
    fun lastChat(c: Context) = p(c).getInt("last_chat", 0)
    fun lastMail(c: Context) = p(c).getInt("last_mail", 0)
    fun setLast(c: Context, chat: Int, mail: Int) = p(c).edit().putInt("last_chat", chat).putInt("last_mail", mail).apply()
    fun setLastTotal(c: Context, n: Int) = p(c).edit().putInt("last_total", n).apply()
}
