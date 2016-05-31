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
import java.nio.file.Paths

println "= IntellIJ IDEA Code Analysis Wrapper - v1.5 - @bentolor"

// Defaults
def resultDir = "target/inspection-results"
def acceptedLeves = ["[WARNING]", "[ERROR]"]
def ideaTimeout = 20 // broken - has no effect
verbose = false

//
// --- Command line option parsing
//
def configOpts = parseConfigFile()
def OptionAccessor cliOpts = parseCli((configOpts << args).flatten())

// Levels
if (cliOpts.l) {
  acceptedLeves.clear()
  cliOpts.ls.each { level -> acceptedLeves << "[" + level + "]" }
}
// target directory
if (cliOpts.t) {
  resultDir = cliOpts.t
}
// timeout
// if (cliOpts.to) ideaTimeout = cliOpts.to.toInteger();
// IDEA home
File ideaPath = findIdeaExecutable(cliOpts)
// Passed project root Directory or working directory
def rootDir =  cliOpts.r ? new File(cliOpts.r) : Paths.get(".").toAbsolutePath().normalize().toFile()
def dotIdeaDir = new File(rootDir, ".idea")
assertPath(dotIdeaDir, "IDEA project directory", "Please set the `rootdir` property to the location of your `.idea` project")
// Inspection Profile
def profileName = cliOpts.p ?: "Project_Default.xml"
def profilePath = new File(dotIdeaDir.path + File.separator + "inspectionProfiles" + File.separator + profileName)
assertPath(profilePath, "IDEA inspection profile file")

// Prepare result directory
def resultPath = new File(resultDir);
if (!resultPath.absolute) {
  resultPath = new File(rootDir, resultDir)
};
if (resultPath.exists() && !resultPath.deleteDir()) {
  fail "Unable to remove result dir " + resultPath.absolutePath
}
if (!resultPath.mkdirs()) {
  fail "Unable to create result dir " + resultPath.absolutePath
}

//
// --- Running VMOptions Modifier
//
if(cliOpts.sc){
  modifyIdeaProps(cliOpts)
}

//
// --- Actually running IDEA
//

//  ~/projects/dashboard.git/. ~/projects/dashboard.git/.idea/inspectionProfiles/bens_idea15_2015_11.xml /tmp/ -d server
def ideaArgs = [ideaPath.path, "inspect", rootDir.absolutePath, profilePath.absolutePath, resultPath.absolutePath, "-v1"]
if (cliOpts.d) {
  ideaArgs << "-d" << rootDir.absolutePath + "/" + cliOpts.d
}

println "#"
println "# Running IDEA IntelliJ Inspection"
println "#"
println "Executing: " + ideaArgs.join(" ")

def processBuilder = new ProcessBuilder(ideaArgs)
def env = processBuilder.environment();
env.put("idea.analyze.scope", "BuilderTREND")
processBuilder.redirectErrorStream(true)
processBuilder.directory(rootDir)  // <--
def ideaProcess = processBuilder.start()
ideaProcess.consumeProcessOutput(System.out, System.err)
ideaProcess.waitForOrKill(1000 * 60 * ideaTimeout)
def exitValue = ideaProcess.exitValue()
if (exitValue != 0) {
  fail("IDEA Process returned with an unexpected return code of $exitValue")
}

//
// --- Now lets look on the results
//
analyzeResult(resultPath, acceptedLeves)

//
//  --- Helper functions
//

void assertPath(File profilePath, String description, String hint = null) {
  if (!profilePath.exists()) {
    println description + " " + profilePath.path + " not found!"
    if (hint) {
      println hint
    }
    System.exit(1)
  }
}


void fail(String message) {
  println "FATAL ERROR: " + message
  println "             Aborting."
  System.exit(1)
}

private parseConfigFile() {
  // Parse root dir with minimal CliBuilder
  def cliBuilder = new CliBuilder()
  cliBuilder.with {
    r argName: 'dir', longOpt: 'rootdir', args: 1, required: false,
      'IDEA project root directory containing the ".idea" directory'
    v argName: 'verbose', longOpt: 'verbose', args: 0, required: false,
      'Enable verbose logging & debugging'
  }

  def opt = cliBuilder.parse(args)
  verbose = verbose ?: (opt && opt.v)
  def rootDir = opt != null && opt.r ? opt.r : '.'

  def configFile = new File(rootDir + '/.ideainspect')
  def configArgs = []
  if (configFile.exists()) {
    if (verbose) {
      println "Parsing " + configFile.absolutePath
    }
    configFile.eachLine { line ->
      def values = line.split(':')
      if (!line.startsWith('#') && values.length == 2) {
        configArgs.push('--' + values[0].trim())
        configArgs.push(values[1].trim())
      }
    }
  }
  if (verbose) {
    println configArgs
  }
  return configArgs
}

