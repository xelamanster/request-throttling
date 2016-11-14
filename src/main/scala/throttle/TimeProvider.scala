package throttle

trait TimeProvider {
  def millis: Long
}

object SystemTimeProvider extends TimeProvider {
  override def millis: Long = System.currentTimeMillis()
}
