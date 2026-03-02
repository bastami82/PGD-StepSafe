package uk.appyapp.stepsafe.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.entities.HomeLocation

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeLocationDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var dao: HomeLocationDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.homeLocationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetHomeLocation() = runTest {
        val home = HomeLocation(id = 0, latitude = 51.0, longitude = 0.0, address = "Test Home")
        dao.insertHomeLocation(home)
        
        val result = dao.getHomeLocation().first()
        assertEquals(home.address, result?.address)
        assertEquals(home.latitude, result?.latitude ?: 0.0, 0.0)
    }
}
