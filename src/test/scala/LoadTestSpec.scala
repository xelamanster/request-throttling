import throttle.ThrottleServiceImpl

import scala.collection.mutable
import scala.util.Random

class LoadTestSpec extends TestSpec {

  it should "permit proper amount of requests" in {
    val usersCount = 3
    val rps = 10
    val duration = 5

    val users = (0 until usersCount).map("user" + _)
    val tokens = users.map(Option(_))
    val expectedRequestsAmount = usersCount * rps * duration
    val delta = math.round(expectedRequestsAmount * 0.05f)

    val slaStub = createSlaStub(rps, users:_*)
    val throttle = new ThrottleServiceImpl(rps, slaStub)

    val result = mutable.ListBuffer[Boolean]()

    val start = System.currentTimeMillis()
    def timePassed = System.currentTimeMillis() - start

    while(timePassed < duration * 1000 ) {
      for (x <- 0 until rps; t <- tokens)
        result += throttle.isRequestAllowed(t)

      Thread.sleep(Math.min(100, timePassed))
    }

    result.count(b => b) shouldBe expectedRequestsAmount +- delta
  }

  "Throttle overhead" should "be measured " in {
    //TODO should replace with JMH
    val toMillsDivider = 1000000.0

    val usersCount = 100
    val warmUpRuns = 15000
    val normalRuns = 10000

    val users = (0 until usersCount).map("user" + _)
    val tokens = users.map(Option(_))

    val result = mutable.ListBuffer[Boolean]()
    val slaStub = createSlaStub(1000, users:_*)
    val throttle = new ThrottleServiceImpl(100000, slaStub)

    def randomToken(): Option[String] = tokens(Random.nextInt(usersCount))

    def runFor(count: Int): Unit =
      for(x <- 0 until count)
        result += throttle.isRequestAllowed(randomToken())

    //warmUp
    runFor(warmUpRuns)

    //benchmark
    val startTime = System.nanoTime()
    runFor(normalRuns)
    val timePassed = System.nanoTime() - startTime
    val timePerRun = timePassed / normalRuns / toMillsDivider

    result.size shouldBe warmUpRuns + normalRuns
    println(s"Throttle overhead: $timePerRun millis")
  }
}
