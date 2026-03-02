package uk.appyapp.stepsafe.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    
    private lateinit var repository: SettingsRepository
    private lateinit var testDataStore: DataStore<Preferences>

    @Before
    fun setup() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_settings.preferences_pb") }
        )
        
        repository = SettingsRepository(context)
        repository.testDataStore = testDataStore
    }

    @Test
    fun `updateSafeZoneRadius should update radius in dataStore`() = runTest(testDispatcher) {
        // Given
        val newRadius = 250.0
        
        // When
        repository.updateSafeZoneRadius(newRadius)
        
        // Then
        val radius = repository.safeZoneRadius.first()
        assertEquals(newRadius, radius, 0.0)
    }

    @Test
    fun `updateMonitoringActive should update flag in dataStore`() = runTest(testDispatcher) {
        // Given
        val active = true
        
        // When
        repository.updateMonitoringActive(active)
        
        // Then
        val isActive = repository.monitoringActive.first()
        assertEquals(active, isActive)
    }
}
