import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import throttle.{Sla, SlaService, ThrottleServiceImpl, TimeProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MyTestSpec extends FlatSpec with Matchers with MockFactory {
  val myTimeProvider = new TimeProvider {
    override def millis: Long = 0
  }

  "ThrottleService" should "return false if" in {
    val user1 = "user1"

    val slaStub = createSlaStub(1, user1)
    val throttle = new ThrottleServiceImpl(1, slaStub)

    throttle.isRequestAllowed(Option(user1)) shouldBe true
    throttle.isRequestAllowed(Option(user1)) shouldBe false
  }

  "ThrottleService" should "call SlaService once for each user" in {
    val user1 = "user1"
    val user2 = "user2"

    val slaStub = createSlaStub(1, user1, user2)
    val throttle = new ThrottleServiceImpl(1, slaStub)

    throttle.isRequestAllowed(Option(user1))
    throttle.isRequestAllowed(Option(user2))
    throttle.isRequestAllowed(Option(user1))
    throttle.isRequestAllowed(Option(user2))

    (slaStub.getSlaByToken _).verify(user1).once()
    (slaStub.getSlaByToken _).verify(user2).once()
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
