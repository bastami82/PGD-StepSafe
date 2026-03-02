package uk.appyapp.stepsafe.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertEquals
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.entities.HomeLocation
import uk.appyapp.stepsafe.data.repository.SettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AlertViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: AlertViewModel
    private val homeLocationDao = mockk<HomeLocationDao>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private val homeLocationFlow = MutableStateFlow<HomeLocation?>(null)
    private val voicePromptFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { homeLocationDao.getHomeLocation() } returns homeLocationFlow
        every { settingsRepository.voicePromptEnabled } returns voicePromptFlow

        viewModel = AlertViewModel(
            homeLocationDao,
            settingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel should expose home location from dao`() = runTest {
        // Given
        val mockLocation = HomeLocation(id = 0, latitude = 51.5, longitude = -0.1, address = "Test")
        
        // Start collection to trigger stateIn
        val job = backgroundScope.launch { viewModel.homeLocation.collect() }
        
        // When
        homeLocationFlow.value = mockLocation
        advanceUntilIdle()
        
        // Then
        assertEquals(mockLocation, viewModel.homeLocation.value)
    }

    @Test
    fun `viewModel should expose voice prompt preference`() = runTest {
        // Given
        // Start collection to trigger stateIn
        val job = backgroundScope.launch { viewModel.voicePromptEnabled.collect() }
        
        // When
        voicePromptFlow.value = false
        advanceUntilIdle()
        
        // Then
        assertEquals(false, viewModel.voicePromptEnabled.value)
    }
}
