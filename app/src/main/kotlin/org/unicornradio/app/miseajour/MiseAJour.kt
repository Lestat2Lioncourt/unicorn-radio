package org.unicornradio.app.miseajour

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.fichier.Fichiers
import java.net.HttpURLConnection
import java.net.URL

/**
 * La mise à jour proposée : la dernière release du dépôt GitHub, lue par
 * l'API publique sans compte, comparée à la version installée. Au plus une
 * vérification par jour d'elle-même ; « Plus tard » compte comme une
 * vérification, on redemande le lendemain. Tout ici bloque : à appeler
 * hors du fil principal, sauf les préférences.
 */
class MiseAJour(contexte: Context) {

    /** Une release : son numéro sans le `v`, sa note telle que publiée, l'APK joint s'il y en a un, et sa page. */
    class Version(val numero: String, val note: String, val apk: String?, val page: String)

    class GitHubInjoignable(message: String) : Exception(message)

    private val prefs = contexte.getSharedPreferences("mise_a_jour", Context.MODE_PRIVATE)

    /** Vrai si la dernière vérification date de plus d'un jour, ou n'a jamais eu lieu. */
    fun aVerifier(): Boolean = System.currentTimeMillis() - prefs.getLong("verifiee_le", 0L) > UN_JOUR_MS

    fun noterLaVerification() {
        prefs.edit().putLong("verifiee_le", System.currentTimeMillis()).apply()
    }

    /** La dernière release publiée, ou `null` s'il n'y en a aucune. */
    fun derniere(): Version? {
        val c = URL(DERNIERE_RELEASE).openConnection() as HttpURLConnection
        c.connectTimeout = DELAI_MS
        c.readTimeout = DELAI_MS
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            val code = try { c.responseCode } catch (e: Exception) { throw GitHubInjoignable(e.message ?: e.javaClass.simpleName) }
            if (code == 404) return null
            if (code != 200) throw GitHubInjoignable("HTTP $code")
            val o = JSONObject(c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val etiquette = o.optString("tag_name")
            if (etiquette.isBlank()) return null
            var apk: String? = null
            val pieces = o.optJSONArray("assets")
            if (pieces != null) for (i in 0 until pieces.length()) {
                val p = pieces.optJSONObject(i) ?: continue
                if (p.optString("name").endsWith(".apk")) { apk = p.optString("browser_download_url").takeIf { it.isNotBlank() }; break }
            }
            return Version(etiquette.removePrefix("v"), o.optString("body"), apk, o.optString("html_url").ifBlank { PAGE_DES_RELEASES })
        } finally {
            c.disconnect()
        }
    }

    companion object {
        const val PAGE_DES_RELEASES = "https://github.com/Lestat2Lioncourt/unicorn-radio/releases"
        private const val DERNIERE_RELEASE = "https://api.github.com/repos/Lestat2Lioncourt/unicorn-radio/releases/latest"
        private const val USER_AGENT = "Unicorn-Radio/${BuildConfig.VERSION_NAME} (https://github.com/Lestat2Lioncourt/unicorn-radio)"
        private const val DELAI_MS = 10_000
        private const val UN_JOUR_MS = 24 * 3_600_000L

        /** Vrai si `a` est plus récente que `b`, chiffre par chiffre : 1.0.0 après 0.10.0, 0.10.0 après 0.9.3. */
        fun plusRecente(a: String, b: String): Boolean {
            val x = chiffres(a)
            val y = chiffres(b)
            for (i in 0 until maxOf(x.size, y.size)) {
                val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
                if (d != 0) return d > 0
            }
            return false
        }

        private fun chiffres(v: String): List<Int> =
            v.trim().removePrefix("v").split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

        /**
         * La partie de la note dans une langue : la note publiée est le fichier
         * `notes-de-version`, une section `[FR]` puis une section `[EN]`, le titre
         * sur la ligne de l'étiquette, une ligne par apport. Rendue avec le titre
         * en tête et une puce par ligne ; sans section pour la langue, la note
         * entière.
         */
        fun noteDans(note: String, langue: String): String {
            val etiquette = "[" + langue.uppercase() + "]"
            val lignes = note.lines()
            val debut = lignes.indexOfFirst { it.trimStart().startsWith(etiquette) }
            if (debut < 0) return note.trim()
            val section = ArrayList<String>()
            section += lignes[debut].trimStart().removePrefix(etiquette).trim()
            for (l in lignes.drop(debut + 1)) {
                val t = l.trim()
                if (t.startsWith("[") && t.length >= 4 && t[3] == ']') break
                if (t.isNotEmpty()) section += "• $t"
            }
            return section.joinToString("\n")
        }
    }
}

/** Journal, pour les traces de ce paquet. */
internal fun tracer(message: String) = Log.i(Fichiers.JOURNAL, "mise à jour : $message")
