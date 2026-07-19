package com.localpos.pro.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products", indices = [Index(value = ["barcode"], unique = true)])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String,
    val price: Long,
    val stock: Int,
    val lowStockAt: Int = 5,
    val imageUri: String? = null
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val total: Long,
    val paymentMethod: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val amount: Long,
    val note: String = "",
    val settled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name") fun observeAll(): Flow<List<ProductEntity>>
    @Query("SELECT COUNT(*) FROM products") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(product: ProductEntity): Long
    @Update suspend fun update(product: ProductEntity)
    @Delete suspend fun delete(product: ProductEntity)
    @Query("DELETE FROM products") suspend fun clear()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC") fun observeAll(): Flow<List<SaleEntity>>
    @Insert suspend fun insert(sale: SaleEntity)
    @Query("DELETE FROM sales") suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY settled, createdAt DESC") fun observeAll(): Flow<List<DebtEntity>>
    @Insert suspend fun insert(debt: DebtEntity)
    @Update suspend fun update(debt: DebtEntity)
    @Query("DELETE FROM debts") suspend fun clear()
}

@Database(entities = [ProductEntity::class, SaleEntity::class, DebtEntity::class], version = 2, exportSchema = true)
abstract class LocalPosDatabase : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun sales(): SaleDao
    abstract fun debts(): DebtDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            LocalPosDatabase::class.java,
            "localpos.db"
        ).addMigrations(object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN imageUri TEXT")
            }
        }).build()
    }
}
