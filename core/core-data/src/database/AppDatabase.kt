package dev.spaceapps.secvault.core.data.database

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

const val VAULT_DB_FILE = "vault.db"

/**
 * One row per vault item. Only ciphertext and sync metadata, never plaintext.
 * version / dirty / deleted (tombstone) are here already so cloud sync can be added without a migration.
 */
@Entity(tableName = "vault_items")
class VaultItemEntity(
    @PrimaryKey val id: String,
    val version: Long,
    val wrappedItemKey: ByteArray,
    val ciphertext: ByteArray,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
)

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items WHERE deleted = 0")
    fun observeActive(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun get(id: String): VaultItemEntity?

    @Upsert
    suspend fun upsert(entity: VaultItemEntity)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()
}

@Database(entities = [VaultItemEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
