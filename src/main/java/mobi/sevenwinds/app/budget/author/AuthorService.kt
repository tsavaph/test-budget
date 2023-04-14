package mobi.sevenwinds.app.budget.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.budget.AuthorRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.time.LocalDateTime

object AuthorService {
    suspend fun addRecord(body: AuthorRecord): AuthorRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = AuthorEntity.new {
                this.authorName = body.authorName
                this.createdAt = DateTime.now()
            }

            return@transaction entity.toResponse()
        }
    }
}