#validateme#
Automatically exported from code.google.com/p/validateme

##Introduction##
This tool validates xml documents and allows user to choose how many validation problems to display, and collects some basic statistics on validation problems. It is available as a Groovy script, and a standalone jar. I wrote this because I found that many validators will stop on the first error. I wanted to be able to see more, and get a feel for the kinds of errors that existed in the document. It won't tell you how to fix your xml document, but hopefully it will help you identify some patterns to do so. Note that you currently have to have a DTD specified in the xml, for the validation to take place. Also, this script can take a fair amount of memory, I use JAVA_OPTS="-Xmx1024M".

##Usage##
<pre>Usage: validateMe (summary|all|unique (asc|desc|none))|<number> <filename>"
        SUMMARY  -  shows no problems
        ALL      -  shows all problems
        UNIQUE   -  shows each problem only once
                    [ASC|DESC|PARSE] (optional, defaults to parse order)
        <number> -  Entering your own number shows the first <number> problems.
If you do not specify a verbosity, defaults to showing the first 10 problems.</pre>

##Use Cases##
The way I used this script was running the script once with unique argument, and once with verbose argument, using redirects to write the output of each to a separate file. Then I would look at the unique file to decide which to go after first (one that occurred the most or the least), then look at the verbose file to see where it occurred and look for patterns. Particularly if the unique file showed the error count as being the same number as the number of elements (hence it happens every time). So the commands you run would look like this:<pre>
validateMe.groovy unique filename.xml > filename-unique.txt
validateMe.groovy all filename.xml > filename-all.txt</pre>
