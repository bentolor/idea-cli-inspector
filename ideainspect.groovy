#!/usr/bin/env groovy
import groovy.io.FileType
import groovy.transform.Field
import org.apache.commons.cli.Option

import java.nio.file.Files
import java.nio.file.Paths

/*
 * Copyright 2015-2016 Benjamin Schmid, @bentolor
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
*/
println "= IntellIJ IDEA Code Analysis Wrapper - v1.6alpha - @bentolor"

// Defaults
def resultDir = "target/inspection-results"
def acceptedLevels = ["[WARNING]", "[ERROR]"]
def skipResults = []
def skipIssueFilesRegex = []
// Process timeout:
//    This is more or less broken, because after reaching the timeout value
//    it will kill only the Wrapper script and the IDEA process will happily
//    continue to run.
//    We should never reach this value and IDEA should always termiante on
//    its own
def ideaWrapperTimeout = 1200  // Minutes
@Field Boolean verbose = false

//
// --- Command line option parsing
//
def configOpts = args.toList()
configOpts.addAll(parseConfigFile())
def OptionAccessor cliOpts = parseCli(configOpts)

// Levels
if (cliOpts.l) {
  acceptedLevels.clear()
  cliOpts.ls.each { level -> acceptedLevels << "[" + level + "]" }
}
// Skip result XML files
if (cliOpts.s) cliOpts.ss.each { skipFile -> skipResults << skipFile.replace(".xml", "") }
// Skip issues affecting given file name regex
if (cliOpts.sf) cliOpts.sfs.each { skipRegex -> skipIssueFilesRegex << skipRegex }
// target directory
if (cliOpts.t) resultDir = cliOpts.t

// IDEA home
File ideaPath = findIdeaExecutable(cliOpts)
// Passed project root Directory or working directory
def rootDir = cliOpts.r ? new File(cliOpts.r) : Paths.get(".").toAbsolutePath().normalize().toFile()
def rootfilePath = cliOpts.rf ? new File(cliOpts.rf) : rootDir

def dotIdeaDir = new File(rootDir, ".idea")
assertPathIsDir(dotIdeaDir, "IDEA project directory", "Please set the `rootdir` property to the location of your `.idea` project")
// Inspection Profile
def profilePath = cliOpts.p ? new File(cliOpts.p) : null
if( profilePath == null || !profilePath.isAbsolute() ) {
  // if the given arg is not a full path, then try with a path relative to the .idea dir
  profileName = cliOpts.p ?: "Project_Default.xml"
  profilePath = new File(dotIdeaDir.path + File.separator + "inspectionProfiles" + File.separator + profileName)
}
assertPathIsFile(profilePath, "IDEA inspection profile file")

// Prepare result directory
def resultPath = new File(resultDir);
if (!resultPath.absolute) resultPath = new File(rootDir, resultDir)
if (resultPath.exists() && !resultPath.deleteDir()) fail "Unable to remove result dir " + resultPath.absolutePath
if (!resultPath.mkdirs()) fail "Unable to create result dir " + resultPath.absolutePath

//
// --- Actually running IDEA
//

//  ~/projects/dashboard.git/. ~/projects/dashboard.git/.idea/inspectionProfiles/bens_idea15_2015_11.xml /tmp/ -d server
def ideaArgs = [ideaPath.path, "inspect", rootfilePath.absolutePath, profilePath.absolutePath, resultPath.absolutePath]
ideaArgs << ((cliOpts.v) ? "-v2" : "-v1")
if (cliOpts.d) ideaArgs << "-d" << cliOpts.d

// Did user define a Analysis "Scope"? We need a dirty workaround
File origPropFile = applyScopeViaPropFile(cliOpts)

println "\n#"
println "# Running IDEA IntelliJ Inspection"
println "#"
println "Executing: " + ideaArgs.join(" ")
def exitValue = 0
if (!cliOpts.n) {
  def processBuilder = new ProcessBuilder(ideaArgs)
  processBuilder.redirectErrorStream(true)
  processBuilder.directory(rootDir)

  println "~" * 80
  def ideaProcess = processBuilder.start()
  ideaProcess.consumeProcessOutput((OutputStream) System.out, System.err)
  ideaProcess.waitForOrKill(1000 * 60 * ideaWrapperTimeout)
  exitValue = ideaProcess.exitValue()
  println "~" * 80
} else {
  println("Dry-run: Not starting IDEA process")
}

