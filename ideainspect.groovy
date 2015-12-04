#!/usr/bin/env groovy

/*
 * Copyright 2015 Benjamin Schmid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
    https://github.com/bentolor/idea-cli-inspector

    Note to the reader:
     This is my very first Groovy script. Please be nice.
     Not very happy with the results for myself.
*/
import groovy.io.FileType
import org.apache.commons.cli.Option

println "= IntellIJ IDEA Code Analysis Wrapper - v1.0 - @bentolor"

// Defaults
def resultDir = "target/inspection-results"
def acceptedLeves = ["[WARNING]", "[ERROR]"]
def skipFiles = []
def ideaTimeout = 20 // broken - has no effect


//
// --- Command line option parsing
//
OptionAccessor opt = parseCli()

if (!opt) System.exit(1) // will print usage automatically
if (opt.help) { cliBuilder.usage(); System.exit(1); }

// Levels
if (opt.l) {
  acceptedLeves.clear()
  opt.ls.each { level -> acceptedLeves << "[" + level + "]" }
}
// Skip files
if (opt.s) {
  skipFiles.clear()
  opt.ss.each { skipFile -> skipFiles << skipFile.replace(".xml", "") }
}
// target directory
if (opt.t) resultDir = opt.t
// timeout
// if (opt.to) ideaTimeout = opt.to.toInteger();
// IDEA home
def scriptExtension = (System.properties['os.name'].toLowerCase().contains('windows')) ? ".bat" : ".sh"
def pathSep = File.separator
def ideaPath = new File(opt.i + pathSep + "bin" + pathSep + "idea" + scriptExtension)
assertPath(ideaPath, "IDEA Installation directory")
// Root Diretory
def rootDir = new File(opt.r)
def dotIdeaDir = new File(opt.r + pathSep + ".idea")
assertPath(dotIdeaDir, "IDEA project directory")
// Inspection Profile
def profilePath = new File(dotIdeaDir.path + pathSep + "inspectionProfiles" + pathSep + opt.p)
assertPath(profilePath, "IDEA inspection profile file")

// Prepare result directory
def resultPath = new File(resultDir);
if (!resultPath.isAbsolute()) resultPath = new File(rootDir, resultDir);
if (resultPath.exists() && !resultPath.deleteDir()) fail "Unable to remove result dir " + resultPath.absolutePath
if (!resultPath.mkdirs()) fail "Unable to create result dir " + resultPath.absolutePath


//
// --- Actually running IDEA
//

//  ~/projects/dashboard.git/. ~/projects/dashboard.git/.idea/inspectionProfiles/bens_idea15_2015_11.xml /tmp/ -d server
def ideaArgs = [ideaPath.path, "inspect", rootDir.absolutePath, profilePath.absolutePath, resultPath.absolutePath]
if (opt.d) ideaArgs << "-d" << opt.d

println "#"
println "# Running IDEA IntelliJ Inspection"
println "#"
println "Executing: " + ideaArgs.join(" ")

def processBuilder=new ProcessBuilder(ideaArgs)
processBuilder.redirectErrorStream(true)
processBuilder.directory(rootDir)  // <--
def ideaProcess = processBuilder.start()
ideaProcess.consumeProcessOutput(System.out, System.err)
ideaProcess.waitForOrKill(1000 * 60 * ideaTimeout)
def exitValue = ideaProcess.exitValue()
if (exitValue != 0) fail("IDEA Process returned with an unexpected return code of $exitValue")


//
// --- Now lets look on the results
//
analyzeResult(resultPath, acceptedLeves, skipFiles)


//
//  --- Helper functions
//

void assertPath(File profilePath, String description) {
  if (!profilePath.exists()) {
    println description + " " + profilePath.path + " not found!"
    System.exit(1)
  }
}


void fail(String message) {
  println "FATAL ERROR: " + message
  println "             Aborting."
  System.exit(1)
}


