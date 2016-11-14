package throttle

import scala.collection.mutable

class ThrottleMetricStore[T](step: Long) {
  val store = mutable.Map[T, ThrottleMetric]()

  def contains(key: T): Boolean =
    store.contains(key)

  def acquire(key: T): Boolean =
    store(key).isAvailable

  def put(key: T, rps: Int): Unit =
    store += key -> ThrottleMetric(rps, step)
}

case class ThrottleMetric(rps: Int, step: Long, time: TimeProvider = SystemTime) {
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
