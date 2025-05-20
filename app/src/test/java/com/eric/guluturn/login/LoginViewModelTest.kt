package com.eric.guluturn.login

import com.eric.guluturn.ui.login.LoginViewModel
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for LoginViewModel.
 * Uses mocked OkHttpClient to simulate API responses.
 */
class LoginViewModelTest {

    private lateinit var mockClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        mockClient = mock()
        mockCall = mock()
        viewModel = LoginViewModel(mockClient)
    }

    @Test
    fun `verifyApiKey returns true when response is successful`() = runTest {
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("https://api.openai.com/v1/models").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody(null))
            .build()

        whenever(mockClient.newCall(any<Request>())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        var result: Boolean? = null
        val latch = CountDownLatch(1)

        viewModel.verifyApiKey("sk-valid-key") {
            result = it
            latch.countDown()
        }

        latch.await(1, TimeUnit.SECONDS)
        assertTrue(result == true)
    }

    @Test
    fun `verifyApiKey returns false when response is not successful`() = runTest {
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("https://api.openai.com/v1/models").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody(null))
            .build()

        whenever(mockClient.newCall(any<Request>())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        var result: Boolean? = null
        val latch = CountDownLatch(1)

        viewModel.verifyApiKey("sk-invalid-key") {
            result = it
            latch.countDown()
        }

        latch.await(1, TimeUnit.SECONDS)
        assertFalse(result == true)
    }

    @Test
    fun `verifyApiKey returns false when exception is thrown`() = runTest {
        whenever(mockClient.newCall(any<Request>())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenThrow(IOException("Network failure"))

        var result: Boolean? = null
        val latch = CountDownLatch(1)

        viewModel.verifyApiKey("sk-error") {
            result = it
            latch.countDown()
        }

        latch.await(1, TimeUnit.SECONDS)
        assertFalse(result == true)
    }

    @Test
    fun `verifyApiKey with empty key should return false`() = runTest {
        var result: Boolean? = null
        val latch = CountDownLatch(1)

        viewModel.verifyApiKey("") {
            result = it
            latch.countDown()
        }

        latch.await(1, TimeUnit.SECONDS)
        assertFalse(result == true)
    }

}
