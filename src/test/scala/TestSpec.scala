import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import throttle.{Sla, SlaService, TimeProvider}

import scala.concurrent.Future

class TestSpec extends FlatSpec with Matchers with MockFactory {
  def createSlaStub(rps: Int, users: String*): SlaService = {
    val serviceMock = stub[SlaService]
    for(user <- users)
      (serviceMock.getSlaByToken _)
        .when(user)
        .returning(
          Future.successful(Sla(user, rps))
        )

    serviceMock
  }
}

class TestTimeProvider extends TimeProvider {
  var time: Long = 0

  override def millis: Long = time

  def increase(time: Long): Unit =
    this.time = time
}
