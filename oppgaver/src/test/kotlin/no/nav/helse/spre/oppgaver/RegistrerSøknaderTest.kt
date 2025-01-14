package no.nav.helse.spre.oppgaver

import java.util.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrerSøknaderTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val testRapid = TestRapid()
    private val oppgaveDAO = OppgaveDAO(dataSource)

    init {
        RegistrerSøknader(testRapid, oppgaveDAO)
    }

    @Test
    fun `dytter søknader inn i db`() {
        val hendelseId = UUID.randomUUID()
        testRapid.sendTestMessage(sendtSøknad(hendelseId))

        val oppgave = oppgaveDAO.finnOppgave(hendelseId)
        assertNotNull(oppgave)
        assertEquals(DokumentType.Søknad, oppgave!!.dokumentType)
    }
}

fun sendtSøknad(
    hendelseId: UUID,
    dokumentId: UUID = UUID.randomUUID()
): String =
    """{
            "@event_name": "sendt_søknad_nav",
            "@id": "$hendelseId",
            "id": "$dokumentId"
        }"""

fun sendtArbeidsgiversøknad(
    hendelseId: UUID,
    dokumentId: UUID = UUID.randomUUID()
): String =
    """{
            "@event_name": "sendt_søknad_arbeidsgiver",
            "@id": "$hendelseId",
            "id": "$dokumentId"
        }"""

