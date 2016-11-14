import org.scalatest.OneInstancePerTest
import throttle.ThrottleService._
import throttle._

class MainTestSpec extends TestSpec with OneInstancePerTest {
  val testUser1 = "user1"
  val testUser2 = "user2"

  "When check availability" should "update rps" in {
    val rps = 10
    val step = 100
    val second = 1000

    val time = new TestTimeProvider
    val metric = ThrottleMetric(rps, step, time)

    val ratePerStep = rps * (step.toDouble / second)

    def check(): Int = (1 to rps)
      .map(i => metric.acquire)
      .count(b => b)

    val initialCount = check()
    time.increase(step)
    val additionalCount = check()

    initialCount shouldBe ratePerStep
    additionalCount shouldBe ratePerStep
  }

  "When no token" should "assume the client as unauthorized" in {
    val slaStub = createSlaStub(1)
    val store = stub[MetricStore[String]]
    val throttle = new ThrottleServiceImpl(1, slaStub, store)

    throttle.isRequestAllowed(None)
    throttle.isRequestAllowed(None)

    (store.acquire _).verify(UnauthorizedUserToken).twice()
  }

  "When no loaded SLA for user" should "assume the client as unauthorized" in {
    val slaStub = createSlaStub(1, testUser1)
    val store = stub[MetricStore[String]]
    val throttle = new ThrottleServiceImpl(1, slaStub, store)

    throttle.isRequestAllowed(Option(testUser1))

    (store.acquire _).verify(UnauthorizedUserToken).once()
  }

  "When rps elapsed" should "return false " in {
    val slaStub = createSlaStub(10, testUser1)
    val throttle = new ThrottleServiceImpl(10, slaStub)

    throttle.isRequestAllowed(Option(testUser1)) shouldBe true // unauthorized or user request. May differ because of race conditions
    throttle.isRequestAllowed(Option(testUser1)) // user request
    throttle.isRequestAllowed(Option(testUser1)) // user request
    throttle.isRequestAllowed(Option(testUser1)) shouldBe false // user request
  }

  "When multiply requests by one user" should "call SlaService once" in {
    val slaStub = createSlaStub(1, testUser1, testUser2)
    val throttle = new ThrottleServiceImpl(1, slaStub)

    throttle.isRequestAllowed(Option(testUser1))
    throttle.isRequestAllowed(Option(testUser2))
    throttle.isRequestAllowed(Option(testUser1))
    throttle.isRequestAllowed(Option(testUser2))

    (slaStub.getSlaByToken _).verify(testUser1).once()
    (slaStub.getSlaByToken _).verify(testUser2).once()
  }
}
