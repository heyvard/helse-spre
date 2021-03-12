package no.nav.helse.spre.saksbehandlingsstatistikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class StatistikkEvent(
    val aktorId: String,
//    val behandlingId: UUID?, //søknadId
//    val funksjonellTid: LocalDateTime, //Tidspunkt for avslag eller fatting av vedtak eller tilsvarende
//    val tekniskTid: LocalDateTime, //?
//    val mottattDato: LocalDate, //Tidspunktet søknaden ankom NAV
//    val registrertDato: LocalDate, //Tidspunktet Spleis ble klar over søknaden
//    val ytelseType: YtelseType, //?, trenger vi å sende denne hvis den er hardkodet?
//    val sakId: UUID, //?
//    val saksnummer: Int, //?
//    val behandlingType: BehandlingType,
    val behandlingStatus: BehandlingStatus,
//    val resterendeDager: Int,
)

enum class BehandlingStatus {
    REGISTRERT
}

enum class BehandlingType {

}

enum class YtelseType {

}
