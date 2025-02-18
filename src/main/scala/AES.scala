package aes

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config._
import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem._

// Blackbox (Class name must match top-level verilog file)
class aes(implicit p: Parameters) extends BlackBox with HasBlackBoxResource {
  val io = IO(new AESCoreIO)

  addResource("/vsrc/aes.v")
}

class AESAccel(opcodes: OpcodeSet)(implicit p: Parameters)
    extends LazyRoCC(opcodes = opcodes, nPTWPorts = 0) {
  val dma = LazyModule(new DMA(p(SystemBusKey).beatBytes, 32, "AESAccelDMA"))

  override lazy val module = new AESAccelImp(this)
  override val tlNode = dma.id_node
}

class AESAccelImp(outer: AESAccel)(implicit p: Parameters)
    extends LazyRoCCModuleImp(outer)
    with HasCoreParameters {
  val beatBytes = p(SystemBusKey).beatBytes

  // RoCC Decoupler
  val dcplr = Module(new RoCCDecoupler)
  dcplr.io.reset := reset.asBool()
  dcplr.io.rocc_cmd <> io.cmd
  io.resp <> dcplr.io.rocc_resp
  io.busy := dcplr.io.rocc_busy
  io.interrupt := dcplr.io.rocc_intr
  dcplr.io.rocc_excp := io.exception

  // Controller
  val ctrl = Module(new AESController(32, beatBytes))
  ctrl.io.reset := reset.asBool()
  ctrl.io.dcplrIO <> dcplr.io.ctrlIO

  // DMA Connections
  outer.dma.module.io.read.req <> ctrl.io.dmem.readReq
  outer.dma.module.io.write.req <> ctrl.io.dmem.writeReq
  ctrl.io.dmem.readResp <> outer.dma.module.io.read.resp
  ctrl.io.dmem.readRespQueue <> outer.dma.module.io.read.queue
  ctrl.io.dmem.busy := outer.dma.module.io.readBusy | outer.dma.module.io.writeBusy

  // AES Core (Black Box)
  val aesbb = Module(new aes)
  aesbb.io <> ctrl.io.aesCoreIO
}
