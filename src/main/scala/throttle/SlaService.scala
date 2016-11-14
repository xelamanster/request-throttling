package throttle

import throttle.ThrottleService.User

import scala.concurrent.Future

case class Sla(user: User, rps: Int)

trait SlaService {
  def getSlaByToken(token: User): Future[Sla]
}
