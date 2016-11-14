package throttle

import throttle.ThrottleService.{DefaultStep, UnauthorizedUserToken}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class ThrottleServiceImpl(
    override protected val graceRps: Int,
    override protected val slaService: SlaService,
    store: MetricStore[String])
  extends ThrottleService {

  def this(graceRps: Int, slaService: SlaService) {
    this(graceRps, slaService, new ThrottleMetricStore(DefaultStep))
  }

  private val slaRequests = mutable.ArrayBuffer[String]()

  store.put(UnauthorizedUserToken, graceRps)

  override def isRequestAllowed(token: Option[String]): Boolean = token match {
    case Some(user) => isRequestAllowed(user)
    case None => store.acquire(UnauthorizedUserToken)
  }

  private def isRequestAllowed(user: String): Boolean =
    if(store.contains(user))
      store.acquire(user)
    else {
      sendSlaRequest(user)
      store.acquire(UnauthorizedUserToken)
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
