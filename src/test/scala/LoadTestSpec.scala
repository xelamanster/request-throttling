import java.util.concurrent.{Executors, TimeUnit}

import throttle.ThrottleServiceImpl

import scala.collection.mutable
import scala.util.Random

class LoadTestSpec extends TestSpec {
  val users = 4
  val rps = 10
  val duration = 5

  s"For $users users, $rps rps during $duration seconds" should
    s"retturn arroud ${users * rps * duration} successful requests" in {
    val ses = Executors.newSingleThreadScheduledExecutor()

    ses.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {

      }
    }
    , 0, 1, TimeUnit.SECONDS);

  }

  "Throttle overhead" should "be measured " in {
    //TODO should replace with JMH, but out of time
    val toMillsDivider = 1000000.0
    val usersCount = 100

    val users = (0 until usersCount).map("user" + _)
    val tokens = users.map(Option(_))

    val warmUpRuns = 15000
    val normalRuns = 10000

    val resultList = mutable.ListBuffer[Boolean]()
    val slaStub = createSlaStub(1000, users:_*)
    val throttle = new ThrottleServiceImpl(100000, slaStub)

    def randomToken(): Option[String] = tokens(Random.nextInt(usersCount))

    //warmUp
    for(x <- 0 until warmUpRuns)
      resultList += throttle.isRequestAllowed(randomToken())

    //benchmark
    val startTime = System.nanoTime()

    for(x <- 0 until normalRuns)
      resultList += throttle.isRequestAllowed(randomToken())

    val timePassed = System.nanoTime() - startTime
    val timePerRun = timePassed / normalRuns / toMillsDivider

    resultList.size shouldBe warmUpRuns + normalRuns
    println("Throttle overhead: " + timePerRun)
  }
}
