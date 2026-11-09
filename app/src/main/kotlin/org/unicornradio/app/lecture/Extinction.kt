package org.unicornradio.app.lecture

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * La minuterie d'extinction : dans tant de minutes, la lecture se met en
 * pause. L'écran programme, le service tient le geste de pause ; les deux
 * vivent dans le même processus, que le service en avant-plan garde en vie
 * tant que ça joue. Une seule minuterie, la dernière programmée gagne.
 */
object Extinction {

    private val main = Handler(Looper.getMainLooper())

    /** L'échéance, en temps machine (`SystemClock.elapsedRealtime`), ou `null`. */
    @Volatile
    var fin: Long? = null
        private set

    /** Ce qui se passe à l'échéance : le service le pose, mettre en pause. */
    @Volatile
    var arret: (() -> Unit)? = null

    private val echeance = Runnable {
        fin = null
        arret?.invoke()
    }

    /** S'arrêter dans `minutes` minutes ; zéro annule. */
    fun programmer(minutes: Int) {
        main.removeCallbacks(echeance)
        fin = null
        if (minutes > 0) {
            fin = SystemClock.elapsedRealtime() + minutes * 60_000L
            main.postDelayed(echeance, minutes * 60_000L)
        }
    }

    fun annuler() = programmer(0)

    val active: Boolean
        get() = fin != null

    /** Les minutes qui restent, une au moins tant que c'est programmé. */
    val minutesRestantes: Int?
        get() = fin?.let { max(1, ((it - SystemClock.elapsedRealtime()) / 60_000.0).roundToInt()) }
}
