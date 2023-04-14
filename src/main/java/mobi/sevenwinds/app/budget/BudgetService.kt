package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

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
            val query = BudgetTable.select { BudgetTable.year eq param.year }
            val data = BudgetEntity.wrapRows(query).map { it.toResponse() }
            val total = query.count()

            val limitedQuery = query.limit(param.limit, param.offset)
                .orderBy(BudgetTable.month to SortOrder.ASC)
                .orderBy(BudgetTable.amount to SortOrder.DESC)

            val limitedData = BudgetEntity.wrapRows(limitedQuery).map { it.toResponse() }
            val sumByType = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = limitedData
            )
        }
    }
}