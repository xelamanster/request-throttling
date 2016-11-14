package throttle

object ThrottleService {
  val UnauthorizedUserToken = "UnauthorizedUserToken"
  val DefaultStep = 100
}

trait ThrottleService {
  protected val slaService: SlaService
  protected val graceRps: Int

  def isRequestAllowed(token: Option[String]): Boolean
}