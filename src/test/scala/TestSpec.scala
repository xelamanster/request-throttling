import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import throttle.{Sla, SlaService, TimeProvider}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class TestSpec extends FlatSpec with Matchers with MockFactory {
  val second = 1000

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

class TestTimeProvider extends TimeProvider {
  var time: Long = 0

  override def millis: Long = time

  def increase(time: Long): Unit =
    this.time = time
}
