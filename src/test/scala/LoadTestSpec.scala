import java.time.Duration
import java.util.concurrent.{Executors, TimeUnit}

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Seconds, Span}
import throttle.ThrottleServiceImpl

import scala.collection.mutable
import scala.util.Random

class LoadTestSpec extends TestSpec with Eventually with IntegrationPatience {
  val usersCount = 1
  val rps = 2
  val duration = 1

//  s"For $usersCount users, $rps rps during $duration seconds" should
//    s"retturn arroud ${usersCount * rps * duration} successful requests" in {

  "For  seconds" should
    "retturn successful requests" in {

    val users = (0 until usersCount).map("user" + _)
    val tokens = users.map(Option(_))
    val fg = usersCount * rps * duration

    val slaStub = createSlaStub(rps, users:_*)
    val throttle = new ThrottleServiceImpl(rps, slaStub)

//    val ses = Executors.newSingleThreadScheduledExecutor()
    val results = mutable.ListBuffer[Boolean]()

    def randomToken(): Option[String] = tokens(Random.nextInt(usersCount))

    val start = System.currentTimeMillis()
    while(System.currentTimeMillis() - start < duration * 1000 ) {
      (0 until rps).foreach{i => tokens.foreach{
                    t => results += throttle.isRequestAllowed(t)}}
      Thread.sleep(Math.min(100, System.currentTimeMillis() - start))
    }
    val timePassed = (System.currentTimeMillis() - start) / 1000.0
    println(timePassed)


//    ses.scheduleAtFixedRate(new Runnable {
//      override def run(): Unit = {
//        (0 until rps).foreach{
//          i => tokens.foreach{
////            t => results += throttle.isRequestAllowed(t)}
//            t => results.synchronized {
//                          results += throttle.isRequestAllowed(t)
//                        }}
//        }
//        for(i <- 0 until rps; token <- tokens)
//        for(i <- 0 until fg; token <- tokens)
//          results.synchronized {
//            results += throttle.isRequestAllowed(token)
//          }
//      }
//    }
//      , 100 , 100, TimeUnit.MILLISECONDS)

//    eventually (timeout(Span(1, Seconds))) {results.count(b => b) shouldBe fg +- 10 }
//    ses.shutdown()
    //    Await.
//    val time = System.currentTimeMillis()
//    Thread.sleep(Duration.ofSeconds(duration).toMillis)
//    Thread.sleep(Duration.ofSeconds(duration).toMillis)
//    println("TIME " + (System.currentTimeMillis() - time))
//    ses.shutdown()
    println(results.filter(b => b).size + " " + fg)
    println(results.filter(b => b).size + " " + (usersCount * rps * timePassed))



  }

  "Throttle overhead" should "be measured " in {
    //TODO should replace with JMH, but out of time
    val toMillsDivider = 1000000.0
    val usersCount = 100
    val warmUpRuns = 15000
    val normalRuns = 10000

    val users = (0 until usersCount).map("user" + _)
    val tokens = users.map(Option(_))

    val resultList = mutable.ListBuffer[Boolean]()
    val slaStub = createSlaStub(1000, users:_*)
    val throttle = new ThrottleServiceImpl(100000, slaStub)

    def randomToken(): Option[String] = tokens(Random.nextInt(usersCount))

    def runFor(count: Int): Unit =
      for(x <- 0 until count)
        resultList += throttle.isRequestAllowed(randomToken())

    //warmUp
    runFor(warmUpRuns)

    //benchmark
    val startTime = System.nanoTime()
    runFor(normalRuns)
    val timePassed = System.nanoTime() - startTime
    val timePerRun = timePassed / normalRuns / toMillsDivider

    resultList.size shouldBe warmUpRuns + normalRuns
    println("Throttle overhead: " + timePerRun)
  }
}
