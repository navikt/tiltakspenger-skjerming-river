package no.nav.tiltakspenger.skjerming

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient
import no.nav.tiltakspenger.skjerming.oauth.AzureTokenProvider

fun main() {
    System.setProperty("logback.configurationFile", "egenLogback.xml")
    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    val tokenProvider = AzureTokenProvider()

    val rapidConfig = if (Configuration.applicationProfile() == Profile.LOCAL) {
        RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidsAndRivers, LokalKafkaConfig())
    } else {
        RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidsAndRivers)
    }

    val rapidsConnection: RapidsConnection = RapidApplication.Builder(rapidConfig).build()

    rapidsConnection.apply {
        SkjermingService(
            rapidsConnection = this,
            skjermingKlient = SkjermingKlient(getToken = tokenProvider::getToken),
        )

        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                log.info { "Starting tiltakspenger-skjerming" }
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                log.info { "Stopping tiltakspenger-skjerming" }
                super.onShutdown(rapidsConnection)
            }
        })
    }.start()
}
