package no.nav.helse.spre.saksbehandlingsstatistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spre.saksbehandlingsstatistikk.Avsender.SPLEIS
import no.nav.helse.spre.saksbehandlingsstatistikk.BehandlingStatus.AVSLUTTET
import no.nav.helse.spre.saksbehandlingsstatistikk.TestUtil.finnSøknadDokumentId
import no.nav.helse.spre.saksbehandlingsstatistikk.YtelseType.SYKEPENGER
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import java.util.UUID.randomUUID

internal class EndToEndTest {
    private val testRapid = TestRapid()
    private val dataSource = TestUtil.dataSource
    private val søknadDao = SøknadDao(dataSource)
    private val utgiver = LokalUtgiver()
    private val spreService = SpreService(utgiver, søknadDao)

    init {
        testRapid.setupRivers(spreService, søknadDao)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "TRUNCATE TABLE søknad"
            session.run(queryOf(query).asExecute)
        }
    }

    @Test
    fun `Happy path`() {
        val søknad = Søknad(
            randomUUID(),
            randomUUID(),
            LocalDateTime.now(),
            LocalDateTime.now()
        )


        testRapid.sendTestMessage(sendtSøknadNavMessage(søknad))
        val vedtaksperiodeId = randomUUID()
        testRapid.sendTestMessage(VedtaksperiodeEndretData(listOf(søknad.søknadHendelseId), vedtaksperiodeId).json)
        testRapid.sendTestMessage(vedtaksperiodeGodkjentMessage("Knut", vedtaksperiodeId))
        testRapid.sendTestMessage(vedtakFattetMessage(listOf(søknad.søknadHendelseId)))

        assert(utgiver.meldinger.size == 1)

        val sendtTilDVH = utgiver.meldinger[0]
        val expected = StatistikkEvent(
            aktorId = "aktørens id",
            behandlingStatus = AVSLUTTET,
            behandlingId = søknad.søknadDokumentId,
            behandlingType = BehandlingType.SØKNAD,
            tekniskTid = sendtTilDVH.tekniskTid,
            funksjonellTid = LocalDateTime.parse("2021-03-09T18:23:27.769"),
            mottattDato = "2021-01-01T00:00",
            registrertDato = "2021-01-01T00:00",
            ytelseType = SYKEPENGER,
            utenlandstilsnitt = Utenlandstilsnitt.NEI,
            totrinnsbehandling = Totrinnsbehandling.NEI,
            avsender = SPLEIS,
            saksbehandlerIdent = "Knut",
            ansvarligEnhetKode = AnsvarligEnhetKode.FIREFIREÅTTEÅTTE,
            ansvarligEnhetType = AnsvarligEnhetType.NORG,
            versjon = sendtTilDVH.versjon
        )

        assertEquals(expected, sendtTilDVH)
    }

    @Test
    fun `lagrer søknad til basen`() {
        val søknadHendelseId = randomUUID()
        val søknad = Søknad(søknadHendelseId, randomUUID(), LocalDateTime.now(), LocalDateTime.now(), null)

        testRapid.sendTestMessage(sendtSøknadNavMessage(søknad))

        val søknadDokumentId = finnSøknadDokumentId(søknadHendelseId)
        assertEquals(søknad.søknadDokumentId, søknadDokumentId)
    }

    @Test
    fun `håndterer duplikate dokumenter`() {
        val søknadHendelseId = randomUUID()
        val søknad = Søknad(søknadHendelseId, randomUUID(), LocalDateTime.now(), LocalDateTime.now(), null)

        testRapid.sendTestMessage(sendtSøknadArbeidsgiverMessage(søknad))
        testRapid.sendTestMessage(sendtSøknadNavMessage(søknad))

        val søknadDokumentId = finnSøknadDokumentId(søknadHendelseId)
        assertEquals(søknad.søknadDokumentId, søknadDokumentId)
    }

    @Test
    fun `Ignorerer vedtaksperiode_endret-events med dato fra før vi har en komplett dokumentdatabase`() {
        val søknad = Søknad(randomUUID(), randomUUID(), LocalDateTime.now(), LocalDateTime.now(), null)

        testRapid.sendTestMessage(vedtaksperiodeEndretMessageUtdatert(listOf(søknad.søknadHendelseId), "TIL_GODKJENNING"))

        assert(utgiver.meldinger.isEmpty())
    }

    @Test
    fun `lagrer saksbehandlingsløp for søknad`() {

        val vedtaksperiodeId = randomUUID()
        val saksbehandlerIdent = "AA10000"

        val nyttDokumentData = nyttDokumentData()

        val søknad = nyttDokumentData.asSøknad
            .saksbehandlerIdent(saksbehandlerIdent)
            .vedtaksperiodeId(vedtaksperiodeId)
            .vedtakFattet(LocalDateTime.parse("2021-03-09T18:23:27.769"))




        testRapid.sendTestMessage(nyttDokumentData.sendtSøknadNavMessage)
        testRapid.sendTestMessage(VedtaksperiodeEndretData(listOf(nyttDokumentData.hendelseId), vedtaksperiodeId).json)
        testRapid.sendTestMessage(vedtaksperiodeGodkjentMessage(saksbehandlerIdent, vedtaksperiodeId))
        testRapid.sendTestMessage(vedtakFattetMessage(listOf(nyttDokumentData.hendelseId), vedtaksperiodeId))

        assertEquals(søknad, søknadDao.finnSøknad(listOf(nyttDokumentData.hendelseId)))
        assertEquals(søknad, søknadDao.finnSøknad(vedtaksperiodeId))
    }


}

