package throttle

import scala.collection.mutable

trait MetricStore[T] {
  def contains(key: T): Boolean
  def acquire(key: T): Boolean
  def put(key: T, rps: Int): Unit
}

class ThrottleMetricStore[T](step: Long, defaultKey: T, defaultRps: Int) extends MetricStore[T] {
  val store = mutable.Map[T, ThrottleMetric]()
  put(defaultKey, defaultRps)

  override def contains(key: T): Boolean =
    store.contains(key)

  override def acquire(key: T): Boolean = store.get(key) match {
    case Some(metric) => metric.isAvailable
    case None => acquire(defaultKey)
  }

  override def put(key: T, rps: Int): Unit = {
    store += key -> ThrottleMetric(rps, step)
  }
}

case class ThrottleMetric(rps: Int, step: Long, time: TimeProvider = SystemTimeProvider) {
  private var lastCheckAt = time.millis
  private var availability: Double = rps

  def isAvailable: Boolean = {
    update()
    availability >= 1 && acquire
  }

  private def update(): Unit = {
    val current = time.millis
    val passed = current - lastCheckAt
    val stepsPassed = passed % step
    val newAvailability = availability + (stepsPassed * step) / 1000.0 * rps

    lastCheckAt = current
    availability = scala.math.min(newAvailability, rps)
  }

  private def acquire: Boolean = {
    availability -= 1
    true
  }
}
