package example

case class CopyTask(
  srcPath: String,
  destPath: String
) {
  var uptodate = 0
  var changed = 0
}

object Hello extends App {
  val srcRoot = "..\\silhmojs\\"
  val destRoot = "..\\chessapp\\"
  val scalaVersion = "2.11"
  val clientoptjs = "client-opt.js"
  val tasks = List(
    CopyTask(srcRoot + "server\\app\\", destRoot + "app\\"),
    CopyTask(srcRoot + "server\\public\\", destRoot + "public\\"),
    CopyTask(srcRoot + "server\\conf\\", destRoot + "conf\\"),
    CopyTask(srcRoot + "shared\\src\\main\\scala\\shared\\", destRoot + "app\\shared\\")

  )
  def WriteStringToFile(path: String, content: String) {
    org.apache.commons.io.FileUtils.writeStringToFile(
      new java.io.File(path),
      content,
      null.asInstanceOf[String]
    )
  }
  def ReadFileToString(path: String): String =
    {
      val f = new java.io.File(path)
      if (!f.exists()) return null
      org.apache.commons.io.FileUtils.readFileToString(
        f,
        null.asInstanceOf[String]
      )
    }
  def CollectFiles(path: String, dirnames: List[String] = List[String](), recursive: Boolean = false, setfiles: List[String] = List[String]()): List[String] =
    {
      val d = new java.io.File(path)

      if (!(d.exists && d.isDirectory)) return setfiles

      var allfiles = setfiles

      val all = d.listFiles
      val dirs = all.filter(_.isDirectory).filter(_.getName != "target")
      val files = all.filter(_.isFile)

      if (recursive) for (dir <- dirs) allfiles = CollectFiles(dir.getAbsolutePath, dirnames :+ dir.getName, recursive, allfiles)

      allfiles = allfiles ::: ((for (f <- files) yield (dirnames :+ f.getName).mkString(java.io.File.separator)).toList)

      allfiles
    }
  def fullPath(root: String, path: String) = root + path

  var copies = scala.collection.mutable.ArrayBuffer[String]()
  def collect(copy: Boolean = false) {
    for (task <- tasks) {
      if ((!copy) || (task.changed > 0)) {
        println("Task " + task)
        val srcs = CollectFiles(task.srcPath, recursive = true)
        for (src <- srcs) {
          val srcpath = task.srcPath + src
          val destpath = task.destPath + src
          val srcfile = new java.io.File(srcpath)
          val destfile = new java.io.File(destpath)
          val destexists = destfile.exists
          val srclastmod = srcfile.lastModified
          val destlastmod = destfile.lastModified
          val uptodate = destexists && (destlastmod >= srclastmod)
          val uptodatestr = if (uptodate) "up to date" else "changed"
          if (uptodate) task.uptodate += 1 else task.changed += 1
          if (copy) {
            if (!uptodate) {
              println(s"--> copy $destpath")
              val content = ReadFileToString(srcpath)
              //WriteStringToFile(destpath, content)
              println(s"    ${content.length} characters")
              copies += s"""copy "$srcpath" "$destpath""""
            }
          } else println(s"$uptodatestr: $destpath")
        }
      }
    }
  }

  def change_version {
    val chesshtmlpath = fullPath(destRoot, "app\\views\\chess.scala.html")
    val content = ReadFileToString(chesshtmlpath)
    val parts = content.split(s"$clientoptjs\\?v")
    val quote = """""""
    val parts2 = parts(1).split(quote)
    val version = parts2(0).toInt
    val newversion = version + 1
    val newcontent = parts(0) + s"$clientoptjs?v" + newversion + quote + parts2(1)
    println("new chess.scala.html version : " + newversion)
    WriteStringToFile("chess.scala.html", newcontent)
  }

  val commitname = args.head

  collect()

  change_version

  val del = "----------------------------------------"
  println(s"$del\nSummary\n$del")
  println("chess.scala.html :")
  println(ReadFileToString("chess.scala.html"))
  for (task <- tasks) {
    println(task)
    println(s"--> up to date: ${task.uptodate}, changed: ${task.changed}")
  }
  println(s"$del\nCopy\n$del")
  collect(copy = true)
  println(s"$del\nDone\n$del")

  val bat = s"""
    |copy "${fullPath(srcRoot, s"client\\target\\scala-$scalaVersion\\$clientoptjs")}" "${fullPath(destRoot, "public\\javascripts\\")}" /Y
    |copy "${fullPath(srcRoot, "client\\target\\scala-$scalaVersion\\client-jsdeps.min.js")}" "${fullPath(destRoot, "public\\javascripts\\")}" /Y
    |copy "chess.scala.html" "${fullPath(destRoot, "app\\views\\")}" /Y
    |${copies.mkString("\n")}
    |cd $srcRoot
    |call pre.bat
    |git add -A .
    |git commit -m "$commitname"
    |git push origin master
    |cd $destRoot
    |call pre.bat
    |git add -A .
    |git commit -m "$commitname"
    |git push origin master
  """.stripMargin

  println(bat)

  WriteStringToFile("c.bat", bat)
}