// Scope workaround: Clean up time
cleanupIdeaProps(cliOpts, origPropFile)

if (exitValue != 0) fail("IDEA process returned with an unexpected return code of $exitValue")

//
// --- Now lets look on the results
//
def returnCode = analyzeResult(resultPath, acceptedLevels, skipResults, skipIssueFilesRegex)
System.exit(returnCode)


// ===============================================================================================
// ==== End of script body
// ===============================================================================================


//
//  --- Helper functions
//
private List<String> parseConfigFile() {
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
    if (verbose) println "Parsing " + configFile.absolutePath

    //noinspection GroovyMissingReturnStatement
    configFile.eachLine { line ->
      def values = line.split(':')
      if (!line.startsWith('#') && values.length == 2) {
        configArgs.push('--' + values[0].trim())
        configArgs.push(values[1].trim())
      }
    }
  }

  if (verbose) println "Config file content: " << configArgs

  return configArgs
}

private OptionAccessor parseCli(List<String> configArgs) {
  def cliBuilder = new CliBuilder(usage: 'groovy ideainspect.groovy [options]',
                                  stopAtNonOption: false)
  cliBuilder.with {
    h argName: 'help', longOpt: 'help', 'Show usage information and quit'
    l argName: 'level', longOpt: 'levels', args: Option.UNLIMITED_VALUES, valueSeparator: ',',
      'Levels to look for. Default: WARNING,ERROR'
    s argName: 'file', longOpt: 'skip', args: Option.UNLIMITED_VALUES, valueSeparator: ',',
      'Analysis result files to skip. For example `TodoComment` or `TodoComment.xml`.'
    sf argName: 'regex', longOpt: 'skipfile', args: Option.UNLIMITED_VALUES, valueSeparator: ',',
       'Ignore issues affecting source files matching given regex. Example: `.*/generated/.*`.'
    sc argName: 'string', longOpt: 'scope', args: 1,
       'The name of the "Custom scope" to apply. Custom scopes can be defined in the IDE. '+
       'Share the resulting file in .idea/scopes/scopename.xml and provide the name of the scope (not file) here.'
    t argName: 'dir', longOpt: 'resultdir', args: 1,
      'Target directory to place the IDEA inspection XML result files. \nDefault: `target/inspection-results`'
    i argName: 'dir', longOpt: 'ideahome', args: 1,
      'IDEA or Android Studio installation home directory. Default: IDEA_HOME env var or `idea`'
    d argName: 'dir', longOpt: 'dir', args: 1, 'Limit IDEA inspection to this directory. Overrides the scope argument.'
    ip argName: 'file', longOpt: 'iprops', args: 1, 'Full path to your `idea.properties`. Only required if 1) you use --scope and 2) ' +
            'file is not located under in the default. \nDefault: `<ideahome>/idea/bin/idea.properties`'
    p argName: 'file', longOpt: 'profile', args: 1,
      'Use this inspection profile file. If given an absolute path, the target file is used, otherwise the arg ' +
      'denotes a file located under `.idea/inspectionProfiles`. \nDefault: `Project_Default.xml`'
    r argName: 'dir', longOpt: 'rootdir', args: 1,
      'IDEA project root directory containing the `.idea` directory. Default: Working directory'
    rf argName: 'file', longOpt: 'rootfile', args: 1,
            'full path to the pom.xml or build.gradle file for the project. Useful if the project is maven or gradle ' +
                    'based and its rootdir does not contain all the *.iml and .idea/modules.xml files'
    v argName: 'verbose', longOpt: 'verbose', args: 0,
      'Enable verbose logging'
    n argName: 'dry-run', longOpt: 'dry-run', args: 0,
      'Dry-run: Do not start IDEA, but run parsing'
    //to argName: 'minutes', longOpt: 'timeout', args: 1,
    //  'Timeout in Minutes to wait for IDEA to complete the inspection. Default:'
  }

  def opt = cliBuilder.parse(configArgs)

  if (!opt) {
    System.exit(1)
  }; // will print usage automatically
  if (opt.help) {
    println "\nThis tools runs IntelliJ IDEA inspection via command line and"
    println "tries to parse the output. \n"
    println "Example usage:"
    println "  ./ideainspect.groovy -i ~/devel/idea -r . -p myinspections.xml \\"
    println "     -d src/main/java -s unused,Annotator,TodoComment.xml -l ERROR\n"
    println "For more convenience you can pass all options in a `.ideainspect` file"
    println "instead of passing it via command line\n"
    cliBuilder.usage();
    System.exit(1);
  }
  if (verbose) {
    List<String> optDebug = []
    for (Option o : cliBuilder.options.options)
       optDebug.add(o.longOpt << ": " << opt.getProperty(o.longOpt))
    println "Effective configuration: " << optDebug.join(", ")
  }

  opt
}

