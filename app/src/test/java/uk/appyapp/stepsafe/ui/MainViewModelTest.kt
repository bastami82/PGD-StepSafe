package uk.appyapp.stepsafe.ui

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import uk.appyapp.stepsafe.data.local.dao.CaregiverContactDao
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.repository.SettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var viewModel: MainViewModel
    private val application = mockk<Application>(relaxed = true)
    private val homeLocationDao = mockk<HomeLocationDao>(relaxed = true)
    private val caregiverContactDao = mockk<CaregiverContactDao>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { homeLocationDao.getHomeLocation() } returns flowOf(null)
        every { caregiverContactDao.getCaregiverContact() } returns flowOf(null)
        every { settingsRepository.safeZoneRadius } returns flowOf(100.0)
        every { settingsRepository.monitoringActive } returns flowOf(false)

        viewModel = MainViewModel(
            application,
            homeLocationDao,
            caregiverContactDao,
            settingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateHomeLocation should call dao insert`() = runTest {
        // Given
        val lat = 51.5074
        val lng = -0.1278
        val address = "London Eye"
        
        // When
        viewModel.updateHomeLocation(lat, lng, address)
        
        // Then
        coVerify { homeLocationDao.insertHomeLocation(any()) }
    }

    @Test
    fun `updateSafeZoneRadius should call repository update`() = runTest {
        // Given
        val radius = 200.0
        
        // When
        viewModel.updateSafeZoneRadius(radius)
        
        // Then
        coVerify { settingsRepository.updateSafeZoneRadius(radius) }
    }

    @Test
    fun `updateCaregiverContact should call dao insert`() = runTest {
        // Given
        val name = "John Doe"
        val phone = "1234567890"
        
        // When
        viewModel.updateCaregiverContact(name, phone)
        
        // Then
        coVerify { caregiverContactDao.insertCaregiverContact(any()) }
    }
}
