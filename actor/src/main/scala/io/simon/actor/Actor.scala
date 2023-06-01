package io.simon.actor

import zio.IO

trait Actor[+E, -Message[+_]] {

  def receive[A]: Actor.Receive[Message, E, A]

}

object Actor {

  final type Receive[-Message[_], +E, A] = PartialFunction[Message[A], IO[E, A]]

}

// todo - stateful actor, then caching actor