private File findIdeaExecutable(OptionAccessor cliOpts) {
  def platform = System.properties['os.name'], scriptPath
  def ideaHome = getIdeaHome(cliOpts)
  def executable = "idea"
  if (ideaHome.toLowerCase().contains("android")) executable = "studio"

  switch (platform) {
    case ~/^Windows.*/:
      scriptPath = "bin" + File.separator + executable + ".bat"
      break;
    case "Mac OS X":
      scriptPath = "Contents/MacOS/" + executable
      break;
    default:
      scriptPath = "bin/" + executable + ".sh"
      break;
  }

  def ideaExecutable = new File(ideaHome + File.separator + scriptPath)
  assertPathIsFile(ideaExecutable, "IDEA Installation directory",
             "Use a IDEA_HOME environment variable or the `ideahome` property in `.ideainspect` \n" +
                     "or the `-i` command line option to point me to a valid IntelliJ installation")
  ideaExecutable
}

private static String getIdeaHome(OptionAccessor cliOpts) {
  cliOpts.i ?: (System.getenv("IDEA_HOME") ?: "idea")
}

/**
 * This method workarounds the lack of a CLI argument for the analysis scope by temporarily adding a
 * scope parameter in the {@code idea.properties} file.
 * @param propertiesPath Path to `idea.properties`
 * @param scopeName The scope to apply
 * @return The backup path of the original `idea.properties`
 */
private File applyScopeViaPropFile(OptionAccessor cliOpts) {

  if (!cliOpts.sc) return null
  String scopeName = cliOpts.sc

  if (cliOpts.n) {
    println "\nDry-run: You defined a analysis scope. We now would temporarily modify `idea.properties`."
    return null
  }
  println "\nYou defined a analysis scope. We need to temporarily modify `idea.properties` to get this working."

  def File ideaPropsFile = findIdeaProperties(cliOpts)
  def newPropsContent = new ArrayList<String>()
  def File propertiesBackupFile = null

  if (ideaPropsFile.exists()) {
    // If the file already exists we copy it
    propertiesBackupFile = new File(ideaPropsFile.absolutePath + ".idea-cli-inspect." + System.currentTimeMillis())
    Files.copy(ideaPropsFile.toPath(), propertiesBackupFile.toPath())
    List<String> lines = ideaPropsFile.readLines()
    for (line in lines) {
      if (!line.contains("idea.analyze.scope")) newPropsContent.add(line)
    }
  }

  // If the file does not exist, it is instantiated when written to
  newPropsContent.add("idea.analyze.scope=" + scopeName)
  ideaPropsFile.write(newPropsContent.join('\n'))

  def backupPath = propertiesBackupFile?.path ?: "<no file existed>"
  println "Added scope `" + scopeName + "` to `" + ideaPropsFile.absolutePath + "` Backup: `" + backupPath + "`"

  propertiesBackupFile
}

/**
 * Revert to original IDEA configuration from backup.
 */
private cleanupIdeaProps(OptionAccessor cliOpts, File backupFile) {
  if (!cliOpts.sc || backupFile == null) return;
  File ideaPropsFile = findIdeaProperties(cliOpts)
  ideaPropsFile.delete()
  if (backupFile?.exists()) {
    println "Recovering `" + backupFile.absolutePath + "` back to `" + ideaPropsFile.absolutePath + "`"
    backupFile.renameTo(ideaPropsFile)
  } else {
    println "Deleted temporarily created `" + ideaPropsFile.absolutePath + "`"
  }
}

