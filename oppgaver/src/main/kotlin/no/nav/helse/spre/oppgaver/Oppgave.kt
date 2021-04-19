package no.nav.helse.spre.oppgaver

import java.util.*

class Oppgave(
    val hendelseId: UUID,
    val dokumentId: UUID,
    var tilstand: Tilstand = Tilstand.DokumentOppdaget,
    val dokumentType: DokumentType
) {
    private var observer: Observer? = null
    fun setObserver(observer: Observer) = apply {
        this.observer = observer
    }

    interface Observer {
        fun lagre(oppgave: Oppgave) {}
        fun publiser(oppgave: Oppgave) {}
    }

    fun håndter(hendelse: Hendelse.TilInfotrygd) = tilstand.håndter(this, hendelse)
    fun håndter(hendelse: Hendelse.Avsluttet) = tilstand.håndter(this, hendelse)
    fun håndter(hendelse: Hendelse.Lest) = tilstand.håndter(this, hendelse)
    fun håndter(hendelse: Hendelse.AvsluttetUtenUtbetaling) = tilstand.håndter(this, hendelse)
    fun håndter(hendelse: Hendelse.MottattInntektsmeldingIAvsluttetUtenUtbetaling) = tilstand.håndter(this, hendelse)

    private fun tilstand(tilstand: Tilstand) {
        val forrigeTilstand = this.tilstand
        this.tilstand = tilstand
        observer?.lagre(this)
        tilstand.entering(this, forrigeTilstand)
    }


    sealed class Tilstand {
        open fun entering(oppgave: Oppgave, forrigeTilstand: Tilstand) {
            oppgave.observer?.publiser(oppgave)
        }

        open fun håndter(oppgave: Oppgave, hendelse: Hendelse.TilInfotrygd) {}
        open fun håndter(oppgave: Oppgave, hendelse: Hendelse.Avsluttet) {}
        open fun håndter(oppgave: Oppgave, hendelse: Hendelse.Lest) {}
        open fun håndter(oppgave: Oppgave, hendelse: Hendelse.AvsluttetUtenUtbetaling) {}
        open fun håndter(oppgave: Oppgave, hendelse: Hendelse.MottattInntektsmeldingIAvsluttetUtenUtbetaling) {}

        object SpleisFerdigbehandlet : Tilstand() {
            override fun entering(oppgave: Oppgave, forrigeTilstand: Tilstand) {
                if (forrigeTilstand == KortSøknadFerdigbehandlet) return
                super.entering(oppgave, forrigeTilstand)
            }
        }

        object LagOppgave : Tilstand()

        object SpleisLest : Tilstand() {
            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.TilInfotrygd) {
                oppgave.tilstand(LagOppgave)
            }

            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.Avsluttet) {
                oppgave.tilstand(SpleisFerdigbehandlet)
            }

            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.AvsluttetUtenUtbetaling) {
                oppgave.tilstand(KortSøknadFerdigbehandlet)
            }

            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.MottattInntektsmeldingIAvsluttetUtenUtbetaling) {
                oppgave.tilstand(KortInntektsmeldingFerdigbehandlet)
            }
        }

        object DokumentOppdaget : Tilstand() {
            override fun entering(oppgave: Oppgave, forrigeTilstand: Tilstand) {}
            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.TilInfotrygd) {
                oppgave.tilstand(LagOppgave)
            }

            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.Avsluttet) {
                oppgave.tilstand(SpleisFerdigbehandlet)
            }

            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.Lest) {
                oppgave.tilstand(SpleisLest)
            }

            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.AvsluttetUtenUtbetaling) {
                oppgave.tilstand(KortSøknadFerdigbehandlet)
            }

            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.MottattInntektsmeldingIAvsluttetUtenUtbetaling){
                oppgave.tilstand(KortInntektsmeldingFerdigbehandlet)
            }
        }

        object KortInntektsmeldingFerdigbehandlet: Tilstand() {
            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.TilInfotrygd) {
                oppgave.tilstand(LagOppgave)
            }
            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.Avsluttet) {
                oppgave.tilstand(SpleisFerdigbehandlet)
            }
        }

        object KortSøknadFerdigbehandlet: Tilstand() {
            override fun håndter(oppgave: Oppgave, hendelse: Hendelse.Avsluttet) {
                oppgave.tilstand(SpleisFerdigbehandlet)
            }
        }

        override fun toString(): String {
            return this.javaClass.simpleName
        }
    }
}

enum class DokumentType {
    Inntektsmelding, Søknad
}
