package zhoushan

import chisel3._
import chisel3.util._
import chisel3.util.experimental._

class Prf extends Module with ZhoushanConfig {
  val io = IO(new Bundle {
    val in = Vec(IssueWidth, Input(new MicroOp))
    val out = Vec(IssueWidth, Output(new MicroOp))
    val rs1_data = Vec(IssueWidth, Output(UInt(64.W)))
    val rs2_data = Vec(IssueWidth, Output(UInt(64.W)))
    val rd_en = Vec(IssueWidth, Input(Bool()))
    val rd_paddr = Vec(IssueWidth, Input(UInt(6.W)))
    val rd_data = Vec(IssueWidth, Input(UInt(64.W)))
    val flush = Input(Bool())
  })

  val prf = RegInit(VecInit(Seq.fill(64)(0.U(64.W))))

  for (i <- 0 until IssueWidth) {
    when (io.rd_en(i) && (io.rd_paddr(i) =/= 0.U)) {
      prf(io.rd_paddr(i)) := io.rd_data(i);
    }
  }

  val rs1_paddr = io.in.map(_.rs1_paddr)
  val rs2_paddr = io.in.map(_.rs2_paddr)
  val rs1_data = Wire(Vec(IssueWidth, UInt(64.W)))
  val rs2_data = Wire(Vec(IssueWidth, UInt(64.W)))

  for (i <- 0 until IssueWidth) {
    rs1_data(i) := Mux((rs1_paddr(i) =/= 0.U), prf(rs1_paddr(i)), 0.U)
    rs2_data(i) := Mux((rs2_paddr(i) =/= 0.U), prf(rs2_paddr(i)), 0.U)
  }

  // pipeline registers

  val out_uop = RegInit(VecInit(Seq.fill(IssueWidth)(0.U.asTypeOf(new MicroOp))))
  val out_rs1_data = RegInit(VecInit(Seq.fill(IssueWidth)(0.U(64.W))))
  val out_rs2_data = RegInit(VecInit(Seq.fill(IssueWidth)(0.U(64.W))))
  val out_valid = RegInit(false.B)

  when (io.flush) {
    for (i <- 0 until IssueWidth) {
      out_uop(i) := 0.U.asTypeOf(new MicroOp)
      out_rs1_data(i) := 0.U
      out_rs2_data(i) := 0.U
    }
  } .otherwise {
    for (i <- 0 until IssueWidth) {
      out_uop(i) := Mux(io.in(i).valid, io.in(i), 0.U.asTypeOf(new MicroOp))
      out_rs1_data(i) := rs1_data(i)
      out_rs2_data(i) := rs2_data(i)
    }
  }
  
  io.out := out_uop
  io.rs1_data := out_rs1_data
  io.rs2_data := out_rs2_data

  BoringUtils.addSource(prf, "prf")

}
