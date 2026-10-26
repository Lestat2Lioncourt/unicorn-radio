package org.unicornradio.app.serveur

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONArray
import java.util.UUID

/**
 * Le serveur Unicorn-TV auquel ce téléphone est apparié, selon
 * `docs/format-d-echange.md`, « La synchronisation avec un serveur » : son
 * identité, son nom, ses adresses, le jeton, l'utilisateur. Dans les
 * préférences privées de l'application, que le bac à sable protège ; le
 * jeton n'ouvre que la liste de radios. L'identifiant d'appareil est tiré
 * une fois pour toutes, et survit au détachement.
 */
class Serveur(contexte: Context) {

    private val prefs = contexte.getSharedPreferences("serveur", Context.MODE_PRIVATE)
    private val resolveur = contexte.contentResolver

    val apparie: Boolean
        get() = jeton != null

    /** L'identifiant du serveur, ce que l'application compare, jamais l'adresse. */
    val id: String?
        get() = prefs.getString("id", null)

    val nom: String
        get() = prefs.getString("nom", null) ?: ""

    val utilisateur: String
        get() = prefs.getString("utilisateur", null) ?: ""

    val jeton: String?
        get() = prefs.getString("jeton", null)

    /** Toutes les adresses locales du serveur, telles que le QR code les a données. */
    val adresses: List<String>
        get() {
            val tableau = JSONArray(prefs.getString("adresses", null) ?: "[]")
            return (0 until tableau.length()).map { tableau.optString(it) }.filter { it.isNotEmpty() }
        }

    /** La dernière adresse qui a répondu avec la bonne identité : essayée en premier. */
    val adresseActive: String?
        get() = prefs.getString("adresseActive", null)

    /** L'instant de la dernière copie tirée, en millisecondes, ou 0. */
    val derniereCopie: Long
        get() = prefs.getLong("derniereCopie", 0L)

    /** L'identifiant de ce téléphone, créé à la première demande. */
    val appareilId: String
        get() = prefs.getString("appareilId", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("appareilId", it).apply()
        }

    /** Le nom que le téléphone se donne dans les réglages, sinon son modèle. */
    val appareilNom: String
        get() = try {
            Settings.Global.getString(resolveur, "device_name")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        } ?: Build.MODEL

    fun enregistrer(id: String, nom: String, adresses: List<String>, adresseActive: String, jeton: String, utilisateur: String) {
        prefs.edit()
            .putString("id", id)
            .putString("nom", nom)
            .putString("adresses", JSONArray(adresses).toString())
            .putString("adresseActive", adresseActive)
            .putString("jeton", jeton)
            .putString("utilisateur", utilisateur)
            .putLong("derniereCopie", 0L)
            .apply()
    }

    fun noterLaCopie(adresseActive: String) {
        prefs.edit().putString("adresseActive", adresseActive).putLong("derniereCopie", System.currentTimeMillis()).apply()
    }

    /** Oublie tout du serveur, sauf l'identifiant d'appareil. */
    fun oublier() {
        prefs.edit()
            .remove("id").remove("nom").remove("adresses").remove("adresseActive")
            .remove("jeton").remove("utilisateur").remove("derniereCopie")
            .apply()
    }
}
