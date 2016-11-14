package throttle

trait TimeProvider {
  def millis: Long
}

object SystemTime extends TimeProvider {
  override def millis: Long = System.currentTimeMillis()
}
