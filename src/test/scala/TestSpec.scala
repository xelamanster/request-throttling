import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import throttle.ThrottleService._
import throttle._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestTimeProvider extends TimeProvider {
  var time: Long = 0

  override def millis: Long = time

  def increase(time: Long): Unit =
    this.time = time
}

class TestSpec extends FlatSpec with Matchers with MockFactory {
  val testUser1 = "user1"
  val testUser2 = "user2"

  "When check availability" should "update rps" in {
    val second = 1000
    val step = 100

    val initialValue = 10
    val time = new TestTimeProvider
    val metric = ThrottleMetric(initialValue, step, time)

    val initialCount = (1 to initialValue + 1)
      .map(i => metric.isAvailable)
      .count(b => b)

    initialCount shouldBe initialValue

    time.increase(step)

    val additionalCount = (1 to initialValue)
      .map(i => metric.isAvailable)
      .count(b => b)

    additionalCount shouldBe initialValue * (step / second)
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
    val slaStub = createSlaStub(1, testUser1)
    val throttle = new ThrottleServiceImpl(1, slaStub)

    throttle.isRequestAllowed(Option(testUser1)) shouldBe true // unauthorized request
    throttle.isRequestAllowed(Option(testUser1)) shouldBe true // user request
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

  def createSlaStub(rps: Int, users: String*): SlaService = {
    val serviceMock = stub[SlaService]
    for(user <- users)
      (serviceMock.getSlaByToken _)
        .when(user)
        .returning(
          Future(Sla(user, rps))
        )

    serviceMock
  }
}
