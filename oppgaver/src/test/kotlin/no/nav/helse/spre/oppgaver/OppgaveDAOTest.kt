package no.nav.helse.spre.oppgaver

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveDAOTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val oppgaveDAO = OppgaveDAO(dataSource)

    @Test
    fun `finner ikke en ikke-eksisterende oppgave`() {
        assertNull(oppgaveDAO.finnOppgave(hendelseId = UUID.randomUUID()))
    }

    @Test
    fun `finner en eksisterende oppgave`() {
        val hendelseId = UUID.randomUUID()
        val dokumentId = UUID.randomUUID()
        oppgaveDAO.opprettOppgaveHvisNy(
            hendelseId = hendelseId,
            dokumentId = dokumentId,
            dokumentType = DokumentType.Søknad
        )
        val oppgave = oppgaveDAO.finnOppgave(hendelseId)
        assertNotNull(oppgave)
        assertEquals(
            hendelseId = hendelseId,
            dokumentId = dokumentId,
            tilstand = Oppgave.Tilstand.DokumentOppdaget,
            dokumentType = DokumentType.Søknad,
            oppgave = requireNotNull(oppgave),
        )
        oppgaveDAO.oppdaterTilstand(oppgave)
        assertTrue(oppgaveDAO.finnOppgave(hendelseId)!!.sistEndret!!.isAfter(oppgave.sistEndret))
    }

    private fun assertEquals(
        hendelseId: UUID,
        dokumentId: UUID,
        tilstand: Oppgave.Tilstand,
        dokumentType: DokumentType,
        oppgave: Oppgave
    ) {
        assertEquals(hendelseId, oppgave.hendelseId)
        assertEquals(dokumentId, oppgave.dokumentId)
        assertEquals(tilstand, oppgave.tilstand)
        assertEquals(dokumentType, oppgave.dokumentType)
        assertTrue(ChronoUnit.SECONDS.between(LocalDateTime.now(), oppgave.sistEndret) < 5)
    }
}