fun nyttDokumentData() = NyttDokumentData(
    randomUUID(),
    randomUUID(),
    LocalDateTime.parse("2021-01-01T00:00:00"),
)

val NyttDokumentData.sendtSøknadNavMessage get() =
    """{
            "@event_name": "sendt_søknad_nav",
            "@id": "${this.hendelseId}",
            "id": "${this.søknadId}",
            "@opprettet": "${this.hendelseOpprettet}"
        }"""

private fun vedtakFattetMessage(hendelseIder: List<UUID>, vedtaksperiodeId: UUID) =
    """{
            "@event_name": "vedtak_fattet",
            "@opprettet": "2021-04-01T12:00:00.000000",
            "aktørId": "aktørens id",
            "hendelser": [${hendelseIder.joinToString { """"$it"""" }}],
            "vedtaksperiodeId": "$vedtaksperiodeId"
        }"""

val VedtaksperiodeEndretData.json get() =
    """{
            "@event_name": "vedtaksperiode_endret",
            "gjeldendeTilstand": "AVVENTER_GODKJENNING",
            "hendelser": [${this.hendelser.joinToString { """"$it"""" }}],
            "@opprettet": "2021-03-09T18:23:27.769",
            "aktørId": "aktørens id",
            "vedtaksperiodeId": "${this.vedtaksperiodeId}"
        }"""

fun vedtaksperiodeGodkjentMessage(saksbehandlerIdent: String, vedtaksperiodeId: UUID) =
    """{
            "@event_name": "vedtaksperiode_godkjent",
            "@opprettet": "2021-03-09T18:23:27.769",
            "saksbehandlerIdent": "${saksbehandlerIdent}",
            "vedtaksperiodeId": "${vedtaksperiodeId}"
        }"""

fun vedtaksperiodeEndretMessageUtdatert(hendelser: List<UUID>, tilstand: String) =
    """{
            "@event_name": "vedtaksperiode_endret",
            "gjeldendeTilstand": "$tilstand",
            "hendelser": [${hendelser.joinToString { """"$it"""" }}],
            "@opprettet": "2021-03-09T18:23:27.769",
            "aktørId": "aktørens id",
            "vedtaksperiodeId": "${randomUUID()}"
        }"""

fun vedtakFattetMessage(hendelser: List<UUID>) =
    """{
            "@event_name": "vedtak_fattet",
            "hendelser": [${hendelser.joinToString { """"$it"""" }}],
            "@opprettet": "2021-03-09T18:23:27.769",
            "utbetalingId": "${randomUUID()}",
            "aktørId": "aktørens id",
            "vedtaksperiodeId": "${randomUUID()}"
        }"""

fun sendtSøknadNavMessage(søknad: Søknad) =
    """{
            "@event_name": "sendt_søknad_nav",
            "@id": "${søknad.søknadHendelseId}",
            "id": "${søknad.søknadDokumentId}",
            "@opprettet": "2021-01-01T00:00:00"
        }"""


private fun sendtSøknadArbeidsgiverMessage(søknad: Søknad) =
    """{
            "@event_name": "sendt_søknad_arbeidsgiver",
            "@id": "${søknad.søknadHendelseId}",
            "id": "${søknad.søknadDokumentId}",
            "@opprettet": "2021-01-01T00:00:00"
        }"""