/** Tries to locate the `idea.properties` file either via convention or via parameters. */
private File findIdeaProperties(OptionAccessor cliOpts) {
  String propertiesPath
  if (cliOpts.ip) {
    propertiesPath = cliOpts.ip
  } else {
    propertiesPath = getIdeaHome(cliOpts) + "/bin/idea.properties"
  }
  def propsFile = new File(propertiesPath)
  assertPathIsFile(propsFile, "idea.properties",
             "IDEA currently does currently not allow to pass the desired inspection scope as program parameter.\n" +
                     "Currently the only way is to set a temporary property in the `idea.properties` configuration file\n" +
                     "of your IntelliJ installation. We did not find that file. Therefore you need to pass the full path\n" +
                     "to this file if you want to restrict the analysis to a specific scope."
  )
  propsFile
}

@SuppressWarnings("GroovyUntypedAccess")
private analyzeResult(File resultPath, List<String> acceptedLeves,
                      List skipResults, List skipIssueFilesRegex) {

  printAnalysisHeader(resultPath, acceptedLeves, skipResults, skipIssueFilesRegex)

  def allGood = true;

  resultPath.eachFile(FileType.FILES) { file ->

    String fileContents = workaroundUnclosedProblemXmlTags(file.getText('UTF-8'))

    def xmlDocument = new XmlParser().parseText(fileContents)
    def fileIssues = []
    def xmlFileName = file.name

    if (skipResults.contains(xmlFileName.replace(".xml", ""))) {
      println "--- Skipping $xmlFileName"
      return
    }

    xmlDocument.problem.each { problem ->
      String severity = problem.problem_class.@severity
      String affectedFile = problem.file.text()
      boolean fileShouldBeIgnored = false
      skipIssueFilesRegex.each { String regex -> fileShouldBeIgnored = (fileShouldBeIgnored || affectedFile.matches(regex)) }
      if (acceptedLeves.contains(severity) && !fileShouldBeIgnored) {
        String problemDesc = problem.description.text()
        String line = problem.line.text()
        fileIssues << "$severity $affectedFile:$line -- $problemDesc";
      }
    }

    if (!fileIssues.empty) {
      allGood = false;
      System.err.println("--- $xmlFileName")
      System.err.println(fileIssues.join("\n"))
      System.err.println("")
    }
  }

  printAnalysisFooter(allGood)
  return allGood ? 0 : 1
}

private void printAnalysisHeader(File resultPath, List<String> acceptedLeves, List skipResults, List skipIssueFilesRegex) {
  println "\n#"
  println "# Inspecting produced result files in $resultPath"
  println "#"
  println "# Looking for levels    : $acceptedLeves"
  println "# Ignoring result files : $skipResults"
  println "# Ignoring source files : $skipIssueFilesRegex"
}

private void printAnalysisFooter(boolean allGood) {
  println "\n#"
  println "# Analysis Result"
  println "#"

  println allGood ? "Looks great - everything seems to be ok!"
                  : "Entries found. Returncode: 1"
}

/**
 * Workaround for wrong XML formatting. IDEA clutters "</problems>" all-over.
 * See Bug report : https://youtrack.jetbrains.com/issue/IDEA-148855
 * @param fileContents XML file content string
 * @return XML file content with duplicate {@code </problems>} entries removed.
 */
private static String workaroundUnclosedProblemXmlTags(String fileContents) {
  fileContents = fileContents.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "")
  fileContents = fileContents.replace("file://\$PROJECT_DIR\$/", "")
  fileContents = "<problems>" + fileContents.replaceAll("<.?problems.*>", "") + "</problems>"
  fileContents
}

/**
 * Fail if the passed File does not exist or is not a directory
 * @param path The path to test for being a directory
 * @param description A human-readable description what dir we are looking for
 * @param hint an optional hint what to do now...
 */
private void assertPathIsDir(File path, String description, String hint = null) {
  if (!path.exists() || !path.isDirectory()) {
    println "PROBLEM: " + description + " `" + path.path + "` not found or not a directory!"
    if (hint) println "\n" + hint
    println "\nAborting."
    System.exit(1)
  }
}
/**
 * Fail if the passed File does not exist or is not a file
 * @param path The path to test for being a file
 * @param description A human-readable description what file we are looking for
 * @param hint an optional hint what to do now...
 */
private void assertPathIsFile(File path, String description, String hint = null) {
  if (!path.exists() || !path.isFile()) {
    println "PROBLEM: " + description + " `" + path.path + "` not found or not a file!"
    if (hint) println "\n" + hint
    println "\nAborting."
    System.exit(1)
  }
}

private void fail(String message) {
  println "FATAL ERROR: " + message
  println "\nAborting."
  System.exit(1)
}
