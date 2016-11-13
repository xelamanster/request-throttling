package throttle

trait ThrottleService {
  val graceRps: Int
  val slaService: SlaService

  def isRequestAllowed(token: Option[String]): Boolean
}