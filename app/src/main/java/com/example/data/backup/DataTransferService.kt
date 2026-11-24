package com.example.data.backup

import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.DatabaseSnapshot
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AccountType
import com.example.domain.model.TransactionType
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CsvImportRowError(
    val lineNumber: Int,
    val reason: String,
)

data class ParsedCsvTransaction(
    val lineNumber: Int,
    val type: TransactionType,
    val amountPaise: Long,
    val timestamp: Long,
    val accountName: String,
    val destinationAccountName: String?,
    val categoryName: String?,
    val merchant: String,
    val description: String,
    val notes: String,
    val tags: String,
)

data class CsvImportAnalysis(
    val validRows: List<ParsedCsvTransaction>,
    val duplicateRows: List<ParsedCsvTransaction>,
    val errors: List<CsvImportRowError>,
)

object DataTransferService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val csvTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun exportSnapshotToJson(snapshot: DatabaseSnapshot): String {
        return json.encodeToString(DatabaseSnapshot.serializer(), snapshot)
    }

    fun parseSnapshotFromJson(raw: String): Result<DatabaseSnapshot> = runCatching {
        val parsed = json.decodeFromString(DatabaseSnapshot.serializer(), raw)
        require(parsed.schemaVersion >= 1) { "Unsupported backup schema version: ${parsed.schemaVersion}" }
        parsed
    }

    fun exportTransactionsToCsv(
        transactions: List<TransactionEntity>,
        accounts: Map<Long, AccountEntity>,
        categories: Map<Long, CategoryEntity>,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("date_time,type,amount_inr,account,destination_account,category,subcategory,merchant,description,notes,tags")
        val zone = ZoneId.systemDefault()
        for (tx in transactions.sortedByDescending { it.timestamp }) {
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tx.timestamp), zone)
                .format(csvTimestampFormatter)
            val amountStr = MoneyFormatter.paiseToPlainInput(tx.amountPaise)
            val accountName = accounts[tx.accountId]?.name ?: "Unknown"
            val destName = tx.destinationAccountId?.let { accounts[it]?.name } ?: ""
            val catName = tx.categoryId?.let { categories[it]?.name } ?: ""
            val subName = tx.subcategoryId?.let { categories[it]?.name } ?: ""
            val row = listOf(
                dt,
                tx.type,
                amountStr,
                accountName,
                destName,
                catName,
                subName,
                tx.merchant,
                tx.description,
                tx.notes,
                tx.tags,
            ).joinToString(",") { escapeCsvField(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    fun analyzeCsvImport(
        csvText: String,
        existingTransactions: List<TransactionEntity>,
        accountsById: Map<Long, AccountEntity>,
    ): CsvImportAnalysis {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return CsvImportAnalysis(emptyList(), emptyList(), listOf(CsvImportRowError(1, "CSV file is empty.")))
        }

        val header = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val expectedCols = listOf("date_time", "type", "amount_inr", "account")
        if (!expectedCols.all { it in header }) {
            return CsvImportAnalysis(
                emptyList(),
                emptyList(),
                listOf(CsvImportRowError(1, "Missing required CSV columns: date_time, type, amount_inr, account")),
            )
        }

        val idxDate = header.indexOf("date_time")
        val idxType = header.indexOf("type")
        val idxAmount = header.indexOf("amount_inr")
        val idxAccount = header.indexOf("account")
        val idxDest = header.indexOf("destination_account")
        val idxCat = header.indexOf("category")
        val idxMerchant = header.indexOf("merchant")
        val idxDesc = header.indexOf("description")
        val idxNotes = header.indexOf("notes")
        val idxTags = header.indexOf("tags")

        val existingSignatures = existingTransactions.mapTo(HashSet()) { tx ->
            val accName = accountsById[tx.accountId]?.name?.trim()?.lowercase() ?: ""
            buildDuplicateSignature(tx.timestamp, tx.type, tx.amountPaise, accName, tx.merchant.trim().lowercase())
        }

        val valid = mutableListOf<ParsedCsvTransaction>()
        val duplicates = mutableListOf<ParsedCsvTransaction>()
        val errors = mutableListOf<CsvImportRowError>()
        val zone = ZoneId.systemDefault()

        for (i in 1 until lines.size) {
            val lineNum = i + 1
            val cols = parseCsvLine(lines[i])
            if (cols.size <= maxOf(idxDate, idxType, idxAmount, idxAccount)) {
                errors.add(CsvImportRowError(lineNum, "Insufficient columns."))
                continue
            }

            val dateStr = cols[idxDate].trim()
            val timestamp = runCatching {
                LocalDateTime.parse(dateStr, csvTimestampFormatter).atZone(zone).toInstant().toEpochMilli()
            }.recoverCatching {
                LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(zone).toInstant().toEpochMilli()
            }.getOrNull()

            if (timestamp == null) {
                errors.add(CsvImportRowError(lineNum, "Invalid date_time '$dateStr'. Expected yyyy-MM-dd HH:mm:ss"))
                continue
            }

            val type = runCatching { TransactionType.valueOf(cols[idxType].trim().uppercase()) }.getOrNull()
            if (type == null) {
                errors.add(CsvImportRowError(lineNum, "Unknown transaction type '${cols[idxType]}'."))
                continue
            }

            val amountPaise = MoneyFormatter.parseRupeesToPaise(cols[idxAmount])
            if (amountPaise == null || amountPaise <= 0L) {
                errors.add(CsvImportRowError(lineNum, "Invalid positive amount '${cols[idxAmount]}'."))
                continue
            }

            val accountName = cols[idxAccount].trim()
            if (accountName.isEmpty()) {
                errors.add(CsvImportRowError(lineNum, "Account name cannot be empty."))
                continue
            }

            val destName = if (idxDest in cols.indices) cols[idxDest].trim().ifEmpty { null } else null
            if (type == TransactionType.TRANSFER && (destName == null || destName.equals(accountName, ignoreCase = true))) {
                errors.add(CsvImportRowError(lineNum, "Transfer requires a distinct destination account."))
                continue
            }

            val merchant = if (idxMerchant in cols.indices) cols[idxMerchant].trim() else ""
            val row = ParsedCsvTransaction(
                lineNumber = lineNum,
                type = type,
                amountPaise = amountPaise,
                timestamp = timestamp,
                accountName = accountName,
                destinationAccountName = destName,
                categoryName = if (idxCat in cols.indices) cols[idxCat].trim().ifEmpty { null } else null,
                merchant = merchant,
                description = if (idxDesc in cols.indices) cols[idxDesc].trim() else "",
                notes = if (idxNotes in cols.indices) cols[idxNotes].trim() else "",
                tags = if (idxTags in cols.indices) cols[idxTags].trim() else "",
            )

            val sig = buildDuplicateSignature(
                timestamp,
                type.name,
                amountPaise,
                accountName.lowercase(),
                merchant.lowercase(),
            )
            if (sig in existingSignatures) {
                duplicates.add(row)
            } else {
                existingSignatures.add(sig)
                valid.add(row)
            }
        }

        return CsvImportAnalysis(valid, duplicates, errors)
    }

    private fun buildDuplicateSignature(
        timestamp: Long,
        type: String,
        amountPaise: Long,
        accountNameLower: String,
        merchantLower: String,
    ): String {
        // Round timestamp to nearest second for CSV roundtrip consistency
        val epochSec = timestamp / 1000L
        return "$epochSec|$type|$amountPaise|$accountNameLower|$merchantLower"
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        result.add(sb.toString())
                        sb.clear()
                    }
                    else -> sb.append(ch)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    fun inferAccountTypeFromName(name: String): AccountType {
        val lower = name.lowercase()
        return when {
            "cash" in lower -> AccountType.CASH
            "upi" in lower || "gpay" in lower || "phonepe" in lower -> AccountType.UPI
            "card" in lower || "credit" in lower -> AccountType.CREDIT_CARD
            "current" in lower -> AccountType.CURRENT
            else -> AccountType.SAVINGS
        }
    }
}
