package throttle

import throttle.ThrottleService.{DefaultStep, UnauthorizedUserToken, User}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Check available requests and update their metrics for users.
  *
  * @param graceRps rps for unauthorized users
  * @param slaService service provides SLA data
  * @param store provide storing and updating of the user's requests data
  */
class ThrottleServiceImpl(
    override protected val graceRps: Int,
    override protected val slaService: SlaService,
    store: MetricStore[User])
  extends ThrottleService {

  def this(graceRps: Int, slaService: SlaService) {
    this(graceRps, slaService, new ThrottleMetricStore(DefaultStep))
  }

  private val slaRequests = mutable.ListBuffer[User]()

  store.put(UnauthorizedUserToken, graceRps)

  /**
    * Check is request allowed
    *
    * If token holds user, then will try to check user's SLA
    * in other case - will check request as from
    * unauthorized user.
    *
    * @param token request's token
    */
  override def isRequestAllowed(token: Option[User]): Boolean = token match {
    case Some(user) => isRequestAllowed(user)
    case None => store.acquire(UnauthorizedUserToken)
  }

  private def isRequestAllowed(user: User): Boolean =
    if(store.contains(user)) store.acquire(user)
    else {sendSlaRequest(user); store.acquire(UnauthorizedUserToken)}

  private def sendSlaRequest(user: User): Unit =
    if(!slaRequests.contains(user)) {
      slaRequests += user

      slaService.getSlaByToken(user).onComplete {result =>
        processSla(result)
        slaRequests synchronized {
          slaRequests -= user
        }
      }
    }

  private def processSla(result: Try[Sla]): Unit = result match {
    case Success(sla) => store synchronized {store.put(sla.user, sla.rps)}
    case Failure(e) => println(e)
  }
}
