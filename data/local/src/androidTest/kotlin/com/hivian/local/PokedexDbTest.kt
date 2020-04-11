package com.hivian.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hivian.common_test.datasets.FakeData
import com.hivian.local.dao.PokedexDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class PokedexDbTest {

    private lateinit var pokedexDatabase: PokedexDatabase
    private lateinit var pokedexDao: PokedexDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        pokedexDatabase = Room.inMemoryDatabaseBuilder(context, PokedexDatabase::class.java)
            .build()
        pokedexDao = pokedexDatabase.pokedexDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        pokedexDatabase.close()
    }

    @Test
    fun daoCall() {
        assertEquals(pokedexDatabase.pokedexDao(), pokedexDao)
    }

    @Test
    fun runSingleItemCRUDTest() {
        val fakeData = FakeData.createFakePokemonDb()
        runBlocking {
            // Insert
            val insertedId = pokedexDao.insert(fakeData)
            assertNotNull(insertedId)
            assertEquals(1, insertedId)

            // Find by name
            val findByName = pokedexDao.getPokemonByName(fakeData.name)
            assertNotNull(findByName)
            assertEquals(fakeData.name, findByName.name)

            // Update
            val new = findByName.apply { name = "Updated" }
            val numberOfRowUpdated = pokedexDao.update(new)
            assertEquals(1, numberOfRowUpdated)
            assertEquals(new.name, findByName.name)

            // Delete
            val numberOfRowDeleted = pokedexDao.delete(findByName)
            assertEquals(1, numberOfRowDeleted)
        }
    }

    @Test
    fun runMultipleItemCRUDTest() {
        val fakeDataList = FakeData.createFakePokemonsDb(5)
        runBlocking {
            // Insert all
            val insertedAllId = pokedexDao.insert(fakeDataList)
            assertNotNull(insertedAllId)
            assertEquals(fakeDataList.size, insertedAllId.size)

            // Find all
            val findAll = pokedexDao.getTopPokemons()
            assertNotNull(findAll)
            assertEquals(fakeDataList.size, findAll.size)

            // Delete all
            val numberOfRowDeleted = pokedexDao.deleteAll()
            assertEquals(fakeDataList.size, numberOfRowDeleted)
        }
    }

    @Test
    fun runCustomQueriesTest() {

    }

}