/**
 * Copied from http://codereview.stackexchange.com/questions/79267/scala-trywith-that-closes-resources-automatically
 * with adaption for throwing exceptions as required for running tests
 */

package uk.ac.cdrc.mintsearch.neo4j

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object WithResource {
  def apply[C <: AutoCloseable, R](resource: => C)(f: C => R): R =
    Try(resource).flatMap(resourceInstance => {
      try {
        val returnValue = f(resourceInstance)
        Try(resourceInstance.close()).map(_ => returnValue)
      } catch {
        case NonFatal(exceptionInFunction) =>
          try {
            resourceInstance.close()
            Failure(exceptionInFunction)
          } catch {
            case NonFatal(exceptionInClose) =>
              exceptionInFunction.addSuppressed(exceptionInClose)
              Failure(exceptionInFunction)
          }
      }
    }) match {
      case Failure(ex) => throw ex
      case Success(x) => x
    }
}