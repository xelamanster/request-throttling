package throttle

import throttle.ThrottleService.User

object ThrottleService {
  type User = String

  val UnauthorizedUserToken = "UnauthorizedUser"
  val DefaultStep = 100
}

trait ThrottleService {
  protected val graceRps: Int
  protected val slaService: SlaService

  def isRequestAllowed(token: Option[User]): Boolean
}