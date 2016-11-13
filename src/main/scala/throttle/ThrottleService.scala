package throttle

trait ThrottleService {
  protected val graceRps: Int
  protected val slaService: SlaService

  def isRequestAllowed(token: Option[String]): Boolean
}