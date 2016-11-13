package throttle

import scala.collection.mutable

class ThrottleMetricStore[T] {
  val store = mutable.Map[T, ThrottleMetric]()


}

case class ThrottleMetric(rps: Int) {
  var lastCheck = System.currentTimeMillis()
  var allowance: Double = rps

  def available(): Boolean = {
    update()
    if(allowance < 1) false
    else {
      allowance -= 1
      true
    }
  }

  private def update(): Unit = {
    val time = System.currentTimeMillis()
    val timePassed = time - lastCheck
    lastCheck = time
    allowance += timePassed / 1000 * rps
  }
}
