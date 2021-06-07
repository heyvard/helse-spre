package no.nav.helse.spre.oppgaver

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

enum class DatabaseTilstand {
    SpleisFerdigbehandlet,
    LagOppgave,
    SpleisLest,
    DokumentOppdaget,
    KortInntektsmeldingFerdigbehandlet,
    KortSøknadFerdigbehandlet
}

class OppgaveDAO(
    private val dataSource: DataSource
) {
    fun finnOppgave(hendelseId: UUID): Oppgave? = using(sessionOf(dataSource)) { session ->
        session.run(queryOf(
            "SELECT * FROM oppgave_tilstand WHERE hendelse_id=?;",
            hendelseId
        )
            .map { rs ->
                Oppgave(
                    hendelseId = UUID.fromString(rs.string("hendelse_id")),
                    dokumentId = UUID.fromString(rs.string("dokument_id")),
                    tilstand = when (enumValueOf<DatabaseTilstand>(rs.string("tilstand"))) {
                        DatabaseTilstand.SpleisFerdigbehandlet -> Oppgave.Tilstand.SpleisFerdigbehandlet
                        DatabaseTilstand.LagOppgave -> Oppgave.Tilstand.LagOppgave
                        DatabaseTilstand.SpleisLest -> Oppgave.Tilstand.SpleisLest
                        DatabaseTilstand.DokumentOppdaget -> Oppgave.Tilstand.DokumentOppdaget
                        DatabaseTilstand.KortInntektsmeldingFerdigbehandlet -> Oppgave.Tilstand.KortInntektsmeldingFerdigbehandlet
                        DatabaseTilstand.KortSøknadFerdigbehandlet -> Oppgave.Tilstand.KortSøknadFerdigbehandlet
                    },
                    dokumentType = DokumentType.valueOf(rs.string("dokument_type")),
                    sistEndret = rs.localDateTimeOrNull("sist_endret")
                )
            }
            .asSingle
        )
    }

    fun opprettOppgaveHvisNy(hendelseId: UUID, dokumentId: UUID, dokumentType: DokumentType) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppgave_tilstand(hendelse_id, dokument_id, dokument_type, sist_endret) VALUES(?, ?, CAST(? AS dokument_type), NOW()) ON CONFLICT (hendelse_id) DO NOTHING;",
                    hendelseId,
                    dokumentId,
                    dokumentType.name
                ).asUpdate
            )
        }

    fun oppdaterTilstand(oppgave: Oppgave) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE oppgave_tilstand SET tilstand=CAST(? AS tilstand_type), sist_endret = NOW() WHERE hendelse_id=?;",
                oppgave.tilstand.toDBTilstand().name, oppgave.hendelseId
            ).asUpdate
        )
    }

}

private fun Oppgave.Tilstand.toDBTilstand(): DatabaseTilstand = when (this) {
    Oppgave.Tilstand.SpleisFerdigbehandlet -> DatabaseTilstand.SpleisFerdigbehandlet
    Oppgave.Tilstand.LagOppgave -> DatabaseTilstand.LagOppgave
    Oppgave.Tilstand.SpleisLest -> DatabaseTilstand.SpleisLest
    Oppgave.Tilstand.DokumentOppdaget -> DatabaseTilstand.DokumentOppdaget
    Oppgave.Tilstand.KortInntektsmeldingFerdigbehandlet -> DatabaseTilstand.KortInntektsmeldingFerdigbehandlet
    Oppgave.Tilstand.KortSøknadFerdigbehandlet -> DatabaseTilstand.KortSøknadFerdigbehandlet
}
