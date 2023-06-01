package io.simon.actor

import zio.ZIO
import zio.test.{ZIOSpecDefault, assertTrue}

object ActorSpec extends ZIOSpecDefault {

  def spec =
    suite("ActorSystem")(
      suite("process messages")(
        test("serial") {
          import CounterActor._

          for {
            system <- ActorSystem.create("test")
            actor  <- system.actorOf("counter", counterActor)
            _      <- actor.start
            _      <- actor ! Increment
            _      <- actor ! Increment
            _      <- actor ! Increment
            c1     <- actor ? Get
            _      <- system.terminate
          } yield assertTrue(c1 == 3)
        },
        test("parallel") {
          import CounterActor._

          for {
            system <- ActorSystem.create("test")
            actor  <- system.actorOf("counter", counterActor)
            _      <- actor.start
            _      <- (actor ! Increment) <&> (actor ! Increment) <&> (actor ! Increment)
            c1     <- actor ? Get
            _      <- system.terminate
          } yield assertTrue(c1 == 3)
        }
      )
    )

}

object CounterActor {

  sealed trait CounterMessage[+A]
  final case object Reset                  extends CounterMessage[Unit]
  final case object Increment              extends CounterMessage[Unit]
  final case object Get                    extends CounterMessage[Int]
  final case class IncrementBy(count: Int) extends CounterMessage[Unit]

  final def counterActor = new Actor[Throwable, CounterMessage] {

    private var currentCount: Int = 0

    def receive[A] = {
      case Reset =>
        ZIO.attempt { currentCount = 0 }
      case Increment =>
        ZIO.attempt(currentCount += 1)
      case Get =>
        ZIO.succeed(currentCount)
      case IncrementBy(count) =>
        ZIO.attempt(currentCount += count)
    }

  }

}
