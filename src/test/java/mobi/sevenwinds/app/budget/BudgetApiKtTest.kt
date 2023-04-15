package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { BudgetTable.deleteAll() }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testBudgetPaginationWithAuthorName() {
        DateTimeUtils.setCurrentMillisFixed(0)
        val authorName = "Круглов Квадрат Тестович"
        addAuthorRecord(AuthorRecord(authorName))
        val authorId = 1

        addRecord(BudgetRecord(2020, 1, 1, BudgetType.Приход, authorId))
        addRecord(BudgetRecord(2020, 2, 2, BudgetType.Приход, authorId))
        addRecord(BudgetRecord(2020, 3, 3, BudgetType.Приход, authorId))
        addRecord(BudgetRecord(2020, 4, 4, BudgetType.Приход, authorId))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 6, 6, BudgetType.Приход))

        RestAssured.given()
            .queryParam("limit", 2)
            .queryParam("offset", 1)
            .queryParam("authorName", "КвАдРат")
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(4, response.total)
                Assert.assertEquals(2, response.items.size)
                Assert.assertEquals(10, response.totalByType[BudgetType.Приход.name])

                Assert.assertEquals("Круглов Квадрат Тестович", response.items[0].authorName)
                Assert.assertEquals(getDateTimeAsString(), response.items[0].authorCreatedAt)
                Assert.assertEquals("Круглов Квадрат Тестович", response.items[1].authorName)
                Assert.assertEquals(getDateTimeAsString(), response.items[1].authorCreatedAt)
            }

        DateTimeUtils.setCurrentMillisSystem()
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                Assert.assertEquals(record, response)
            }
    }

    private fun addAuthorRecord(authorRecord: AuthorRecord) {
        RestAssured.given()
            .jsonBody(authorRecord)
            .post("/author/add")
            .toResponse<AuthorRecord>().let { response ->
                Assert.assertEquals(authorRecord, response)
            }
    }

    private fun getDateTimeAsString() : String {
        val dateTime = DateTime.now()
        val dtf: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")
        return dtf.print(dateTime)
    }

}