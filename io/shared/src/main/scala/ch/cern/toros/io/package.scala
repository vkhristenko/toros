package ch.cern.toros

// java.nio imports
import java.nio.ByteBuffer
import java.nio.file.FileSystems
import java.nio.channels.FileChannel

package object io {

  // for simple cli debugging
  def setup(path: String, bsize: Int = 1000): (ByteBuffer, FileChannel) = {
    val p = FileSystems.getDefault.getPath(path)
    val ch = FileChannel.open(p)
    val buffer = ByteBuffer.allocateDirect(bsize)
    ch.read(buffer)
    buffer.rewind.limit(if (bsize > ch.size.toInt) ch.size.toInt else bsize)
    (buffer, ch)
  }

  // enhance all the buffers
  implicit class ByteBufferWrapper(buffer: ByteBuffer) {
    def slice(start: Int, end: Int): ByteBuffer = 
      buffer.slice.position(start).limit(end).asInstanceOf[ByteBuffer]

    def toRawString: String = {
      def dump(bytes: Array[Byte], ngroups: Int = 25): String = {
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
