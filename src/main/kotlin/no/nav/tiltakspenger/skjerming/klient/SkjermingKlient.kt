package no.nav.tiltakspenger.skjerming.klient

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.skjerming.Configuration
import no.nav.tiltakspenger.skjerming.defaultObjectMapper
import no.nav.tiltakspenger.skjerming.httpClientWithRetry

class SkjermingKlient(
    private val skjermingConfig: Configuration.SkjermingKlientConfig = Configuration.skjermingKlientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = httpClientWithRetry(
        objectMapper = objectMapper,
        engine = engine,
    ) {},
) {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    suspend fun erSkjermetPerson(fødselsnummer: String, behovId: String): Boolean {
        val httpResponse = httpClient.preparePost("${skjermingConfig.baseUrl}/azure/skjermet") {
            header(navCallIdHeader, behovId)
            bearerAuth(getToken())
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(SkjermetDataRequestDTO(fødselsnummer))
        }.execute()

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra tiltakspenger-skjerming")
        }
    }

    private data class SkjermetDataRequestDTO(val personident: String)
}
