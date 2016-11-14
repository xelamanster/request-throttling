package throttle

object ThrottleService {
  val UnauthorizedUserToken = "UnauthorizedUser"
  val DefaultStep = 100
}

trait ThrottleService {
  protected val graceRps: Int
  protected val slaService: SlaService

  def isRequestAllowed(token: Option[String]): Boolean
}