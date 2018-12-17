import _root_.io.gatling.core.scenario.Simulation
import ch.qos.logback.classic.{Level, LoggerContext}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import java.util.{Calendar, SimpleTimeZone}

/**
  * Performance test for the Scheduler.
  */
class SchedulerDelayedTasksGatlingTest extends Simulation {

    val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

    // Log all HTTP requests
    //context.getLogger("io.gatling.http").setLevel(Level.valueOf("TRACE"))
    //  Log failed HTTP requests
    //context.getLogger("io.gatling.http").setLevel(Level.valueOf("DEBUG"))

    val dateFmt = "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'"
    def taskDate(): String = {

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 5)
        val sdf = new SimpleDateFormat(dateFmt)
        sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
        sdf.format(calendar.getTime())
    }

    var requestBody = """{"typeKey": "TASK_TYPE", "createdBy":"gatling", "scheduleType":"ONE_TIME", "ttl":5000,"startDate":"""" + taskDate() +""""}""";

    val baseURL = Option(System.getProperty("baseURL")) getOrElse """http://localhost:8080"""

    val httpConf = http
        .baseURL(baseURL)
        .inferHtmlResources()
        .acceptHeader("*/*")
        .acceptEncodingHeader("gzip, deflate")
        .acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
        .connectionHeader("keep-alive")
        .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:33.0) Gecko/20100101 Firefox/33.0")

    val headers_http = Map(
        "Accept" -> """application/json"""
    )

    val authorization_header = "Basic " + "aW50ZXJuYWw6aW50ZXJuYWw="

    val headers_http_authentication = Map(
        "Content-Type" -> """application/x-www-form-urlencoded""",
        "Authorization"-> authorization_header
    )

    val headers_http_authenticated = Map(
        "Accept" -> """application/json""",
        "Authorization" -> "Bearer ${access_token}"
    )

    val scn = scenario("Test the scheduler")
        .exec(http("Authentication")
            .post("/uaa/oauth/token")
            .headers(headers_http_authentication)
            .formParam("username", "xm")
            .formParam("password", "P@ssw0rd")
            .formParam("grant_type", "password")
            .check(jsonPath("$.access_token").saveAs("access_token"))).exitHereIfFailed
        .pause(10)
        .repeat(10000) {
                exec(http("Create new task")
                    .post("/scheduler/api/tasks")
                    .headers(headers_http_authenticated)
                    .body(StringBody(requestBody.stripMargin)).asJSON
                    .check(status.is(201))
                    .check(jsonPath("$.id").saveAs("task_id"))).exitHereIfFailed
                .exec(http("Get created task")
                   .get("/scheduler/api/tasks/${task_id}")
                   .headers(headers_http_authenticated))
        }

    val users = scenario("Users").exec(scn)

    context.getLogger("io.gatling.http").info("Request {} ", requestBody)

    setUp(
        users.inject(atOnceUsers(1))
    ).protocols(httpConf)
}
