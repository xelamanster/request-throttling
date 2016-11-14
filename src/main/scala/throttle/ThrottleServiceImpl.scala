package throttle

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class ThrottleServiceImpl(_graceRps: Int, _slaService: SlaService) extends ThrottleService {
  override protected val graceRps: Int = _graceRps
  override protected val slaService: SlaService = _slaService
  private val store = new ThrottleMetricStore[String](100)
  private val slaRequests = mutable.ArrayBuffer[String]()

  store.put("default", graceRps)

  override def isRequestAllowed(token: Option[String]): Boolean = token match {
    case Some(user) => isRequestAllowed(user)
    case None => store.acquire("default")
  }

  @tailrec
  private def isRequestAllowed(user: String): Boolean =
    if(store.contains(user))
      store.acquire(user)
    else if(slaRequests.contains(user))
      store.acquire("default")
    else {
      sendSlaRequest(user)
      isRequestAllowed(user)
    }

  private def sendSlaRequest(user: String): Unit =
    if (!slaRequests.contains(user)) {
      slaRequests += user
      slaService.getSlaByToken(user).onComplete(processSla(_, user))
    }

  private def processSla(result: Try[Sla], user: String): Unit = {
    slaRequests -= user
    result match {
      case Success(sla) =>
        store.synchronized {
          store.put(sla.user, sla.rps)
        }
      case Failure(e) => println(e)
    }
  }
}
