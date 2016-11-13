package throttle

class ThrottleServiceImpl extends ThrottleService{
  override val graceRps: Int = 5

  override def isRequestAllowed(token: Option[String]): Boolean = ???

  override val slaService: SlaService = _
}
