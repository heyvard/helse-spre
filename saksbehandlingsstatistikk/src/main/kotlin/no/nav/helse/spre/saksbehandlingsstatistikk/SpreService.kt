package no.nav.helse.spre.saksbehandlingsstatistikk

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("saksbehandlingsstatistikk")

internal class SpreService(
    private val utgiver: Utgiver,
    private val søknadDao: SøknadDao
) {

    internal fun spre(vedtakFattetData: VedtakFattetData) {
        val søknad =
            checkNotNull(søknadDao.finnSøknad(vedtakFattetData.hendelser)) {
                "Finner ikke søknad for vedtak_fattet, med hendelseIder=${vedtakFattetData.hendelser}"
            }
        if (søknad.vedtaksperiodeId != vedtakFattetData.vedtaksperiodeId)
            log.info(
                "Hmm 🤨, lagret søknad og matchende vedtak_fattet har ikke samme vedtaksperiodeId. " +
                        "Søknadsdata har vedtaksperiodeId ${søknad.vedtaksperiodeId}, " +
                        "vedtak_fattet-event har vedtaksperiodeId ${vedtakFattetData.vedtaksperiodeId}"
            )

        val melding = if (søknad.saksbehandlerIdent == "SPLEIS")
            StatistikkEvent.statistikkEventForSøknadAvsluttetAvSpleis(søknad, vedtakFattetData)
        else StatistikkEvent.statistikkEvent(søknad, vedtakFattetData)

        utgiver.publiserStatistikk(melding)
    }

    internal fun spre(vedtaksperiodeForkastetData: VedtaksperiodeForkastetData) {
        val søknad =
            checkNotNull(søknadDao.finnSøknad(vedtaksperiodeForkastetData.vedtaksperiodeId)) {
                "Finner ikke søknad for vedtaksperiode_forkastet, med id=${vedtaksperiodeForkastetData.vedtaksperiodeId}"
            }

        val melding = if (søknad.saksbehandlerIdent == "SPLEIS")
            StatistikkEvent.statistikkEventForAvvistAvSpleis(søknad, vedtaksperiodeForkastetData)
        else StatistikkEvent.statistikkEventForAvvist(søknad, vedtaksperiodeForkastetData)

        utgiver.publiserStatistikk(melding)
    }
}
