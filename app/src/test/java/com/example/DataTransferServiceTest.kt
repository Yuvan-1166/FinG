package com.example

import com.example.data.backup.DataTransferService
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.DatabaseSnapshot
import com.example.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataTransferServiceTest {

    @Test
    fun `csv export and import detects duplicates and invalid rows`() {
        val accId = 1L
        val catId = 2L
        val tx = TransactionEntity(
            id = 10L,
            type = TransactionType.EXPENSE.name,
            amountPaise = 45_000L,
            timestamp = 1_764_000_000_000L,
            accountId = accId,
            categoryId = catId,
            merchant = "Blue Tokai",
            description = "Coffee",
            notes = "Client meeting",
            tags = "work,coffee",
        )
        val accounts = mapOf(accId to AccountEntity(id = accId, name = "HDFC", type = "SAVINGS", openingBalancePaise = 0L))
        val categories = mapOf(catId to CategoryEntity(id = catId, name = "Food", type = "EXPENSE"))

        val csv = DataTransferService.exportTransactionsToCsv(
            transactions = listOf(tx),
            accounts = accounts,
            categories = categories,
        )

        val duplicateCheck = DataTransferService.analyzeCsvImport(
            csvText = csv,
            existingTransactions = listOf(tx),
            accountsById = accounts,
        )
        assertEquals(0, duplicateCheck.validRows.size)
        assertEquals(1, duplicateCheck.duplicateRows.size)
        assertEquals(0, duplicateCheck.errors.size)

        val freshImport = DataTransferService.analyzeCsvImport(
            csvText = csv,
            existingTransactions = emptyList(),
            accountsById = accounts,
        )
        assertEquals(1, freshImport.validRows.size)
        assertEquals(45_000L, freshImport.validRows.first().amountPaise)
        assertEquals("Blue Tokai", freshImport.validRows.first().merchant)
    }

    @Test
    fun `csv import rejects negative amounts and invalid dates`() {
        val badCsv = """
            date_time,type,amount_inr,account,destination_account,category,subcategory,merchant,description,notes,tags
            not-a-date,EXPENSE,100.00,Cash,,,,,,,
            2026-11-24 10:00:00,EXPENSE,-50.00,Cash,,,,,,,
            2026-11-24 10:00:00,TRANSFER,500.00,Cash,Cash,,,,,,
        """.trimIndent()

        val result = DataTransferService.analyzeCsvImport(badCsv, emptyList(), emptyMap())
        assertEquals(0, result.validRows.size)
        assertEquals(3, result.errors.size)
    }

    @Test
    fun `json snapshot backup roundtrips cleanly`() {
        val snapshot = DatabaseSnapshot(
            accounts = listOf(AccountEntity(id = 1L, name = "SBI", type = "SAVINGS", openingBalancePaise = 1_000_000L)),
            categories = listOf(CategoryEntity(id = 1L, name = "Salary", type = "INCOME", isDefault = true)),
            transactions = listOf(TransactionEntity(id = 1L, type = "INCOME", amountPaise = 5_000_000L, timestamp = 1_764_000_000_000L, accountId = 1L, categoryId = 1L)),
        )
        val encoded = DataTransferService.exportSnapshotToJson(snapshot)
        val decoded = DataTransferService.parseSnapshotFromJson(encoded)
        assertTrue(decoded.isSuccess)
        assertEquals(snapshot.accounts, decoded.getOrThrow().accounts)
        assertEquals(snapshot.transactions, decoded.getOrThrow().transactions)
    }
}
