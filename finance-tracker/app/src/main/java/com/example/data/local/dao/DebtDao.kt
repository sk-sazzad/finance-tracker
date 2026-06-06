package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.DebtEntryEntity
import com.example.data.local.entities.DebtPersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    // DebtPerson queries
    @Query("SELECT * FROM debt_persons ORDER BY createdAt DESC")
    fun getAllDebtPersons(): Flow<List<DebtPersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPerson(person: DebtPersonEntity): Long

    @Update
    suspend fun updateDebtPerson(person: DebtPersonEntity)

    @Delete
    suspend fun deleteDebtPerson(person: DebtPersonEntity)

    // DebtEntry queries
    @Query("SELECT * FROM debt_entries WHERE personId = :personId ORDER BY date DESC")
    fun getEntriesForPerson(personId: Long): Flow<List<DebtEntryEntity>>

    @Query("SELECT * FROM debt_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DebtEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtEntry(entry: DebtEntryEntity): Long

    @Update
    suspend fun updateDebtEntry(entry: DebtEntryEntity)

    @Delete
    suspend fun deleteDebtEntry(entry: DebtEntryEntity)

    @Query("DELETE FROM debt_entries WHERE personId = :personId")
    suspend fun deleteAllEntriesForPerson(personId: Long)

    @Query("DELETE FROM debt_persons") suspend fun deleteAllDebtPersons()
    @Query("DELETE FROM debt_entries") suspend fun deleteAllDebtEntries()
}
