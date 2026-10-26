package org.unicornradio.app.serveur

import android.util.Log
import org.json.JSONObject
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.fichier.Fichiers
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Les routes du serveur Unicorn-TV, côté téléphone, selon
 * `docs/format-d-echange.md` : l'identité sans jeton, l'appariement par le
 * code court, l'export et le détachement avec le jeton en en-tête
 * `Authorization: Bearer`. Un `401` est une révocation. Tout ici bloque : à
 * appeler hors du fil principal.
 */
object ClientServeur {

    class Identite(val id: String, val nom: String, val format: Int)
    class Appariement(val jeton: String, val serveur: String, val nom: String, val utilisateur: String)

    /** Pourquoi une adresse n'a pas donné le serveur attendu ; l'écran en fait un texte. */
    enum class Raison { DELAI, REFUSEE, ADRESSE_INCONNUE, HTTP, PAS_UNICORN_TV, AUTRE_SERVEUR, AUCUNE_ADRESSE, AUTRE }

    /** Pas de réponse, ou une réponse qui n'est pas celle d'un Unicorn-TV : l'adresse essayée et la raison. */
    class Injoignable(val adresse: String?, val raison: Raison, val detail: String? = null) :
        Exception("${adresse ?: "-"} : $raison${if (detail != null) " ($detail)" else ""}")
    /** Le jeton n'est plus connu : le téléphone a été révoqué. */
    class Revoque : Exception("401")
    /** Le code d'appariement est inconnu ou périmé. */
    class CodeInconnu : Exception("404")
    /** Dix codes faux : la porte est fermée dix minutes. */
    class TropDEssais : Exception("429")

    private const val USER_AGENT = "Unicorn-Radio/${BuildConfig.VERSION_NAME}"
    /** Le premier contact sur le wifi réveille la radio du téléphone : cinq secondes, et un second essai. */
    private const val DELAI_IDENTITE_MS = 5_000
    private const val ESSAIS_IDENTITE = 2
    private const val DELAI_MS = 15_000
    private const val PORT_PAR_DEFAUT = 3000

    /** Ce que le QR code d'appariement porte, ou ce qu'on a tapé à la main. */
    class Invitation(val serveur: String?, val nom: String?, val adresses: List<String>, val code: String) {
        companion object {
            /** Le JSON du QR code ; `null` s'il n'est pas un appariement Unicorn-TV. */
            fun depuisQr(texte: String): Invitation? = try {
                val o = JSONObject(texte)
                if (o.optString("application") != "unicorn-tv" || o.optInt("appariement") != 1) null
                else {
                    val tableau = o.optJSONArray("adresses")
                    val adresses = if (tableau == null) emptyList()
                    else (0 until tableau.length()).map { tableau.optString(it) }.filter { it.isNotBlank() }
                    val code = o.optString("code").trim()
                    if (adresses.isEmpty() || code.isEmpty()) null
                    else Invitation(o.optString("serveur").takeIf { it.isNotEmpty() }, o.optString("nom").takeIf { it.isNotEmpty() }, adresses, code)
                }
            } catch (e: Exception) {
                null
            }

            /** Une adresse tapée : `http://` d'office quand le schéma manque, le port 3000 quand le port manque, sans barre finale. */
            fun normaliser(adresse: String): String {
                var a = adresse.trim().trimEnd('/')
                if (!a.contains("://")) a = "http://$a"
                val hote = a.substringAfter("://")
                if (!hote.contains(':') && !hote.contains('/')) a += ":$PORT_PAR_DEFAUT"
                return a
            }

            /** Pour l'affichage : l'adresse sans son `http://`. */
            fun courte(adresse: String?): String = adresse?.removePrefix("http://") ?: "-"
        }
    }

    /** `GET /api/radio/serveur` : l'identité ; lève `Injoignable` si rien d'un Unicorn-TV ne répond là. */
    fun identite(adresse: String): Identite {
        val o = JSONObject(demander(adresse, "GET", "/api/radio/serveur", delai = DELAI_IDENTITE_MS))
        if (o.optString("application") != "unicorn-tv") throw Injoignable(adresse, Raison.PAS_UNICORN_TV)
        return Identite(o.getString("serveur"), o.optString("nom"), o.optInt("format", 1))
    }

