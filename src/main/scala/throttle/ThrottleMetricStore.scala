package throttle

import scala.collection.mutable

trait MetricStore[T] {
  def contains(key: T): Boolean
  def acquire(key: T): Boolean
  def put(key: T, rps: Int): Unit
}

/**
  * Holds throttle metrics.
  */
class ThrottleMetricStore[T](step: Long) extends MetricStore[T] {
  val store = mutable.Map[T, ThrottleMetric]()

  override def contains(key: T): Boolean =
    store.contains(key)

  override def acquire(key: T): Boolean = store.get(key) match {
    case Some(metric) => metric.acquire
    case None => false
  }

  override def put(key: T, rps: Int): Unit = {
    store += key -> ThrottleMetric(rps, step)
  }
}

/**
  * Holds and updates throttle metrics for one user.
  * Control max available requests per update step value.
  *
  * @param rps maximum requests per second
  * @param step update step
  * @param time time providing utility
  */
case class ThrottleMetric(rps: Int, step: Long, time: TimeProvider = SystemTimeProvider) {
  private val stepDelta = step / 1000.0
  private val ratePerStep = rps * stepDelta
  private val maxAmount = 1

  private var lastCheckAt = time.millis
  private var availability: Double = maxAmount

  /**
    * Updates metrics and return is there available request at current time.
    *
    * @return is request available
    */
  def acquire: Boolean = {
    update()
    decrease()
  }

  private def decrease(): Boolean = {
    if (availability >= 1) {availability -= 1; true}
    else false
  }

  private def update(): Unit = {
    val current = time.millis
    val passed = current - lastCheckAt
    val stepsPassed = passed / step

    if(stepsPassed > 0) {
      lastCheckAt = current
      val newAvailability = availability + stepsPassed * ratePerStep
      availability = math.min(newAvailability, maxAmount)
    }
  }
}
