package no.nav.helse.spre.gosys.annullering

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AnnulleringMessage private constructor(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    private val fom: LocalDate,
    private val tom: LocalDate,
    val organisasjonsnummer: String,
    private val dato: LocalDateTime,
    private val saksbehandlerIdent: String,
    private val saksbehandlerEpost: String,
    private val personFagsystemId: String?,
    private val arbeidsgiverFagsystemId: String?,
) {
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val norskFom: String = fom.format(formatter)
    val norskTom: String = tom.format(formatter)

    constructor(hendelseId: UUID, packet: JsonMessage) :
            this(
                hendelseId = hendelseId,
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                fom = packet["fom"].asLocalDate(),
                tom = packet["tom"].asLocalDate(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                dato = packet["annullertAvSaksbehandler"].asLocalDateTime(),
                saksbehandlerIdent = packet["ident"].asText(),
                saksbehandlerEpost = packet["epost"].asText(),
                personFagsystemId = packet["personFagsystemId"].takeUnless { it.isMissingOrNull() }?.asText(),
                arbeidsgiverFagsystemId = packet["arbeidsgiverFagsystemId"].takeUnless { it.isMissingOrNull() }
                    ?.asText()
            )

    internal fun toPdfPayloadV2(organisasjonsnavn: String?, navn: String?) = AnnulleringPdfPayloadV2(
        fødselsnummer = fødselsnummer,
        fom = fom,
        tom = tom,
        organisasjonsnummer = organisasjonsnummer,
        dato = dato,
        epost = saksbehandlerEpost,
        ident = saksbehandlerIdent,
        personFagsystemId = personFagsystemId,
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        organisasjonsnavn = organisasjonsnavn,
        navn = navn
    )

    data class Linje(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int,
        val beløp: Int
    )
}

private fun JsonMessage.utbetalingslinjer() = this["utbetalingslinjer"].map {
    AnnulleringMessage.Linje(
        fom = it["fom"].asLocalDate(),
        tom = it["tom"].asLocalDate(),
        grad = it["grad"].asInt(),
        beløp = it["beløp"].asInt()
    )
}
