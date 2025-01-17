package no.nav.helse.spre.gosys.vedtak

import java.time.LocalDate

data class VedtakPdfPayload(
    val fødselsnummer: String,
    val fagsystemId: String,
    val type: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val linjer: List<Linje>,
    val personOppdrag: Oppdrag = Oppdrag(),
    val arbeidsgiverOppdrag: Oppdrag,
    val organisasjonsnummer: String,
    val behandlingsdato: LocalDate,
    val dagerIgjen: Int,
    val automatiskBehandling: Boolean,
    val godkjentAv: String,
    val totaltTilUtbetaling: Int,
    val ikkeUtbetalteDager: List<IkkeUtbetalteDager>,
    val dagsats: Int?,
    val maksdato: LocalDate?,
    val sykepengegrunnlag: Double,
    val grunnlagForSykepengegrunnlag: Map<String, Double>
) {
    data class Oppdrag(val linjer: List<Linje> = emptyList())

    enum class MottakerType {
        Arbeidsgiver {
            override fun formatter(mottaker: String): String = mottaker.chunked(3).joinToString(separator = " ")
        }, Person {
            override fun formatter(mottaker: String): String = mottaker.chunked(6).joinToString(separator = " ")
        };

        abstract fun formatter(mottaker: String): String
    }

    data class Linje(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int,
        val beløp: Int,
        val mottaker: String,
        val mottakerType: MottakerType = MottakerType.Arbeidsgiver,
        val erOpphørt: Boolean
    )

    data class IkkeUtbetalteDager(
        val fom: LocalDate,
        val tom: LocalDate,
        val grunn: String,
        val begrunnelser: List<String>
    )
}
