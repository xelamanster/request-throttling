package throttle

import scala.util.{Failure, Success, Try}

class ThrottleServiceImpl(_graceRps: Int, _slaService: SlaService) extends ThrottleService {
  override protected val graceRps: Int = _graceRps
  override protected val slaService: SlaService = _slaService
  private val store = new ThrottleMetricStore[String]

  store.put("default", graceRps)

  override def isRequestAllowed(token: Option[String]): Boolean = token match {
    case Some(user) => isRequestAllowed(user)
    case None => store.acquire("default")
  }

  private def isRequestAllowed(user: String): Boolean = {
    if(store.contains(user))
      store.acquire(user)
    else {
      slaService.getSlaByToken(user).onComplete(processSla)
      store.acquire("default")
    }
  }

  private def processSla(result: Try[Sla]): Unit = result match {
    case Success(sla) => store.put(sla.user, sla.rps)
    case Failure(ex) => println(ex)
  }
}
