package org.perftester.renderer

import org.perftester.ProfileMain.printAggResults
import org.perftester.{EnvironmentConfig, TestConfig}
import org.perftester.results.{PhaseResults, RunResult}
import org.perftester.results.rows.MainPhaseRow

import scala.collection.mutable

object TextRenderer {
	case class Result(id: String, rawData: Seq[PhaseResults], phases: Set[String])

	def outputTextResults(envConfig: EnvironmentConfig,
	                      results: Seq[(TestConfig, RunResult)]): Unit =
		outputTextResults(envConfig.iterations, results.map {
			case (config, run) => Result(config.id, run.rawData, run.phases)
		})

  def outputTextResults(iterations: Int,
                        results: Seq[Result]): Unit = {
    def heading(title: String) {
      println(
        f"-----\n$title\n${"Run Name"}%25s\t${"Wall time (ms)"}%25s\t${"All Wall time (ms)"}%25s\t${"CPU(ms)"}%25s\t${"Idle time (ms)"}%25s\t${"Allocated(MBs)"}%25s")
    }
    def allPhases(raw: Seq[PhaseResults]): Seq[PhaseResults] = {
      val res = raw.groupBy(_.iterationId) map {
        case (iterationNo, iterationData) =>
          val first   = iterationData.head
          val startNs = iterationData.sortBy(_.main.startNs).head.main.startNs
          val endNs   = iterationData.sortBy(-_.main.endNs).head.main.endNs

          val mainPhaseRow: MainPhaseRow = MainPhaseRow(
            startNs = startNs,
            endNs = endNs,
            runId = first.main.runId,
            phaseId = -1,
            phaseName = "all",
            purpose = "collated",
            taskCount = -1,
            threadId = -1,
            threadName = "N/A",
            runNs = endNs - startNs,
            idleNs = iterationData.map(_.main.idleNs).sum,
            cpuTimeNs = iterationData.map(_.main.cpuTimeNs).sum,
            userTimeNs = iterationData.map(_.main.userTimeNs).sum,
            allocatedBytes = iterationData.map(_.main.allocatedBytes).sum,
            heapSize = iterationData.map(_.main.heapSize).max
          )
          PhaseResults(
            main = mainPhaseRow,
            background = iterationData.flatMap(_.background)(scala.collection.breakOut),
            gc = iterationData.flatMap(_.gc)(scala.collection.breakOut)
          )

      }
      res.toSeq
    }

    heading("ALL")
    results.foreach(r => printAggResults(r, 1.0))
    val phases: mutable.LinkedHashSet[String] =
      results.flatMap(_.phases)(scala.collection.breakOut)

    if (iterations > 10) {
      (10 until (iterations, 10)) foreach { i =>
        println(
          "\n---------------------------------------------------------------------------------------------------")
        println(
          "---------------------------------------------------------------------------------------------------")
        heading(s"after $i 90%")
        results.foreach { result =>
            val skipped = result.rawData.dropWhile(_.iterationId <= i)
            printAggResults(result, 0.9)
        }

        for (phase <- phases) {
          heading(s"after $i 90%, phase $phase")
          for { result <- results } {
            val skipped = result.rawData.filter {
              case row => row.iterationId > i && row.phaseName == phase
            }

            printAggResults(result, 0.9)
          }
        }
        for (phase <- phases) {
          heading(s"after $i 90%, phase $phase no GC")
          for { result <- results } {
            val skipped = result.rawData.filter {
              case row => row.iterationId > i && row.phaseName == phase && row.gcTimeMS == 0
            }

            printAggResults(result, 0.9)
          }
        }
      }
    }
  }
}