private OptionAccessor parseCli() {
  def cliBuilder = new CliBuilder(usage: 'groovy ideainspect.groovy -i <IDEA_HOME> -p <PROFILEXML> -r <PROJDIR>')
  cliBuilder.with {
    h argName: 'help', longOpt: 'help', 'Show usage information and quit'
    l argName: 'level', longOpt: 'levels', args: Option.UNLIMITED_VALUES, valueSeparator: ',',
      'Levels to look for. Default: WARNING,ERROR'
    s argName: 'file', longOpt: 'skip', args: Option.UNLIMITED_VALUES, valueSeparator: ',',
      'Analysis result files to skip. For example "TodoComment" or "TodoComment.xml". \nDefault: <empty>'
    t argName: 'dir', longOpt: 'resultdir', args: 1,
      'Target directory to place the IDEA inspection XML result files. Default: target/inspection-results'
    i argName: 'dir', longOpt: 'ideahome', args: 1, required: true,
      'IDEA installation home directory. Required'
    d argName: 'dir', longOpt: 'dir', args: 1, 'Limit IDEA inspection to this directory'
    p argName: 'file', longOpt: 'profile', args: 1, required: true,
      'Use this inspection profile file located ".idea/inspectionProfiles". \nExample: "myprofile.xml"'
    r argName: 'dir', longOpt: 'rootdir', args: 1, required: true,
      'IDEA project root directory containing the ".idea" directory'
    //to argName: 'minutes', longOpt: 'timeout', args: 1,
    //  'Timeout in Minutes to wait for IDEA to complete the inspection. Default:'
  }

  def opt = cliBuilder.parse(args)

  if (!opt) System.exit(1); // will print usage automatically
  if (opt.help) {
      println ""
      println "This tools runs IntelliJ IDEA inspection via command line and"
      println "tries to parse the output. \n"
      println "Example usage:"
      println "  ./ideainspect.groovy -i ~/devel/idea -r . -p myinspections.xml \\"
      println "     -d src/main/java -s unused,Annotator,TodoComment.xml -l ERROR"
      println " "
      cliBuilder.usage();
      System.exit(1);
  }
  opt
}


private analyzeResult(File resultPath, List<String> acceptedLeves, List skipFiles) {
  println " "
  println "#"
  println "# Inspecting produced result files in $resultPath"
  println "#"
  println "# Looking for: $acceptedLeves"
  println "# Ignoring   : $skipFiles"

  def allGood = true;

  resultPath.eachFile(FileType.FILES) { file ->
    // Workaround for wrong XML formatting.
    // They clutter "</problems>" all-over
    //    See Bug: https://youtrack.jetbrains.com/issue/IDEA-148855
    String fileContents = file.getText('UTF-8')
    fileContents = fileContents.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "")
    fileContents = fileContents.replace("file://\$PROJECT_DIR\$/", "")
    fileContents = "<problems>" + fileContents.replaceAll("<.?problems.*>", "") + "</problems>"

    def xmlDocument = new XmlParser().parseText(fileContents)
    def fileIssues = []
    def xmlFileName = file.name

    if (skipFiles.contains(xmlFileName.replace(".xml", ""))) {
      println "--- Skipping $xmlFileName"
      return
    }

    xmlDocument.problem.each { problem ->
      String severity = problem.problem_class.@severity
      if (acceptedLeves.contains(severity)) {
        String affectedFile = problem.file.text()
        String problemDesc = problem.description.text()
        String line = problem.line.text()
        fileIssues << "$severity $affectedFile:$line -- $problemDesc";
      }
    }

    if (!fileIssues.empty) {
      allGood = false;
      println("--- $xmlFileName")
      println(fileIssues.join("\n"))
      println("")
    }
  }

  println " "
  println "#"
  println "# Analysis Result"
  println "#"

  if (allGood) {
    println "Looks great - everything seems to be ok!"
    System.exit(0)
  } else {
    println "Entries found. Returncode: 1"
    System.exit(1)
  }
}
