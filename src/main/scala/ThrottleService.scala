import scala.concurrent.Future

trait ThrottleService {
  val graceRps: Int
  val slaService: SlaService

  def isRequestAllowed(token: Option[String]): Boolean
}

case class Sla(user: String, rps: Int)
trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}