    /**
     * La première adresse où un Unicorn-TV répond avec l'identité attendue,
     * ou avec n'importe quelle identité si on n'en attend pas encore ; l'adresse
     * active d'abord, chaque adresse essayée deux fois quand elle ne répond
     * pas. Sinon, `Injoignable` avec la dernière adresse et sa raison.
     */
    fun trouver(adresses: List<String>, idAttendu: String?, dAbord: String? = null): Pair<String, Identite> {
        val ordre = LinkedHashSet<String>()
        if (dAbord != null) ordre += dAbord
        ordre += adresses
        var derniere: Injoignable? = null
        for (adresse in ordre) {
            for (essai in 1..ESSAIS_IDENTITE) {
                try {
                    val i = identite(adresse)
                    if (idAttendu == null || i.id == idAttendu) return adresse to i
                    Log.w(Fichiers.JOURNAL, "serveur : $adresse est « ${i.nom} » (${i.id}), pas celui attendu")
                    derniere = Injoignable(adresse, Raison.AUTRE_SERVEUR, i.nom)
                    break
                } catch (e: Injoignable) {
                    Log.i(Fichiers.JOURNAL, "serveur : $adresse, essai $essai : ${e.message}")
                    derniere = e
                    // Une réponse claire, même mauvaise, ne mérite pas un second essai ; un silence, si.
                    if (e.raison != Raison.DELAI && e.raison != Raison.REFUSEE) break
                }
            }
        }
        throw derniere ?: Injoignable(null, Raison.AUCUNE_ADRESSE)
    }

    /** `POST /api/radio/appariement` : le code court contre un jeton. */
    fun apparier(adresse: String, code: String, appareilId: String, appareilNom: String): Appariement {
        val corps = JSONObject()
            .put("code", code)
            .put("appareil", JSONObject().put("id", appareilId).put("nom", appareilNom))
        val o = JSONObject(demander(adresse, "POST", "/api/radio/appariement", corps = corps.toString()))
        return Appariement(o.getString("jeton"), o.getString("serveur"), o.optString("nom"), o.optString("utilisateur"))
    }

    /** `GET /api/radio/export` avec le jeton : la section seule, telle quelle. */
    fun export(adresse: String, jeton: String): String = demander(adresse, "GET", "/api/radio/export", jeton = jeton)

    /** `DELETE /api/radio/appariement` avec le jeton : le téléphone se retire. */
    fun detacher(adresse: String, jeton: String) {
        demander(adresse, "DELETE", "/api/radio/appariement", jeton = jeton)
    }

    private fun demander(adresse: String, methode: String, chemin: String, corps: String? = null, jeton: String? = null, delai: Int = DELAI_MS): String {
        val c = try {
            URL(adresse + chemin).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw Injoignable(adresse, Raison.ADRESSE_INCONNUE, e.message)
        }
        try {
            c.requestMethod = methode
            c.connectTimeout = delai
            c.readTimeout = delai
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", USER_AGENT)
            c.setRequestProperty("Accept", "application/json")
            if (jeton != null) c.setRequestProperty("Authorization", "Bearer $jeton")
            if (corps != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(corps.toByteArray(Charsets.UTF_8)) }
            }
            when (val code = c.responseCode) {
                200 -> return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                401 -> throw Revoque()
                404 -> if (chemin == "/api/radio/appariement" && methode == "POST") throw CodeInconnu() else throw Injoignable(adresse, Raison.HTTP, "404")
                429 -> throw TropDEssais()
                else -> throw Injoignable(adresse, Raison.HTTP, code.toString())
            }
        } catch (e: SocketTimeoutException) {
            throw Injoignable(adresse, Raison.DELAI)
        } catch (e: ConnectException) {
            throw Injoignable(adresse, Raison.REFUSEE, e.message)
        } catch (e: UnknownHostException) {
            throw Injoignable(adresse, Raison.ADRESSE_INCONNUE, e.message)
        } catch (e: java.io.IOException) {
            throw Injoignable(adresse, Raison.AUTRE, e.message)
        } finally {
            c.disconnect()
        }
    }
}
