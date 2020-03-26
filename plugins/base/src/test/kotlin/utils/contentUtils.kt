package utils

import matchers.content.ContentMatcherBuilder
import matchers.content.group
import matchers.content.link

fun ContentMatcherBuilder<*>.signature(name: String, vararg params: Pair<String, String>) =
    signature(name, null, *params)

fun ContentMatcherBuilder<*>.signature(name: String, returnType: String?, vararg params: Pair<String, String>) = group {
    +"final fun"
    link { +name }
    +"("
    params.forEach { (n, t) ->
        +"$n:"
        group { link { +t } }
    }
    +")"
    returnType?.let { +": $it" }
}