/*
 * This is a simple Groovy script to validate xml documents and show validation problems
 * Note: This script requires groovy installed to run
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
                String filename = args[0]
                String verbosity                
                Sort sortUniquesBy = Sort.PARSE
                
                // set up the parser
                File file = new File(filename)
                boolean validating = true
                boolean namespaceAware = true
                def xmlReader = new XmlSlurper(validating, namespaceAware)
                MyErrorHandler errorH = null
                
                // set up the error handler
                if (args.length == 2) {
                    verbosity = args[1]
                    if (Verbosity.SUMMARY.toString().equalsIgnoreCase(verbosity)) {
                        errorH = new MyErrorHandler(0)
                    } else if (Verbosity.VERBOSE.toString().equalsIgnoreCase(verbosity)) {
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
                    // can only be 3 in the case of unique
                    if (!Verbosity.UNIQUE.toString().equalsIgnoreCase(verbosity)) {
                        showHelp()
                    } else {
                        sortUniquesBy = Sort.valueOf(args[2].toUpperCase())
                        errorH = new MyErrorHandler(0)
                        println "Unique Problems:\n"
                    }
                } else {    // no verbosity specified
                    errorH = new MyErrorHandler(10)
                    println "The First 10 Problems:\n"
                }
                
                // perform the parsing
                xmlReader.setErrorHandler(errorH)
                xmlReader.parse(filename)
                
                // show unique problems, if requested
                if (Verbosity.UNIQUE.toString().equalsIgnoreCase(verbosity)) {                    
                    def uniques
                    if (sortUniquesBy == Sort.ASC) {
                        uniques = errorH.uniqueExceptions.sort {it.value}
                    } else if (sortUniquesBy == Sort.DESC) {
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
                println "\nSUMMARY"
                println "  $warnings  warnings"
                println "+ $errors  errors"
                println "+ $fatal  fatal errors"
                println "----------------------"
                println "= $total total problems"
                println "Of which, there are $unique unique problems"
                
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
        println "Usage: validateMe <filename> (summary|verbose|unique (asc|desc|none))|<number>"
        println "\tSUMMARY  -  shows no problems"
        println "\tVERBOSE  -  shows all problems"
        println "\tUNIQUE   -  shows each problem only once"
        println "\t            [ASC|DESC|PARSE] (optional, defaults to parse order)"
        println "\t<number> -  Entering your own number shows the first <number> problems."
        println "If you do not specify a verbosity, defaults to showing the first 10 problems."
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
public enum Verbosity { SUMMARY, VERBOSE, UNIQUE }