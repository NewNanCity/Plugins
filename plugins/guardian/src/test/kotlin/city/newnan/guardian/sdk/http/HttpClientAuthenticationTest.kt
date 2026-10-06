package city.newnan.guardian.sdk.http

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import city.newnan.guardian.sdk.models.ServerSessionResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HttpClientAuthenticationTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `requests send exactly one API token header`() {
        server.enqueue(MockResponse().setResponseCode(204))
        val token = "server-token"
        val client = HttpClient(server.url("/").toString().removeSuffix("/"), token)

        client.delete("/api/v1/monitor/1/heartbeat")

        val request = server.takeRequest()
        assertEquals(token, request.getHeader("X-API-Token"))
        assertNull(request.getHeader("Authorization"))
        assertEquals(1, request.headers.values("X-API-Token").size)
        client.close()
    }

    @Test
    fun `accepts session ids from the canonical length range`() {
        val client = HttpClient("http://localhost", "server-token")

        client.setServerSession(ServerSessionResponse("a".repeat(64), 1))
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            client.setServerSession(ServerSessionResponse("a".repeat(31), 1))
        }
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            client.setServerSession(ServerSessionResponse("a".repeat(65), 1))
        }

        client.close()
    }
}
