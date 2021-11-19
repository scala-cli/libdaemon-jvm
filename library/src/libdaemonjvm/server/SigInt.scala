package libdaemonjvm.server

import sun.misc.{Signal, SignalHandler}

object SigInt {
  def ignoreSigInt(onSigInt: Signal => Unit): Unit = {
    val handler: SignalHandler = sig => onSigInt(sig)
    Signal.handle(new Signal("INT"), handler)
  }
}
