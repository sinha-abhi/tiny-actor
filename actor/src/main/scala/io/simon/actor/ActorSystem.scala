package io.simon.actor

import io.simon.actor.ActorError.{IllegalArgument, InvalidState}
import io.simon.actor.ActorPath.SystemNamePattern
import io.simon.actor.ActorSystem.ActorEffect
import zio._

final class ActorSystem private[actor] (
  private val systemName: String,
  private val map: Ref[Map[String, Any]],
  private val isTerminated: Ref[Boolean]
) {

  def name: UIO[String] = ZIO.succeed(s"ActorSystem($systemName)")

  // todo - pass actor as a layer
  def actorOf[E: Tag, Message[+_]: TagK](
    actorName: String,
    actor: Actor[E, Message]
  ): ActorEffect[ActorRef[E, Message]] =
    for {
      actors <- map.get
      _      <- ZIO.fail(InvalidState(s"Actor name=$actorName already exists")).when(actors.contains(actorName))
      finalName = s"$systemName/$actorName"
      actorRef <- ActorRef.createActorThread(finalName).provide(ZLayer.succeed(actor))
      _        <- map.set(actors + (actorName -> actorRef))
    } yield actorRef

  // todo - retrieve actorref

  def terminate: ActorEffect[Seq[_]] =
    for {
      terminated <- isTerminated.get
      _          <- ZIO.fail(InvalidState(s"ActorSystem name=$systemName is already terminated")).when(terminated)
      actors     <- map.get
      pending    <- ZIO.foreach(actors.values.toSeq)(_.asInstanceOf[ActorRef[Any, Any]].stop)
      _          <- isTerminated.set(true)
    } yield pending.flatten

}

object ActorSystem {

  final type ActorEffect[A] = IO[ActorError, A]

  final def create(name: String = "default"): ActorEffect[ActorSystem] =
    for {
      _ <- ZIO.fail(IllegalArgument("Invalid ActorSystem name", name)).unless(name.matches(SystemNamePattern))
      initActorMap <- Ref.make(Map.empty[String, Any])
      isTerminated <- Ref.make(false)
    } yield new ActorSystem(name, initActorMap, isTerminated)

}
