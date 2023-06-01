package io.simon.actor

import zio._

sealed trait ActorRef[+E, -Message[+_]] extends Serializable {

  def name: UIO[String]

  def start[E1 >: E]: IO[E1, Unit]

  def stop[E1 >: E]: UIO[Chunk[_]]

  def ask[E1 >: E, A](m: Message[A]): IO[E1, A]

  def tell[E1 >: E](m: Message[_]): IO[E1, Unit]

  final def ?[E1 >: E, A](m: Message[A]): IO[E1, A] = ask(m)

  final def ![E1 >: E](m: Message[_]): IO[E1, Unit] = tell(m)

  private[actor] def isTerminated: Ref[Boolean]

}

object ActorRef {

  // todo - should we add scope around this? should return a layer?
  final def createActorThread[E: Tag, Message[+_]: TagK](
    actorName: String,
    queueBound: Int = 1000
  ): ZIO[Actor[E, Message], Nothing, ActorRef[E, Message]] =
    for {
      actor        <- ZIO.service[Actor[E, Message]]
      queue        <- Queue.bounded[WrappedActorMessage[Message, E, _]](queueBound)
      isTerminated <- Ref.make(true)
    } yield new ActorThread[E, Message](actorName, isTerminated, actor, queue)

  private[actor] final class ActorThread[+E, -Message[+_]](
    private[actor] val actorName: String,
    private[actor] val isTerminated: Ref[Boolean],
    actor: Actor[E, Message],
    queue: Queue[WrappedActorMessage[Message, E, _]]
  ) extends ActorRef[E, Message] {

    def name: UIO[String] = ZIO.succeed(s"ActorThread($actorName)")

    def start[E1 >: E]: IO[E1, Unit] = {
      def handle[A](msg: WrappedActorMessage[Message, E, A]) = {
        val (m, promise) = msg
        actor.receive(m).flatMap(a => promise.succeed(a))
      }

      for {
        _ <- ZIO.log(s"starting actor $actorName") // fixme
        terminated <- isTerminated.get
        _ <- ZIO.when(terminated) {
          queue.take.flatMap(message => handle(message).unit).forever.fork *> isTerminated.set(false)
        }
      } yield ()
    }

    def stop[E1 >: E]: UIO[Chunk[_]] =
      for {
        _ <- ZIO.log(s"terminating actor $actorName") // fixme
        terminated <- isTerminated.get
        unprocessed <-
          if (!terminated)
            for {
              c <- queue.takeAll
              _ <- queue.shutdown
              _ <- isTerminated.set(true)
            } yield c
          else
            ZIO.succeed(Chunk.empty)
      } yield unprocessed

    def ask[E1 >: E, A](m: Message[A]): IO[E1, A] =
      for {
        _ <- ZIO.log(s"queueing $m and waiting") // fixme
        p   <- Promise.make[E, A]
        _   <- queue.offer((m, p))
        ret <- p.await
      } yield ret

    def tell[E1 >: E](m: Message[_]): IO[E1, Unit] =
      for {
        _ <- ZIO.log(s"queueing $m") // fixme
        p <- Promise.make[E, Any]
        _ <- queue.offer((m, p))
      } yield ()

  }

  private[actor] type WrappedActorMessage[Message[_], E, A] = (Message[A], Promise[E, A])

}
