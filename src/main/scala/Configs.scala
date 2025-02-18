package aes

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.tile._

class WithAESAccel
    extends Config((site, here, up) => { case BuildRoCC =>
      up(BuildRoCC) ++ Seq((p: Parameters) => {
        val aes = LazyModule.apply(new AESAccel(OpcodeSet.custom0)(p))
        aes
      })
    })
