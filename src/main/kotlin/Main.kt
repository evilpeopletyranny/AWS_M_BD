import database.DatabaseFactory
import database.model.dao.repository.CourseDAO
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun main() {
    DatabaseFactory.init(
        url = """jdbc:postgresql://localhost:5432/cqc_test?
                        reWriteBatchedInserts=true& +
                        rewriteBatchedStatements=true&
                        shouldReturnGeneratedValues=false"""
    )
}