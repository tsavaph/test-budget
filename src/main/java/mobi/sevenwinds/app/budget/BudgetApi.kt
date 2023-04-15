package mobi.sevenwinds.app.budget

import com.fasterxml.jackson.annotation.JsonInclude
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.annotations.type.string.length.MinLength
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import mobi.sevenwinds.app.budget.author.AuthorService
import java.util.*

fun NormalOpenAPIRoute.budget() {
    route("/budget") {
        route("/add").post<Unit, BudgetRecord, BudgetRecord>(info("Добавить запись")) { param, body ->
            respond(BudgetService.addRecord(body))
        }

        route("/year/{year}/stats") {
            get<BudgetYearParam, BudgetYearStatsResponse>(info("Получить статистику за год")) { param ->
                respond(BudgetService.getYearStats(param))
            }
        }
    }

    route("/author") {
        route("/add").post<Unit, AuthorRecord, AuthorRecord>(info("Добавить автора")) { param, body ->
            respond(AuthorService.addRecord(body))
        }
    }
}
data class BudgetRecord(
    @Min(1900) val year: Int,
    @Min(1) @Max(12) val month: Int,
    @Min(1) val amount: Int,
    val type: BudgetType,
    @JsonInclude(JsonInclude.Include.NON_NULL) val authorId: Int?,
) {
    constructor(year: Int, month: Int, amount: Int, type: BudgetType)
            : this(year, month, amount, type, null)
}

data class BudgetRecordWithAuthorNameAndDate(
    val year: Int,
    val month: Int,
    val amount: Int,
    val type: BudgetType,
    @JsonInclude(JsonInclude.Include.NON_NULL) val authorName: String?,

    @JsonInclude(JsonInclude.Include.NON_NULL) val authorCreatedAt: String?,
) {
    constructor(budgetRecord: BudgetRecord)
            : this(budgetRecord.year, budgetRecord.month, budgetRecord.amount, budgetRecord.type, null, null)
    constructor(budgetRecord: BudgetRecord, authorName: String?, authorCreatedAtString: String?)
            : this(budgetRecord.year, budgetRecord.month, budgetRecord.amount, budgetRecord.type, authorName, authorCreatedAtString)
}
data class BudgetYearParam(
    @PathParam("Год") val year: Int,
    @QueryParam("Лимит пагинации") val limit: Int,
    @QueryParam("Смещение пагинации") val offset: Int,

    @QueryParam("ФИО автора")
    @MinLength(2)
    val authorName: String?,
)

class BudgetYearStatsResponse(
    val total: Int,
    val totalByType: Map<String, Int>,
    val items: List<BudgetRecordWithAuthorNameAndDate>,
)


data class AuthorRecord(
    @MinLength(2) val authorName: String
)

enum class BudgetType {
    Приход, Расход, Комиссия
}