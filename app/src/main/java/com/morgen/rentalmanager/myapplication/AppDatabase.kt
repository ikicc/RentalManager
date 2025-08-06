package com.morgen.rentalmanager.myapplication

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import androidx.room.migration.Migration

@Database(entities = [Tenant::class, Bill::class, BillDetail::class, Price::class, MeterNameConfig::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tenantDao(): TenantDao
    abstract fun billDao(): BillDao
    abstract fun priceDao(): PriceDao
    abstract fun meterNameConfigDao(): MeterNameConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val LOCK = Any() // 明确的锁对象，减少同步开销
        
        // 迁移数据库从版本6到版本7 (添加租户房间号字段)
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建新表
                database.execSQL(
                    "CREATE TABLE meter_name_configs_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "meter_type TEXT NOT NULL, " +
                    "default_name TEXT NOT NULL, " +
                    "custom_name TEXT NOT NULL, " +
                    "tenant_room_number TEXT NOT NULL DEFAULT '', " +
                    "is_active INTEGER NOT NULL DEFAULT 1, " +
                    "created_date INTEGER NOT NULL, " +
                    "updated_date INTEGER NOT NULL)"
                )
                
                // 复制旧数据到新表，设置tenant_room_number为空字符串（全局配置）
                database.execSQL(
                    "INSERT INTO meter_name_configs_new (id, meter_type, default_name, custom_name, is_active, created_date, updated_date) " +
                    "SELECT id, meter_type, default_name, custom_name, is_active, created_date, updated_date FROM meter_name_configs"
                )
                
                // 删除旧表
                database.execSQL("DROP TABLE meter_name_configs")
                
                // 重命名新表为正式名称
                database.execSQL("ALTER TABLE meter_name_configs_new RENAME TO meter_name_configs")
            }
        }
        
        // 主线程优化的数据库实例获取
        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance // 快速路径，避免同步开销
            }
            
            synchronized(LOCK) {
                var instance = INSTANCE
                if (instance == null) {
                    // 使用快速构建选项，最大限度减少启动阻塞
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "tenant_database"
                    )
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_6_7) // 添加迁移策略
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // 使用WAL模式提高性能
                    .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
        
        // 改进的异步创建数据库实例，避免主线程阻塞
        fun createDatabaseAsync(context: Context, scope: CoroutineScope, callback: (AppDatabase) -> Unit) {
            // 首先检查实例是否已存在
            val existingInstance = INSTANCE
            if (existingInstance != null) {
                // 如果实例已存在，立即调用回调
                scope.launch(Dispatchers.Main) {
                    callback(existingInstance)
                }
                return
            }
            
            // 实例不存在，创建新实例
            scope.launch(Dispatchers.IO) {
                try {
                    val startTime = System.currentTimeMillis()
                    
                    // 创建优化的数据库实例
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "tenant_database"
                    )
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_6_7) // 添加迁移策略
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // 使用WAL模式提高性能
                    .setQueryExecutor(Executors.newSingleThreadExecutor()) // 使用单线程执行器
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d("AppDatabase", "首次创建数据库")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // 数据库打开后预热常用查询
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // 预热常用查询
                                    prewarmDatabaseQueries(INSTANCE!!)
                                } catch (e: Exception) {
                                    Log.e("AppDatabase", "预热查询失败", e)
                                }
                            }
                        }
                    })
                    .build()
                    
                    // 设置全局实例
                    synchronized(LOCK) {
                        if (INSTANCE == null) {
                            INSTANCE = db
                        }
                    }
                    
                    val elapsedTime = System.currentTimeMillis() - startTime
                    Log.d("AppDatabase", "数据库初始化完成，耗时：$elapsedTime ms")
                    
                    // 切换到主线程通知回调
                    withContext(Dispatchers.Main) {
                        callback(db)
                    }
                } catch (e: Exception) {
                    Log.e("AppDatabase", "创建数据库失败", e)
                    // 出现错误时仍然尝试使用备用方法获取数据库
                    val fallbackDb = getDatabase(context)
                    withContext(Dispatchers.Main) {
                        callback(fallbackDb)
                    }
                }
            }
        }
        
        // 数据库查询预热函数
        private suspend fun prewarmDatabaseQueries(db: AppDatabase) {
            try {
                withContext(Dispatchers.IO) {
                    // 1. 预热租户查询
                    val tenantCount = db.tenantDao().getTenantCount()
                    if (tenantCount > 0) {
                        // 只预取少量数据
                        val limitedTenants = db.tenantDao().getRecentTenants(5)
                    }
                    
                    // 2. 预热价格查询
                    val price = db.priceDao().getPrice()
                    
                    Log.d("AppDatabase", "数据库查询预热完成")
                }
            } catch (e: Exception) {
                // 忽略预热错误，不影响应用继续运行
                Log.w("AppDatabase", "数据库预热查询失败，但不影响应用运行", e)
            }
        }
    }
} 