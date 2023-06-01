package io.simon.actor

sealed trait ActorError

object ActorError {

  final case class IllegalArgument[A](reason: String, arg: A) extends ActorError

  final case class InvalidState(reason: String) extends ActorError

}
