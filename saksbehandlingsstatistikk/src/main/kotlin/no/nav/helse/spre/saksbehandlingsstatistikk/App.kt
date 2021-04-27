package no.nav.helse.spre.saksbehandlingsstatistikk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.util.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("saksbehandlingsstatistikk")

val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}

@KtorExperimentalAPI
fun main() {
    val env = Environment(System.getenv())
    launchApplication(env)
}

@KtorExperimentalAPI
fun launchApplication(env: Environment) {
    val dataSource = DataSourceBuilder(env.db).getMigratedDataSource()

    val koblingDao = KoblingDao(dataSource)
    val søknadDao = SøknadDao(dataSource)
    val producer =
        KafkaProducer<String, String>(
            loadBaseConfig(env.raw).toProducerConfig()
        )
    val spreService = SpreService(producer, koblingDao, søknadDao)

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.raw))
        .build()
        .apply {
            setupRivers(spreService, søknadDao, koblingDao)
            start()
        }

}

internal fun RapidsConnection.setupRivers(
    spreService: SpreService,
    søknadDao: SøknadDao,
    koblingDao: KoblingDao
) {
    NyttDokumentRiver(this, søknadDao)
    VedtaksperiodeEndretRiver(this, spreService)
    UtbetalingUtbetaltRiver(this, koblingDao, søknadDao)
    VedtakFattetRiver(this, spreService)
}
