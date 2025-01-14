package no.nav.helse.spre.gosys.feriepenger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.LocalDateTime
import java.util.*

class FeriepengerMessage(hendelseId: UUID, packet: JsonMessage) {
    val hendelseId = hendelseId
    val fødselsnummer = packet["fødselsnummer"].asText()
    val aktørId = packet["aktørId"].asText()
    val oppdrag = mutableListOf<Oppdrag>()
    val orgnummer: String = packet["organisasjonsnummer"].asText()
    lateinit var utbetalt: LocalDateTime

    init {
        if (!packet["arbeidsgiverOppdrag.linjer"].isEmpty) {
            val arbeidsgiverOppdrag = packet["arbeidsgiverOppdrag"]
            oppdrag.add(parseOppdrag(arbeidsgiverOppdrag, OppdragType.ARBEIDSGIVER))
            utbetalt = arbeidsgiverOppdrag["tidsstempel"].asLocalDateTime()
        }
        if (!packet["personOppdrag.linjer"].isEmpty) {
            val personOppdrag = packet["personOppdrag"]
            oppdrag.add(parseOppdrag(personOppdrag, OppdragType.PERSON))
            utbetalt = personOppdrag["tidsstempel"].asLocalDateTime()
        }
    }

    private fun parseOppdrag(oppdragPacket: JsonNode, type: OppdragType) = Oppdrag(
        type = type,
        fom = oppdragPacket["fom"].asLocalDate(),
        tom = oppdragPacket["tom"].asLocalDate(),
        mottaker = oppdragPacket["mottaker"].asText(),
        totalbeløp = oppdragPacket["linjer"].sumOf { it["totalbeløp"].asInt() },
        fagsystemId = oppdragPacket["fagsystemId"].asText()
    )
}