private OptionAccessor parseCli(configArgs) {
  def cliBuilder = new CliBuilder(usage: 'groovy ideainspect.groovy\n      -i <IDEA_HOME> -p <PROFILEXML> -r <PROJDIR>',
                                  stopAtNonOption: false)
  cliBuilder.with {
    h argName: 'help', longOpt: 'help', 'Show usage information and quit'
    l argName: 'level', longOpt: 'levels', args: Option.UNLIMITED_VALUES, valueSeparator: ',',
      'Levels to look for. Default: WARNING,ERROR'
    sc argName: 'string', longOpt: 'scope', args: 1,
      'The name of the scope to be processed'
    t argName: 'dir', longOpt: 'resultdir', args: 1,
      'Target directory to place the IDEA inspection XML result files. Default: target/inspection-results'
    i argName: 'dir', longOpt: 'ideahome', args: 1,
      'IDEA or Android Studio installation home directory. Default: IDEA_HOME environment variable or "idea"'
    d argName: 'dir', longOpt: 'dir', args: 1, 'Limit IDEA inspection to this directory'
    li argName: 'dir', longOpt: 'lib', args: 1, 'The directory contains the *.vmoptions file'
    p argName: 'file', longOpt: 'profile', args: 1,
      'Use this inspection profile file located ".idea/inspectionProfiles". \nExample: "myprofile.xml". Default: "Project_Default.xml"'
    r argName: 'dir', longOpt: 'rootdir', args: 1,
      'IDEA project root directory containing the ".idea" directory. Default: Working directory'
    v argName: 'verbose', longOpt: 'verbose', args: 0,
      'Enable verbose logging & debugging'
    //to argName: 'minutes', longOpt: 'timeout', args: 1,
    //  'Timeout in Minutes to wait for IDEA to complete the inspection. Default:'
  }

  def opt = cliBuilder.parse(configArgs)

  if (!opt) {
    System.exit(1)
  }; // will print usage automatically
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

private File findIdeaExecutable(OptionAccessor cliOpts) {
  def platform = System.properties['os.name'], scriptPath
  def ideaHome = cliOpts.i ?: (System.getenv("IDEA_HOME") ?: "idea")
  def executable = "idea"
  if(ideaHome.toLowerCase().contains("android")){
  	executable = "studio"
  }

  switch (platform) {
    case ~/^Windows.*/:
      scriptPath =  "bin" + File.separator + executable + ".bat"
      break;
    case "Mac OS X":
      scriptPath = "Contents/MacOS/" + executable
      break;
    default:
      scriptPath = "bin/" + executable + ".sh"
      break;
  }

  def ideaExecutable = new File(ideaHome + File.separator + scriptPath)
  assertPath(ideaExecutable, "IDEA Installation directory",
             "Use a IDEA_HOME environment variable or the `ideahome` property in `.ideainspect` \n" +
                     "or the `-i` command line option to point me to a valid IntelliJ installation")
  ideaExecutable
}

private modifyIdeaProps(OptionAccessor cliOpts){
  println " "
  println "#"
  println "# Modifying idea.properties for scope: $cliOpts.sc"

  def ideaHome = cliOpts.i ?: (System.getenv("IDEA_HOME") ?: "idea")
  def libraryHome = cliOpts.li.replace("~", System.getProperty("user.home"))
  def ideaPropsFile = new File(libraryHome + "/idea.properties")
  def outputLines = new ArrayList<String>();
  assertPath(ideaPropsFile, "Idea Properties File",
              "Use the `lib` proeprty in `.ideainspect` or the `-li` command line option to point\n" +
                      "to a valid preferences folder.")
  def lines = ideaPropsFile.text.split(System.getProperty("line.separator"))
  for(line in lines){
    if(!line.contains("idea.analyze.scope")){
      outputLines.add(line);
    }
  }
  outputLines.add("idea.analyze.scope=" + cliOpts.sc)
  ideaPropsFile.newWriter().withWriter { w ->
    outputLines.each { l ->
      w << l << System.getProperty("line.separator")
    }
  }
}

private analyzeResult(File resultPath, List<String> acceptedLeves) {
  println " "
  println "#"
  println "# Inspecting produced result files in $resultPath"
  println "#"
  println "# Looking for levels    : $acceptedLeves"

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

    xmlDocument.problem.each { problem ->
      String severity = problem.problem_class.@severity
      String affectedFile = problem.file.text()
      if (acceptedLeves.contains(severity)) {
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
