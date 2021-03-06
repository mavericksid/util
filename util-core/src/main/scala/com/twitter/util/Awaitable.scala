package com.twitter.util

import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.concurrent.Scheduler

/**
 * Wait for the result of some action. Awaitable is not used
 * directly, but through the `Await` object.
 */
trait Awaitable[+T] {
  import Awaitable._

  /**
   * Support for `Await.ready`. The use of the implicit permit is an
   * access control mechanism: only `Await.ready` may call this
   * method.
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def ready(timeout: Duration)(implicit permit: CanAwait): this.type

  /**
   * Support for `Await.result`. The use of the implicit permit is an
   * access control mechanism: only `Await.result` may call this
   * method.
   */
  @throws(classOf[Exception])
  def result(timeout: Duration)(implicit permit: CanAwait): T

  /**
   * Is this Awaitable ready? In other words: would calling
   * [[com.twitter.util.Awaitable.ready Awaitable.ready]] block?
   */
  def isReady(implicit permit: CanAwait): Boolean
}

object Awaitable {
  sealed trait CanAwait
}

/**
 * Await the result of some action.
 *
 * @define ready
 *
 * Returns the object when the action has completed.
 *
 * @define result
 *
 * Returns the result of the action when it has completed.
 *
 * @define all
 *
 * Returns after all actions have completed. The timeout given is
 * passed to each awaitable in turn, meaning await time will be
 * awaitables.size * timeout.
 */
object Await {
  import Awaitable._
  private implicit object AwaitPermit extends CanAwait

  /** $ready */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def ready[T <: Awaitable[_]](awaitable: T): T =
    ready(awaitable, Duration.Top)

  /** $ready */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def ready[T <: Awaitable[_]](awaitable: T, timeout: Duration): T = {
    if (awaitable.isReady) awaitable.ready(timeout)
    else Scheduler.blocking { awaitable.ready(timeout) }
  }

  /** $result */
  @throws(classOf[Exception])
  def result[T](awaitable: Awaitable[T]): T =
    result(awaitable, Duration.Top)

  /** $result */
  @throws(classOf[Exception])
  def result[T](awaitable: Awaitable[T], timeout: Duration): T =
    if (awaitable.isReady) awaitable.result(timeout)
    else Scheduler.blocking { awaitable.result(timeout) }

  /** $all */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def all(awaitables: Awaitable[_]*): Unit =
    all(awaitables, Duration.Top)

  /** $all */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def all(awaitables: Seq[Awaitable[_]], timeout: Duration): Unit =
    awaitables foreach { _.ready(timeout) }
}

/**
 * A mixin to make an [[com.twitter.util.Awaitable]] out
 * of a [[com.twitter.util.Closable]].
 *
 * Use `closeAwaitably` in the definition of `close`:
 *
 * {{{
 * class MyClosable extends Closable with CloseAwaitably {
 *   def close(deadline: Time) = closeAwaitably {
 *     // close the resource
 *   }
 * }
 * }}}
 */
trait CloseAwaitably extends Awaitable[Unit] {
  private[this] val onClose = new Promise[Unit]
  private[this] val closed = new AtomicBoolean(false)

  /**
   * closeAwaitably is intended to be used as a wrapper for
   * `close`. The underlying `f` will be called at most once.
   */
  protected def closeAwaitably(f: => Future[Unit]): Future[Unit] = {
    if (closed.compareAndSet(false, true))
      onClose.become(f)
    onClose
  }

  def ready(timeout: Duration)(implicit permit: Awaitable.CanAwait): this.type = {
    onClose.ready(timeout)
    this
  }

  def result(timeout: Duration)(implicit permit: Awaitable.CanAwait): Unit =
    onClose.result(timeout)
  
  def isReady(implicit permit: Awaitable.CanAwait): Boolean =
    onClose.isReady
}
