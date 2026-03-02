package uk.appyapp.stepsafe.service

import android.content.Context
import android.content.Intent
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import uk.appyapp.stepsafe.data.repository.SettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class BootReceiverTest {

    private lateinit var bootReceiver: BootReceiver
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val intent = mockk<Intent>()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        
        bootReceiver = BootReceiver()
        bootReceiver.settingsRepository = settingsRepository
    }

    @Test
    fun `handleBoot should check monitoring state`() = runTest {
        // Given
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { settingsRepository.monitoringActive } returns flowOf(true)
        
        // When
        bootReceiver.handleBoot(context, intent)
        
        // Then
        verify { settingsRepository.monitoringActive }
    }

    @Test
    fun `handleBoot should not start service when monitoring is inactive`() = runTest {
        // Given
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { settingsRepository.monitoringActive } returns flowOf(false)
        
        // When
        bootReceiver.handleBoot(context, intent)
        
        // Then
        verify { settingsRepository.monitoringActive }
    }
}
