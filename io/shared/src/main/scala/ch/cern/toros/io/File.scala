package ch.cern.toros.io

class File(path: String) {

}

object File {
  def open(path: String) = new File(path)
  def create(path: String) = new File(path)
  def apply(path: String) = new File(path)
}
