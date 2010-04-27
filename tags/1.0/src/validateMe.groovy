/*
 * This is a simple Groovy script to validate xml documents and show validation problems
 *
 *
 * Copyright (c)2009 Keegan Witt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException

/**
 * This class is the main class that runs the validation
 */
class ValidateMe {
    public static main(def args) {
        if (args.length > 0 && args.length < 4 && !"help".equalsIgnoreCase(args[0])) {
            try {
                def filename = ""
                def verbosity = ""
                def sortUniquesBy = Sort.PARSE
                MyErrorHandler errorH = null

                // set up the error handler, filename, and verbosity
                if (args.length == 2) {
                    verbosity = args[0]
                    filename = args[1]
                    if (Verbosity.SUMMARY.toString().equalsIgnoreCase(verbosity)) {
                        errorH = new MyErrorHandler(0)
                    } else if (Verbosity.ALL.toString().equalsIgnoreCase(verbosity)) {
                        errorH = new MyErrorHandler(Long.MAX_VALUE)
                        println "All Problems:\n"
                    } else if (Verbosity.UNIQUE.toString().equalsIgnoreCase(verbosity)) {
                        errorH = new MyErrorHandler(0)
                        println "Unique Problems:\n"
                    } else {
                        try {
                            long numToDisplay = Long.parseLong(verbosity)
                            errorH = new MyErrorHandler(numToDisplay)
                            println "The First $numToDisplay Problems:\n"
                        } catch (NumberFormatException nfe) {
                            errorH = new MyErrorHandler(10)
                            println "The First 10 Problems:\n"
                        }
                    }
                } else if (args.length == 3) {
                    verbosity = args[0]
                    def order = args[1]
                    filename = args[2]
                    // can only be 3 in the case of unique
                    if (!Verbosity.UNIQUE.toString().equalsIgnoreCase(verbosity)) {
                        showHelp()
                    } else {
                        sortUniquesBy = Sort.valueOf(order.toUpperCase())
                        errorH = new MyErrorHandler(0)
                        println "Unique Problems:\n"
                    }
                } else {    // no verbosity specified (args.length == 1)
                    filename = args[0]
                    errorH = new MyErrorHandler(10)
                    println "The First 10 Problems:\n"
                }

                // set up the parser
                def file = new File(filename)
                def validating = true
                def namespaceAware = true
                def xmlReader = new XmlSlurper(validating, namespaceAware)

                // perform the parsing
                xmlReader.setErrorHandler(errorH)
                xmlReader.parse(filename)

                // show unique problems, if requested
                if (Verbosity.UNIQUE.toString().equalsIgnoreCase(verbosity)) {
                    def uniques
                    if (sortUniquesBy == Sort.ASC) {
                        // sorts smallest first
                        uniques = errorH.uniqueExceptions.sort {it.value}
                    } else if (sortUniquesBy == Sort.DESC) {
                        // sorts largest first
                        uniques = errorH.uniqueExceptions.sort {-it.value}
                    } else {
                        uniques = errorH.uniqueExceptions
                    }

                    uniques.eachWithIndex () { problem, index ->
                        println "${index + 1}. (${problem.value} occurrences): ${problem.key}\n"
                    }
                }

                // display summary statistics
                int total = errorH.total
                int warnings = errorH.warnings
                int errors = errorH.errors
                int fatal = errorH.fatal
                int unique = errorH.uniqueExceptions.size()
                println ""
                println "SUMMARY".center(26)
                println "--------------------------"
                printf("|%-15s|%8d|\n", "warnings", warnings)
                printf("|%-15s|%8d|\n", "errors", errors)
                printf("|%-15s|%8d|\n", "fatal errors", fatal)
                println "--------------------------"
                printf("|%-15s|%8d|\n", "total problems", total)
                printf("|%-15s|%8d|\n", "unique problems", unique)
                println "--------------------------"

                // display validation judgement
                if (errors == 0 && fatal == 0) {
                    println "\n${file.name} is a *VALID* XML document."
                } else {
                    println "\n${file.name} is an *INVALID* XML document."
                }
            } catch(Exception exception) {
                println exception
            }
        } else {    // arguments didn't match or asked for help, show usage
            showHelp()
        }

        return
    }

    public static void showHelp() {
        System.err.println "Usage: validateMe (summary|all|unique (asc|desc|none))|<number> <filename>"
        System.err.println "\tSUMMARY  -  shows no problems."
        System.err.println "\tALL      -  shows all problems."
        System.err.println "\tUNIQUE   -  shows each problem only once"
        System.err.println "\t            [ASC|DESC|PARSE] (optional, defaults to parse order)"
        System.err.println "\t              to sort by the number of occurences."
        System.err.println "\t<number> -  Entering your own number shows the first <number> problems."
        System.err.println "If you do not specify a verbosity, defaults to showing the first 10 problems."
    }
}

/**
 * This class is an error handler for SAX exceptions, it prints more
 * than the default handler, and also collects summary statistics.
 */
class MyErrorHandler implements ErrorHandler {
    long warnings = 0
    long errors = 0
    long fatal = 0
    long total = 0
    long numToDisplay = Long.MAX_VALUE
    def uniqueExceptions = [:]    // map of exception messages : number of occurances

    public MyErrorHandler(long numToDisplay) {
        this.warnings = 0
        this.errors = 0
        this.fatal = 0
        this.total = 0
        this.numToDisplay = numToDisplay
        this.uniqueExceptions = [:]
    }

    public MyErrorHandler() {
        this.warnings = 0
        this.errors = 0
        this.fatal = 0
        this.total = 0
        this.numToDisplay = Long.MAX_VALUE
        this.uniqueExceptions = [:]
    }

    public void warning(SAXParseException exception) {
        if (total < numToDisplay) {
            println "${total + 1}. (L${exception.getLineNumber()}, C${exception.getColumnNumber()}): $exception\n"
        }
        if (uniqueExceptions.getAt(exception.getMessage()) == null) {
            uniqueExceptions.put(exception.getMessage(), 1)
        } else {
            uniqueExceptions.(exception.getMessage())++
        }
        warnings++
        total++
    }

    public void error(SAXParseException exception) {
        if (total < numToDisplay) {
            println "${total + 1}. (L${exception.getLineNumber()}, C${exception.getColumnNumber()}): $exception\n"
        }
        if (uniqueExceptions.getAt(exception.getMessage()) == null) {
            uniqueExceptions.put(exception.getMessage(), 1)
        } else {
            uniqueExceptions.(exception.getMessage())++
        }
        errors++
        total++
    }

    public void fatalError(SAXParseException exception) {
        if (total < numToDisplay) {
            println "${total + 1}. (L${exception.getLineNumber()}, C${exception.getColumnNumber()}): $exception\n"
        }
        if (uniqueExceptions.getAt(exception.getMessage()) == null) {
            uniqueExceptions.put(exception.getMessage(), 1)
        } else {
            uniqueExceptions.(exception.getMessage())++
        }
        fatal++
        total++
    }
}

// enums down here instead of inside ValidateMe because of http://jira.codehaus.org/browse/GROOVY-3979
public enum Sort { PARSE, ASC, DESC }
public enum Verbosity { SUMMARY, ALL, UNIQUE }