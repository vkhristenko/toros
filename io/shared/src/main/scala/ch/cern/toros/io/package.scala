package ch.cern.toros

// java.nio imports
import java.nio.ByteBuffer
import java.nio.file.FileSystems
import java.nio.channels.FileChannel

package object io {

  abstract class PObject;
  case class PDatime(raw: Int) extends PObject;
  object PDatime {
    def build(buffer: ByteBuffer): PDatime = {
      new PDatime(buffer.getInt)
    }
  }
  case class PKey(
    totalBytes: Int,
    version: Int,
    objBytes: Int,
    datime: PDatime,
    keyBytes: Short,
    cycle: Short,
    seekKey: Long,
    seekPDir: Long,
    className: String,
    objName: String,
    objTitle: String
  ) extends PObject;
  object PKey {
    def build(buffer: ByteBuffer): PKey = {
      val nbytes = buffer.getInt
      val version = buffer.getVersion
      val objBytes = buffer.getInt
      val dt = PDatime.build(buffer)
      val keyBytes = buffer.getShort
      val cycle = buffer.getShort
      val (seekKey, seekPDir) = 
        if (version > 1000) (buffer.getLong, buffer.getLong)
        else (buffer.getInt.toLong, buffer.getInt.toLong)
      val className = buffer.getString
      val objName = buffer.getString
      val objTitle = buffer.getString
      new PKey(nbytes, version, objBytes,
               dt, keyBytes, cycle,
               seekKey, seekPDir, 
               className, objName,objTitle)
    }
  }

  def open(path: String, bsize: Int = 1000): tmp.File = tmp.File(path)

  // for simple cli debugging
  def setup(path: String, bsize: Int = 1000): (ByteBuffer, FileChannel) = {
    val p = FileSystems.getDefault.getPath(path)
    val ch = FileChannel.open(p)
    val buffer = ByteBuffer.allocateDirect(bsize)
    ch.read(buffer)
    buffer.rewind.limit(if (bsize.toLong > ch.size) ch.size.toInt else bsize)
    (buffer, ch)
  }

  // enhance all the buffers
  implicit class ByteBufferWrapper(buffer: ByteBuffer) {
    def slice(start: Int, end: Int): ByteBuffer = 
      buffer.slice.position(start).limit(end).asInstanceOf[ByteBuffer]

    def slice(from: Int): ByteBuffer = 
      buffer.slice.position(from).asInstanceOf[ByteBuffer]

    def toRawString(ngroups: Int = 25): String = {
      def dump(bytes: Array[Byte]): String = {
        val header = (for (i <- 0 until ngroups) yield ("%02d" format i)).mkString(" ");
        val dump = bytes.map({
          x: Byte => "%02X" format x
        }).grouped(ngroups).map(_.mkString(" ")).mkString("\n")
        header + "\n" + "="*(ngroups*2 + ngroups/2) + "\n" + dump;
      }
      
      val out = if (buffer.hasArray) 
        dump(buffer.array)
      else {
        val array = Array.fill[Byte](buffer.remaining)(0)
        buffer.get(array)
        dump(array)
      }
      buffer.rewind
      out
    }

    def getVersion: Int = {
      val v = buffer.getShort
      if ((v & 0x4000) == 0) 
        v.toInt
      else {
        val nbytes: Int = ((v & 0x4fff) << 16) + buffer.getShort
        nbytes
      }
    }

    def getString: String = {
      val size = buffer.get
      if (size == 0) return ""
      val newsize = 
        if (size == -1) 
          buffer.getInt
        else if (size < 0) 
          256 + size
        else 
          size  
      
      (for (i <- 0 until newsize) yield buffer.get.toChar).mkString
    }

    def putString(str: String): ByteBuffer = {
      buffer.put(str.length.toByte).put(str.toArray.map(_.toByte))
    }
  }
}
