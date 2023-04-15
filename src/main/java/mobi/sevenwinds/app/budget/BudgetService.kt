package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.budget.author.AuthorEntity
import mobi.sevenwinds.app.budget.author.AuthorTable
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.authorId = body.authorId
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val authorName = param.authorName

            val query = BudgetTable.select { BudgetTable.year eq param.year }
            val data = getFilteredData(query, authorName)
            val total = data.count()

            val limitedQuery = query.limit(param.limit, param.offset)
                .orderBy(BudgetTable.month to SortOrder.ASC)
                .orderBy(BudgetTable.amount to SortOrder.DESC)

            val limitedData = getFilteredData(limitedQuery, authorName)
            val sumByType = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = limitedData,
            )
        }
    }

    private fun getFilteredData(query: Query, authorName: String?) : List<BudgetRecordWithAuthorNameAndDate> {

        val data = BudgetEntity.wrapRows(query).map { it.toResponse() }.map { budgetRecord ->
            val authorId =  budgetRecord.authorId

            if (authorId == null) {
                return@map BudgetRecordWithAuthorNameAndDate(budgetRecord)
            } else {
                val authorQuery = AuthorTable.select { AuthorTable.id eq authorId }
                val authorEntity = AuthorEntity.wrapRow(authorQuery.first())

                val fullAuthorName = authorEntity.authorName
                val authorCreatedAt = authorEntity.createdAt

                val dtf: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")
                val authorCreatedAtString = dtf.print(authorCreatedAt)

                return@map BudgetRecordWithAuthorNameAndDate(budgetRecord, fullAuthorName, authorCreatedAtString)
            }
        }

        return if (authorName == null) {
            data
        } else {
            data.filter { budgetRecordWithAuthorNameAndDate ->
                return@filter budgetRecordWithAuthorNameAndDate.authorName?.contains(authorName, ignoreCase = true) == true
            }
        }
    }
}