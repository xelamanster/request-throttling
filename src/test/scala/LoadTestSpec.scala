import throttle.ThrottleServiceImpl

import scala.collection.mutable

class LoadTestSpec extends TestSpec {
  val users = 4
  val rps = 10
  val duration = 5

  s"For $users users, $rps rps during $duration seconds" should
    s"retturn arroud ${users * rps * duration} successful requests" in {


  }

  "Throttle overhead" should "be measured " in {
    //should write with JMH, but out of time
    val toMillsDivider = 1000000.0
    val testUser1 = "user1"
    val token1 = Option(testUser1)
    val testUser2 = "user2"
    val token2 = Option(testUser2)

    val warmUpRuns = 15000
    val normalRuns = 10000

    val resultList = mutable.ListBuffer[Boolean]()
    val slaStub = createSlaStub(1000000, testUser1, testUser2)
    val throttle = new ThrottleServiceImpl(100000, slaStub)

    def randomToken(i: Int): Option[String] = if (i % 2 == 0) token1 else token2

    //warmUp
    for(i <- 0 to warmUpRuns)
      resultList += throttle.isRequestAllowed(randomToken(i))

    //benchmark
    val startTime = System.nanoTime()

    for(i <- 0 to normalRuns)
      resultList += throttle.isRequestAllowed(randomToken(i))

    val timePassed = System.nanoTime() - startTime
    val timePerRun = timePassed / normalRuns / toMillsDivider

    val r = resultList.map(!_).splitAt(resultList.size - 10)

    println(r)
    println("Throttle overhead: " + timePerRun)
  }
}
