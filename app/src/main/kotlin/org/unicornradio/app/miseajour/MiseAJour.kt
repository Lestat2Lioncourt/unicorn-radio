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
 * l'API publique sans compte, comparée à la version installée. Vérifiée à
 * chaque retour de l'écran au premier plan, sans un mot si rien n'est plus
 * récent ; « Plus tard » écarte cette version-là jusqu'au lendemain, une
 * plus récente encore serait proposée tout de suite. Tout ici bloque : à
 * appeler hors du fil principal, sauf les préférences.
 */
class MiseAJour(contexte: Context) {

    /** Une release : son numéro sans le `v`, sa note telle que publiée, l'APK joint s'il y en a un, et sa page. */
    class Version(val numero: String, val note: String, val apk: String?, val page: String)

    class GitHubInjoignable(message: String) : Exception(message)

    private val prefs = contexte.getSharedPreferences("mise_a_jour", Context.MODE_PRIVATE)

    /** « Plus tard » : cette version ne sera plus proposée d'elle-même avant un jour. */
    fun ecarter(numero: String) {
        prefs.edit().putString("ecartee", numero).putLong("ecartee_le", System.currentTimeMillis()).apply()
    }

    /** Vrai si cette version a été écartée il y a moins d'un jour. */
    fun estEcartee(numero: String): Boolean =
        prefs.getString("ecartee", null) == numero && System.currentTimeMillis() - prefs.getLong("ecartee_le", 0L) < UN_JOUR_MS

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

    /**
     * Le total des téléchargements des APK de toutes les releases, ce que
     * GitHub compte, mises à jour comprises ; `null` si GitHub ne répond pas.
     */
    fun telechargements(): Int? {
        val c = URL(TOUTES_LES_RELEASES).openConnection() as HttpURLConnection
        c.connectTimeout = DELAI_MS
        c.readTimeout = DELAI_MS
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Accept", "application/vnd.github+json")
        return try {
            if (c.responseCode != 200) return null
            val releases = org.json.JSONArray(c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            var total = 0
            for (i in 0 until releases.length()) {
                val pieces = releases.optJSONObject(i)?.optJSONArray("assets") ?: continue
                for (j in 0 until pieces.length()) {
                    val p = pieces.optJSONObject(j) ?: continue
                    if (p.optString("name").endsWith(".apk")) total += p.optInt("download_count")
                }
            }
            total
        } catch (e: Exception) {
            tracer("téléchargements illisibles : ${e.message}")
            null
        } finally {
            c.disconnect()
        }
    }

    companion object {
        private const val TOUTES_LES_RELEASES = "https://api.github.com/repos/Lestat2Lioncourt/unicorn-radio/releases?per_page=100"
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
