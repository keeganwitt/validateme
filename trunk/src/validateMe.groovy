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
        if (args.length > 0 && args.length < 3 && !"help".equalsIgnoreCase(args[0])) {
            try {
                String filename = args[0]
                String verbosity
                
                // set up the parser
                File file = new File(filename)
                boolean validating = true
                boolean namespaceAware = true
                def xmlReader = new XmlParser(validating, namespaceAware)
                MyErrorHandler errorH = null
                
                // set up the error handler
                if (args.length == 2) {
                    verbosity = args[1]
                    if ("summary".equalsIgnoreCase(verbosity)) {
                        errorH = new MyErrorHandler(0)
                    } else if ("verbose".equalsIgnoreCase(verbosity)) {
                        errorH = new MyErrorHandler(Long.MAX_VALUE)
                        println "All Problems:\n"
                    } else if ("unique".equalsIgnoreCase(verbosity)) {
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
                } else {    // no verbosity specified
                    errorH = new MyErrorHandler(10)
                    println "The First 10 Problems:\n"
                }
                
                // perform the parsing
                xmlReader.setErrorHandler(errorH)
                xmlReader.parse(filename)
                
                // show unique problems, if requested
                if ("unique".equalsIgnoreCase(verbosity)) {
                    errorH.uniqueExceptions.eachWithIndex () { problem, index ->
                        println "${index + 1}. $problem\n"
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
            println "Usage: validateMe <filename> {summary|verbose|unique|<number of problems to display>}"
            println "SUMMARY shows no problems, VERBOSE shows all problems, UNIQUE shows each problem only once (though not in any order).  Entering your own number shows the first <number of problems to display> problems."
            println "If you do not specify a verbosity, it will default to outputting only the first 10 problems."
        }
        
        return
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
    def uniqueExceptions = [] as Set
    
    public MyErrorHandler(long numToDisplay) {
        this.warnings = 0
        this.errors = 0
        this.fatal = 0
        this.total = 0
        this.numToDisplay = numToDisplay
        this.uniqueExceptions = [] as Set
    }
    
    public MyErrorHandler() {
        this.warnings = 0
        this.errors = 0
        this.fatal = 0
        this.total = 0
        this.numToDisplay = Long.MAX_VALUE
        this.uniqueExceptions = [] as Set
    }

    public void warning(SAXParseException exception) {
        if (total < numToDisplay) {
            println "${total + 1}. exception\n"
        }
        uniqueExceptions.add(exception.getMessage())
        warnings++
        total++
    }

    public void error(SAXParseException exception) {
        if (total < numToDisplay) {
            println "${total + 1}. $exception\n"
        }
        uniqueExceptions.add(exception.getMessage())
        errors++
        total++
    }

    public void fatalError(SAXParseException exception) {
        if (total < numToDisplay) {
            println "${total + 1}.$exception\n"
        }
        uniqueExceptions.add(exception.getMessage())
        fatal++
        total++
    }
}