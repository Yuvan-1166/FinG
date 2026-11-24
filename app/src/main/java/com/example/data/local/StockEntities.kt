package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "stocks",
    indices = [
        Index(value = ["symbol", "exchange"], unique = true),
    ],
)
data class StockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val symbol: String,
    val companyName: String,
    val exchange: String = "NSE",
    val sector: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "stock_holdings",
    foreignKeys = [
        ForeignKey(
            entity = StockEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["stockId"], unique = true),
    ],
)
data class StockHoldingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val stockId: Long,
    val quantityScale4: Long,
    val averageCostPaise: Long,
    val totalInvestedPaise: Long,
    val realizedGainLossPaise: Long = 0L,
    val currentManualPricePaise: Long = 0L,
    val priceUpdatedAtMillis: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "stock_transactions",
    foreignKeys = [
        ForeignKey(
            entity = StockEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["stockId"]),
        Index(value = ["tradeDateEpochDay"]),
    ],
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val stockId: Long,
    val type: String, // StockTxType: BUY or SELL
    val quantityScale4: Long,
    val pricePerSharePaise: Long,
    val chargesPaise: Long = 0L,
    val totalAmountPaise: Long,
    val realizedGainLossPaise: Long = 0L,
    val tradeDateEpochDay: Long,
    val timestamp: Long,
    val accountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "stock_price_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = StockEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["stockId"]),
        Index(value = ["updatedAtMillis"]),
    ],
)
data class StockPriceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val stockId: Long,
    val pricePaise: Long,
    val updatedAtMillis: Long,
    val note: String = "",
)
