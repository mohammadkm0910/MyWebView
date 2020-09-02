package com.mohammadkk.mywebview

object UrlHelper {
    const val googleUrl = "https://www.google.com/"
    const val googleSearchUrl = "https://www.google.com/search?q="
    const val bingSearchUrl = "https://www.bing.com/search?q="
    const val yahooSearchUrl = "https://search.yahoo.com/search;_ylt=A0oG7l7PeB5P3G0AKASl87UF?p="
    const val jsInversesColor =
            ("javascript: ("
                    + "function () { "
                    + "var css = 'html {-webkit-filter: invert(100%);' +"
                    + "    '-moz-filter: invert(100%);' + "
                    + "    '-o-filter: invert(100%);' + "
                    + "    '-ms-filter: invert(100%); }',"
                    + "head = document.getElementsByTagName('head')[0],"
                    + "style = document.createElement('style');"
                    + "if (!window.counter) { window.counter = 1;} else  { window.counter ++;"
                    + "if (window.counter % 2 == 0) { var css ='html {-webkit-filter: invert(0%); -moz-filter:    invert(0%); -o-filter: invert(0%); -ms-filter: invert(0%); }'}"
                    + "};"
                    + "style.type = 'text/css';"
                    + "if (style.styleSheet){"
                    + "style.styleSheet.cssText = css;"
                    + "} else {"
                    + "style.appendChild(document.createTextNode(css));"
                    + "}" //injecting the css to the head
                    + "head.appendChild(style);"
                    + "}());")
}