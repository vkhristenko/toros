package ch.cern.toros.apps

// toros
import ch.cern.toros.io._

// java.io
import java.lang.IllegalArgumentException

object dump_file_content {
  def main(args: Array[String]): Unit = {
    if (args.size==0)
      throw new IllegalArgumentException(s"Illegal arguments provided: ${args}")

    // dump TFile contens
    println(s"dumping contents for file: ${args(0)}")
    lldump(args(0))
  }
}
